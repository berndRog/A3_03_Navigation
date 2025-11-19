package de.rogallab.mobile.ui.base

import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import de.rogallab.mobile.domain.utilities.logDebug
import de.rogallab.mobile.domain.utilities.logError
import de.rogallab.mobile.ui.errors.ErrorState
import de.rogallab.mobile.ui.navigation.INavHandler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.also
import kotlin.collections.any
import kotlin.collections.indexOfFirst
import kotlin.collections.toMutableList
import kotlin.onFailure
import kotlin.ranges.coerceAtMost

abstract class BaseViewModel(
   private val _navHandler: INavHandler,
   private val _tag: String = "<-BaseViewModel"
): ViewModel() {

   // region Generic Single-slot UNDO buffer -------------------------------------------------------
   /**
    * REMOVE (Optimistic-then-Persist)
    *
    * Implements an optimistic deletion pattern:
    *  - The UI updates immediately for responsiveness.
    *  - The actual repository deletion happens asynchronously.
    *
    * Workflow:
    *  1) Determine the index of the item using its ID (more robust than instance equality).
    *  2) Expose the removed item and its index to the caller (e.g., for undo or logging).
    *  3) Update the UI state immediately by removing the item from the list.
    *  4) Persist the deletion in the background via the provided suspend function.
    *
    * @param item The item to remove.
    * @param currentList The current list containing the item.
    * @param getId Function that extracts a unique ID from an item.
    * @param onRemovedItem Callback invoked with the removed item (or null if not found).
    * @param onRemovedItemIndex Callback invoked with the index of the removed item.
    * @param updateUi Updates the UI state with the modified list.
    * @param persistRemove Suspend function that persists the removal and returns a Result.
    * @param tag Logging tag for debug output.
    */
   protected fun <T> removeItem(
      item: T,
      currentList: List<T>,
      getId: (T) -> String,
      onRemovedItem: (T?) -> Unit,
      onRemovedItemIndex: (Int) -> Unit,
      updateUi: (List<T>) -> Unit,
      persistRemove: suspend (T) -> Result<Unit>,
      tag: String
   ) {
      logDebug(tag, "removeItem()")

      // First step: Optimistic UI update
      // Find index of person to remove
      val index = currentList.indexOfFirst { getId(it) == getId(item) }
      if (index == -1) return
      onRemovedItem( item )
      onRemovedItemIndex(index)

      // Remove item from list and update UI immediately
      // immediately update UI - without data handling
      val updatedList = currentList.toMutableList()
         .also { it.removeAt(index) }
      updateUi(updatedList)

      // Second step: Persistence in background
      // Remove person from repository
      viewModelScope.launch {
         logDebug(tag, "persistRemove()")
         persistRemove(item)
            .onFailure { t -> handleErrorEvent(t) }
      }
   }

   /**
    * UNDO (Optimistic-then-Persist)
    *
    * Restores a previously removed item by applying the inverse of an
    * optimistic deletion: the UI is updated immediately, and persistence
    * happens asynchronously in the background.
    *
    * Workflow:
    *  1) Validate the undo buffer (item and index must be present).
    *  2) Reinsert the item into the current UI list at its original index.
    *     (Index is clamped via `coerceAtMost` to avoid out-of-bounds errors
    *      if the list has changed in the meantime.)
    *  3) Notify the UI of the restored item (optionally used for scrolling/highlighting).
    *  4) Persist the recreation of the item in the repository.
    *  5) Clear the undo buffer.
    *
    * @param removedItem The item previously removed (null → nothing to undo).
    * @param getId Extracts the unique ID of an item.
    * @param removedIndex The original index of the removed item.
    * @param currentList The list before the undo operation.
    * @param updateUi Updates the UI list and provides the restored item ID.
    * @param persistCreate Suspend function to persist the recreation.
    * @param onReset Clears the undo buffer after undo is completed.
    * @param tag Logging tag for debug output.
    */
   protected fun <T> undoItem(
      removedItem: T?,
      getId: (T) -> String,
      removedIndex: Int,
      currentList: List<T>,
      updateUi: (List<T>, String?) -> Unit,
      persistCreate: suspend (T) -> Result<Unit>,
      onReset: () -> Unit, // reset undo buffer
      tag: String
   ) {
      // Restore the last removed person if any
      val itemToRestore = removedItem ?: return
      if (removedIndex == -1) return
      val itemId = getId(itemToRestore)
      logDebug(tag, "undoRemove: $itemId")

      // Restore person in StateFlow and
      // immediately update UI - without data handling
      val updated = currentList.toMutableList()
      if (updated.any { getId(it) == itemId }) return // already in list
      // Reinsert at old index (or at end if list got shorter)
      updated.add(removedIndex.coerceAtMost(updated.size), itemToRestore)
      updateUi(updated, itemId)

      // Add person back to repository in background
      viewModelScope.launch {
         logDebug(tag, "persistCreate()()")
         persistCreate(itemToRestore)
            .onFailure { t -> handleErrorEvent(t) }
      }
      // reset undo buffer
      onReset()
   }

   // handle undo event
   fun handleUndoEvent(errorState: ErrorState) {
      logError(_tag, "handleUndoEvent ${errorState.message}")
      viewModelScope.launch {
         _errorFlow.emit(errorState)
      }
   }
   // endregion

   // region ErrorHandling -------------------------------------------------------------------------
   // MutableSharedFlow with replay = 1 ensures that the last emitted error is replayed
   // to new collectors, allowing the error to be shown immediately when a new observer
   // collects the flow (navigation case).
   private val _errorFlow: MutableSharedFlow<ErrorState?> = MutableSharedFlow<ErrorState?>(replay = 1)
   val errorFlow: Flow<ErrorState?> = _errorFlow.asSharedFlow()

   /**
    * Emits an error event that is typically handled by a UI error host
    * (e.g., a SnackbarHost in Compose). Supports optional user actions
    * and delayed navigation.
    *
    * Behavior:
    *  - If a throwable is provided, its message is preferred unless an
    *    explicit `message` parameter overrides it.
    *  - The UI receives a complete error payload including optional:
    *      • action label and callback (e.g., Retry)
    *      • dismiss action callback
    *      • custom snackbar duration
    *  - Optional navigation (`navKey`) can be triggered after the snackbar
    *    is dismissed or its action is performed, depending on the UI host.
    *
    * @param throwable Optional exception that triggered the error.
    * @param message Optional human-readable error message. Overrides throwable message if set.
    * @param actionLabel Optional label for an actionable button (e.g., "Retry").
    * @param onActionPerform Callback executed when the user presses the action button.
    * @param withDismissAction Whether the snackbar shows a dismiss button.
    * @param onDismissed Callback executed when the snackbar is dismissed.
    * @param duration Snackbar visibility duration.
    * @param navKey Optional navigation target to trigger after handling the error.
    */
   protected fun handleErrorEvent(
      throwable: Throwable? = null,
      message: String? = null,
      actionLabel: String? = null,       // no actionLabel by default
      onActionPerform: () -> Unit = {},  // do nothing by default
      withDismissAction: Boolean = true, // show dismiss action
      onDismissed: () -> Unit = {},      // do nothing by default
      duration: SnackbarDuration = SnackbarDuration.Long,
      // delayed navigation
      navKey: NavKey? = null           // no navigation by default
   ) {
      val errorMessage =  throwable?.message ?: message ?: "Unknown error"
      logError(_tag, "handleErrorEvent $errorMessage")

      val errorState = ErrorState(
         message = errorMessage,
         actionLabel = actionLabel,
         onActionPerform = onActionPerform,
         withDismissAction = withDismissAction,
         onDismissed = onDismissed,
         duration = duration,
         navKey = navKey,
         onDelayedNavigation = { key ->
            // Only navigate after dismissal
            if (key != null) {
               logDebug(_tag, "Navigating to $key after error dismissal")
               _navHandler.popToRootAndNavigate(key)
            }
         }
      )
      viewModelScope.launch {
         logError(_tag, errorMessage)
         _errorFlow.emit(errorState)
      }
   }

   /**
    * Clears the current error state by emitting `null` into the error flow.
    * This signals the UI to remove any visible error indicators
    * (e.g., snackbar, dialog, inline message).
    *
    * The operation runs inside `viewModelScope` to ensure that
    * flow emission is lifecycle-aware and safe for concurrent collectors.
    */
   fun clearErrorState() {
      logError(_tag, "clearErrorState")
      viewModelScope.launch {
         _errorFlow.emit(null)  // Emit null to clear the error state
      }
   }
   // endregion
}