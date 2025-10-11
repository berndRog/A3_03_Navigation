package de.rogallab.mobile.ui.people.composables.input_detail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import de.rogallab.mobile.domain.utilities.logDebug
import de.rogallab.mobile.domain.utilities.logVerbose
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun InputTextField(
   value: String,                                  // State ↓
   onValueChange: (String) -> Unit,                // Event ↑
   label: String,                                  // Value ↓
   validate: (String) -> Pair<Boolean, String>,    // Event ↑
   leadingIcon: @Composable (() -> Unit)? = null,  // Composable ↑
   keyboardOptions: KeyboardOptions =              // Value ↓
      KeyboardOptions.Default,
   imeAction: ImeAction = ImeAction.Done,          // State ↓
) {
   // local state for error handling
   var isError by rememberSaveable { mutableStateOf(false) }
   var errorText by rememberSaveable { mutableStateOf("") }
   val focusManager = LocalFocusManager.current

   // delete error state when the value changes
   LaunchedEffect(value) {
      isError = false
      errorText = ""
   }

   // Validate the input when focus is lost
   fun validateAndPropagate(newValue: String) {
      val (error, text) = validate(newValue)
      isError = error
      errorText = text
      onValueChange(newValue)
   }

   OutlinedTextField(
      modifier = Modifier
         .fillMaxWidth()
         .onFocusChanged { focusState ->
            // logVerbose("<-InputTextField","onFocusChanged !focusState.isFocused ${!focusState.isFocused} isFocus $isFocus")
            if (!focusState.isFocused) {
               validateAndPropagate(value)
            }
         },

      value = value,
      onValueChange = { validateAndPropagate(it) },
      // Set the label of the text field
      label = { Text(label) },
      textStyle = MaterialTheme.typography.bodyLarge,
      // Add leading icon to the text field
      leadingIcon = leadingIcon,
      // Ensure the text field is single line
      singleLine = true,
      // Set keyboard options for the text field
      keyboardOptions = keyboardOptions.copy(imeAction = imeAction),
      // Set keyboard actions for the text field
      keyboardActions = KeyboardActions(
         onAny = {
            validateAndPropagate(value)
            if (!isError) focusManager.clearFocus()
         }
      ),
      // Is there an error?
      isError = isError,
      // Provide supporting text and trailing icon if there is an error
      supportingText = {
         if (isError) Text(
            text = errorText,
            color = MaterialTheme.colorScheme.error
         )
      },
      trailingIcon = {
         if (isError) Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = errorText,
            tint = MaterialTheme.colorScheme.error
         )
      }
   )
}