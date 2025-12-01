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

   // handle undo event
   fun handleUndoEvent(errorState: ErrorState) {
      logError(_tag, "handleUndoEvent ${errorState.message}")
      viewModelScope.launch {
         _errorFlow.emit(errorState)
      }
   }

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