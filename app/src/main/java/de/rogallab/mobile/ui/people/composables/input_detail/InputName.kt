package de.rogallab.mobile.ui.people.composables.input_detail

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.ImeAction

@Composable
fun InputName(
   name: String,                                      // State ↓
   onNameChange: (String) -> Unit,                    // Event ↑
   label: String,                                     // State ↓
   validateName: (String) -> Pair<Boolean, String>    // Event ↑
) {
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