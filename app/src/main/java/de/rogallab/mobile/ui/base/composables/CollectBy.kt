package de.rogallab.mobile.ui.base.composables

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.domain.utilities.logVerbose
import kotlinx.coroutines.flow.StateFlow
import kotlin.toString

@Composable
fun <T> CollectBy (uiStateFlow: StateFlow<T>, tag:String ): T {

   // Get the LifecycleOwner
   val lifecycleOwner = (LocalActivity.current as? ComponentActivity)
      ?: LocalLifecycleOwner.current

   // Get the Lifecycle
   val lifecycle = lifecycleOwner.lifecycle

   // Collect the StateFlow as State with lifecycle awareness
   val uiState: T by uiStateFlow.collectAsStateWithLifecycle(
      lifecycle = lifecycle,
      minActiveState = Lifecycle.State.STARTED
   )
   SideEffect {
      logVerbose(tag, "lifecycleOwner:${lifecycleOwner.toString()} lifecycle.State:${lifecycle.currentState.toString()}")
      logVerbose(tag, "${uiState.toString()}")
   }
   return uiState
}