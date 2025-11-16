package de.rogallab.mobile.ui.errors

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import de.rogallab.mobile.domain.utilities.logVerbose
import de.rogallab.mobile.ui.base.BaseViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ErrorHandler(
   viewModel: BaseViewModel,
   snackbarHostState: SnackbarHostState,
   onCleanUp: () -> Unit = { },
) {
   val tag = "<-ErrorHandler"

   val lifecycleOwner = (LocalActivity.current as? ComponentActivity)
      ?: LocalLifecycleOwner.current
   val lifecycle = lifecycleOwner.lifecycle

   LaunchedEffect(viewModel, lifecycleOwner, lifecycle, snackbarHostState) {
      logVerbose(tag, "lifecycleOwner:$lifecycleOwner")
      logVerbose(tag, "lifecycle.State:${lifecycle.currentState}")

      lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
         // collectLatest automatically cancels the currently running Snackbar
         // when a new event is emitted.
         viewModel.errorFlow.collectLatest { errorState ->
            if (errorState == null) return@collectLatest
            logVerbose(tag, "${errorState.toString()}")
            try {
               showError(snackbarHostState, errorState)
            } finally {
               onCleanUp()                 // reset loading indicator, emptylist() ...
               viewModel.clearErrorState() // reset the error state in the ViewModel
            }
         }
      }
   }
}