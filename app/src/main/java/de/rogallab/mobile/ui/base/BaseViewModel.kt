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

open class BaseViewModel(
   private val _navHandler: INavHandler,
   private val _tag: String = "<-BaseViewModel"
): ViewModel() {

   // MutableSharedFlow with replay = 1 ensures that the last emitted error is replayed
   // to new collectors, allowing the error to be shown immediately when a new observer
   // collects the flow (navigation case).
   private val _errorFlow: MutableSharedFlow<ErrorState?> =
      MutableSharedFlow<ErrorState?>(replay = 1)
   val errorFlow: Flow<ErrorState?> =
      _errorFlow.asSharedFlow() 

   // handle throwable, i.e. an error event
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

   // handle undo event
   fun handleUndoEvent(errorState: ErrorState) {
      logError(_tag, "handleUndoEvent ${errorState.message}")
      viewModelScope.launch {
         _errorFlow.emit(errorState)
      }
   }

   fun clearErrorState() {
      logError(_tag, "clearErrorState")
      viewModelScope.launch {
         _errorFlow.emit(null)  // Emit null to clear the error state
      }
   }
}