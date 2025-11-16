package de.rogallab.mobile.ui.people.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.Globals
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.logDebug
import kotlinx.coroutines.delay
/**
 * SwipePersonListItem — Algorithmic Overview
 *
 * PURPOSE
 *  - Displays a row that reacts to horizontal swipe gestures:
 *      • StartToEnd (left → right): triggers edit navigation
 *      • EndToStart (right → left): triggers a delete animation + Undo prompt
 *  - The gesture acts only as an *input trigger*. The component itself never
 *    remains in a visually dismissed state. Instead, it starts a controlled
 *    exit animation and defers the actual data mutation to the ViewModel.
 *
 * STATE
 *  - `isRemoved`: ephemeral UI state controlling the AnimatedVisibility exit.
 *      • Initialized with `remember(person.id)` to reset cleanly after Undo.
 *  - `SwipeToDismissBoxState`: detects swipe direction only; immediately reset
 *    to `Settled` so Compose’s internal dismiss logic never takes over.
 *
 * ALGORITHM
 *  1) User swipes → state.currentValue changes.
 *  2) If StartToEnd → call `onNavigate(person.id)` and snap back.
 *  3) If EndToStart → set `isRemoved = true` to trigger the exit animation,
 *     then snap back (we manage visuals ourselves).
 *  4) A `LaunchedEffect(isRemoved)` waits for the animation duration, then:
 *       • Calls `onDelete()` to update UI + repository via ViewModel.
 *       • Calls `onUndo()` to show the Snackbar with Undo action.
 *  5) If the same person is restored later, Compose recomposes with a new key,
 *     resetting `isRemoved = false` automatically.
 *
 * WHY IT WORKS
 *  - **Decoupled gesture & state:** prevents conflicts with internal dismiss logic.
 *  - **Predictable Undo:** keying state by person.id guarantees a fresh state.
 *  - **Smooth UX:** user gets immediate visual feedback; expensive I/O happens later.
 *
 * This pattern demonstrates an *Optimistic-then-Persist* update:
 * The UI responds instantly, while persistence catches up asynchronously.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipePersonListItemWithUndo(
   person: Person,
   onNavigate: (String) -> Unit,
   onRemove: () -> Unit,
   onUndo: () -> Unit,
   content: @Composable () -> Unit
) {
   val tag = "<-SwipePersonListItem"

   var isRemoved by remember(person.id) { mutableStateOf(false) }

   val state = rememberSwipeToDismissBoxState(
      positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold
   )

   // Handle swipe actions when the state changes
   LaunchedEffect(state.currentValue) {
      when (state.currentValue) {
         SwipeToDismissBoxValue.StartToEnd -> {
            logDebug(tag, "Swipe to Edit for ${person.firstName} ${person.lastName}")
            onNavigate(person.id)
            // Reset to settled position
            state.snapTo(SwipeToDismissBoxValue.Settled)
         }
         SwipeToDismissBoxValue.EndToStart -> {
            logDebug(tag, "Swipe to Delete for ${person.firstName} ${person.lastName}")
            isRemoved = true
            // Reset to settled position
            state.snapTo(SwipeToDismissBoxValue.Settled)
         }
         SwipeToDismissBoxValue.Settled -> {
            // Do nothing - this is the default state
         }
      }
   }

   // Reset swipe state on identity change (e.g., after Undo)
   LaunchedEffect(person.id) {
      state.snapTo(SwipeToDismissBoxValue.Settled)
   }

   // After the exit animation finishes, perform the actual remove and prompt Undo
   LaunchedEffect(isRemoved, person.id) {
      if (isRemoved) {
         delay(Globals.animationDuration.toLong())
         onRemove()
         onUndo()
      }
   }

   AnimatedVisibility(
      visible = !isRemoved,
      exit = shrinkVertically(
         animationSpec = tween(durationMillis = Globals.animationDuration),
         shrinkTowards = Alignment.Top
      ) + fadeOut(
         animationSpec = spring(
            dampingRatio = Spring.DampingRatioHighBouncy,
            stiffness = Spring.StiffnessHigh,
            visibilityThreshold = 0.002f
         )
      )
   ) {
      SwipeToDismissBox(
         state = state,
         backgroundContent = { SwipeSetBackground(state) },
         enableDismissFromStartToEnd = true,
         enableDismissFromEndToStart = true,
         modifier = Modifier.padding(vertical = 4.dp)
      ) {
         content()
      }
   }
}