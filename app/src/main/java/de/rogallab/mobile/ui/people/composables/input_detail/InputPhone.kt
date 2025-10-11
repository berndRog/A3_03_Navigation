package de.rogallab.mobile.ui.people.composables.input_detail

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import de.rogallab.mobile.R

@Composable
fun InputPhone(
   phone: String,                                     // State ↓
   onPhoneChange: (String) -> Unit,                   // Event ↑
   label: String = stringResource(R.string.phone),    // State ↓
   validatePhone: (String) -> Pair<Boolean, String>,  // Event ↑
) {
   InputTextField(
      value = phone,
      onValueChange = onPhoneChange,
      label = label,
      validate = validatePhone,
      leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = label) },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
      imeAction = ImeAction.Done
   )
}