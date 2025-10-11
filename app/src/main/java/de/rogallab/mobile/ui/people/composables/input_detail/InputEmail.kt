package de.rogallab.mobile.ui.people.composables.input_detail

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import de.rogallab.mobile.R

@Composable
fun InputEmail(
   email: String,                                     // State ↓
   onEmailChange: (String) -> Unit,                   // Event ↑
   label: String = stringResource(R.string.email),   // State ↓
   validateEmail: (String) -> Pair<Boolean, String>,  // Event ↑
) {
   InputTextField(
      value = email,
      onValueChange = onEmailChange,
      label = label,
      validate = validateEmail,
      leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = label) },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
      imeAction = ImeAction.Next
   )
}