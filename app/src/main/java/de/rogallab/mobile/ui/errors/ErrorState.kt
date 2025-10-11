package de.rogallab.mobile.ui.errors

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey

@Immutable
data class ErrorState(
   // Snackbar parameters

   // message:           The main text shown in the snackbar.
   //                    Default is "Error unknown" if no message is provided.
   val message: String = "Error unknown",

   // actionLabel:       Text for an optional action button on the Snackbar. If clicked,
   //                    showSnackbar() returns SnackbarResult.ActionPerformed and
   //                    onActionPerform() is called (can be a no-op).
   val actionLabel: String? = null,
   val onActionPerform: () -> Unit = { }, // no-op (do nothing)

   // withDismissAction: If true, shows a dismiss icon (X) on the Snackbar.
   //                    If clicked, or if the Snackbar times out or is swiped away,
   //                    showSnackbar() returns SnackbarResult.Dismissed,
   //                    and onDismissed() is called (can be a no-op).
   val withDismissAction: Boolean = true,
   val onDismissed: () -> Unit = { }, // no-op (do nothing)

   // duration:          Defines how long the Snackbar remains visible.
   val duration: SnackbarDuration = SnackbarDuration.Long,
   // delayed navigation: Optional navigation key used for deferred navigation
   //                     onDelayedNavigation() will be called after the Snackbar is
   //                     dismissed.
   val navKey: NavKey? = null, // optional NavKey for navigation
   val onDelayedNavigation: (NavKey?) -> Unit = {  }
)


/*
data class ErrorState(
   // Snackbar parameters
   // message:           this is the primary text shown in the snackbar
   val message: String = "Error unknown", // default message if none is provided
   // actionLabel:       Text for an optional action button on the Snackbar. If clicked showSnackbar()
   //                    returns SnackbarResult.ActionPerformed and the onActionPerform() is called,
   //                    which can be "do nothing"
   val actionLabel: String? = null,
   val onActionPerform: () -> Unit = { }, // do nothing
   // withDismissAction: if true, shows a dismiss icon X on the Snackbar. If clicked, or if the snackbar
   //                    times out or is swiped away, showSnackbar() returns SnackbarResult.Dismissed,
   //                    and onDismissed() is called which can be "do nothing".
   val withDismissAction: Boolean = true,
   val onDismissed: () -> Unit = { }, // do nothing
   // duration:          Defines how long the Snackbar will be visible.
   // duration of the snackbars visibility
   val duration: SnackbarDuration = SnackbarDuration.Long,

   // delayed navigation, default do nothing
   val navKey: NavKey? = null, // optional NavKey for navigation
   val onDelayedNavigation: (NavKey?) -> Unit = {  }
)
 */