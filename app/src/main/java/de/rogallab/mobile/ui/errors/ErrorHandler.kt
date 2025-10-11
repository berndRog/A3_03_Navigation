package de.rogallab.mobile.ui.errors

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import de.rogallab.mobile.ui.base.BaseViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ErrorHandler(
   viewModel: BaseViewModel,
   snackbarHostState: SnackbarHostState
) {
   val lifecycleOwner = LocalLifecycleOwner.current
   LaunchedEffect(viewModel, lifecycleOwner, snackbarHostState) {
      lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
         // collectLatest automatically cancels the currently running Snackbar
         // when a new event is emitted.
         viewModel.errorFlow.collectLatest { errorState ->
            if (errorState == null) return@collectLatest
            try {
               showError(snackbarHostState, errorState)
            } finally {
               viewModel.clearErrorState()
            }
         }
      }
   }
}