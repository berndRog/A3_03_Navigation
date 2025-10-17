package de.rogallab.mobile.ui.people.composables.input_detail

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.input.ImeAction
import de.rogallab.mobile.domain.utilities.logComp

@Composable
fun InputName(
   name: String,                                      // State ↓
   onNameChange: (String) -> Unit,                    // Event ↑
   label: String,                                     // State ↓
   validateName: (String) -> Pair<Boolean, String>    // Event ↑
) {
   val tag = "<-InputName"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { logComp(tag, "Composition #${nComp.value++}") }

   InputTextField(
      value = name,
      onValueChange = onNameChange,
      label = label,
      validate = validateName,
      leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = label) },
      keyboardOptions = KeyboardOptions.Default,
      imeAction = ImeAction.Next
   )
}