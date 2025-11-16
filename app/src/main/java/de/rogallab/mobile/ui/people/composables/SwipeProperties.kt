package de.rogallab.mobile.ui.people.composables

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
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp


data class SwipeProperties(
   val colorBox: Color,
   val colorIcon: Color,
   val alignment: Alignment,
   val icon: ImageVector,
   val description: String,
   val scale: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeSetBackground(state: SwipeToDismissBoxState) {
   val (colorBox, colorIcon, alignment, icon, description, scale) =
      SwipeGetProperties(state)

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
fun SwipeGetProperties(
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

   val scale = if (state.targetValue == SwipeToDismissBoxValue.Settled) 1.2f else 1.2f

   return SwipeProperties(colorBox, colorIcon, alignment, icon, description, scale)
}

