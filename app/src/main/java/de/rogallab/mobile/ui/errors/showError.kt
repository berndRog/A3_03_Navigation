package de.rogallab.mobile.ui.errors

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import de.rogallab.mobile.domain.utilities.logDebug
import de.rogallab.mobile.domain.utilities.logError

suspend fun showError(
   snackbarHostState: SnackbarHostState,  // State ↓
   errorState: ErrorState,                // State ↓
) {
   val message =  errorState.message
   logError("<-showError", "Displaying Snackbar: $message") // Changed log slightly for clarity

   // Show the Snackbar with the provided parameters
   //
   // message:           this is the primary text shown in the snackbar
   // actionLabel:       Text for an optional action button on the Snackbar. If clicked showSnackbar()
   //                    returns SnackbarResult.ActionPerformed and the onActionPerform() is called,
   //                    which can be "do nothing"
   // withDismissAction: if true, shows a dismiss icon X on the Snackbar. If clicked, or if the snackbar
   //                    times out or is swiped away, showSnackbar() returns SnackbarResult.Dismissed,
   //                    and onDismissed() is called which can be "do nothing".
   // duration:          Defines how long the Snackbar will be visible.
   snackbarHostState.showSnackbar(
      message = message,
      actionLabel = errorState.actionLabel,
      withDismissAction = errorState.withDismissAction,
      duration = errorState.duration
   ).also { result: SnackbarResult ->
      // Handle Snackbar action
      when (result) {
         SnackbarResult.ActionPerformed -> {
            logDebug("<-showError", "Snackbar action performed")
            errorState.onActionPerform()
         }
         SnackbarResult.Dismissed -> {
            logDebug("<-showError", "Snackbar dismissed")
            errorState.onDismissed()

            // Only navigate after dismissal
            if (errorState.navKey != null) {
               logDebug("<-showError", "Executing delayed navigation")
               errorState.onDelayedNavigation(errorState.navKey)
            }
         }
      }

   }
}