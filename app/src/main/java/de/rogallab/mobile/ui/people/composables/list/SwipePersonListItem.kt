package de.rogallab.mobile.ui.people.composables.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.Globals.ANIMATION_DURATION
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
fun SwipePersonListItem(
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
         delay(ANIMATION_DURATION.toLong())
         onRemove()
         onUndo()
      }
   }

   AnimatedVisibility(
      visible = !isRemoved,
      exit = shrinkVertically(
         animationSpec = tween(durationMillis = ANIMATION_DURATION),
         shrinkTowards = Alignment.Top
      ) + fadeOut()
   ) {
      SwipeToDismissBox(
         state = state,
         backgroundContent = { SetSwipeBackground(state) },
         enableDismissFromStartToEnd = true,
         enableDismissFromEndToStart = true,
         modifier = Modifier.padding(vertical = 4.dp)
      ) {
         content()
      }
   }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetSwipeBackground(state: SwipeToDismissBoxState) {
   val (colorBox, colorIcon, alignment, icon, description, scale) =
      GetSwipeProperties(state)

   Box(
      Modifier
         .fillMaxSize()
         .background(
            color = colorBox,
            shape = RoundedCornerShape(10.dp)
         )
         .padding(horizontal = 16.dp),
      contentAlignment = alignment
   ) {
      Icon(
         imageVector = icon,
         contentDescription = description,
         modifier = Modifier.scale(scale),
         tint = colorIcon
      )
   }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GetSwipeProperties(
   state: SwipeToDismissBoxState
): SwipeProperties {
   val direction = state.dismissDirection

   val colorBox: Color = when (direction) {
      SwipeToDismissBoxValue.StartToEnd -> Color(0xFF008000) // Green
      SwipeToDismissBoxValue.EndToStart -> Color(0xFFB22222) // Firebrick Red
      else -> MaterialTheme.colorScheme.surface
   }
   val colorIcon: Color = Color.White

   val alignment: Alignment = when (direction) {
      SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
      SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
      else -> Alignment.Center
   }

   val icon: ImageVector = when (direction) {
      SwipeToDismissBoxValue.StartToEnd -> Icons.Outlined.Edit
      SwipeToDismissBoxValue.EndToStart -> Icons.Outlined.Delete
      else -> Icons.Outlined.Info
   }

   val description: String = when (direction) {
      SwipeToDismissBoxValue.StartToEnd -> "Edit"
      SwipeToDismissBoxValue.EndToStart -> "Delete"
      else -> "Unknown Action"
   }

   val scale = if (state.targetValue == SwipeToDismissBoxValue.Settled) 1.2f else 1.8f

   return SwipeProperties(colorBox, colorIcon, alignment, icon, description, scale)
}

data class SwipeProperties(
   val colorBox: Color,
   val colorIcon: Color,
   val alignment: Alignment,
   val icon: ImageVector,
   val description: String,
   val scale: Float
)