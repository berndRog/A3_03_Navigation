package de.rogallab.mobile.ui.base.composables

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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import de.rogallab.mobile.domain.utilities.logComp
import de.rogallab.mobile.domain.utilities.logDebug
import kotlin.let
import kotlin.to

@Composable
fun InputValueString(
   value: String,
   onValueChange: (String) -> Unit,
   label: String,
   leadingIcon: ImageVector? = null,
   validate: (String) -> Pair<Boolean, String> = { false to "" },
   keyboardType: KeyboardType = KeyboardType.Text,
   imeAction: ImeAction = ImeAction.Done,
   modifier: Modifier = Modifier,
) {
   val tag = "<-InputStringValue"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { logComp(tag, "Composition #${nComp.value++}") }

   var isError by rememberSaveable { mutableStateOf(false) }
   var errorMessage by rememberSaveable { mutableStateOf("") }
   val focusManager = LocalFocusManager.current

   LaunchedEffect(value) {
      isError = false
      errorMessage = ""
   }

   fun validateAndPropagate(newValue: String) {
      logDebug(tag, "validateAndPropagate $newValue")
      val (error, text) = validate(newValue)
      isError = error
      errorMessage = text
      onValueChange(newValue)
   }

   OutlinedTextField(
      modifier = modifier
         .fillMaxWidth()
         .onFocusChanged { focusState ->
            if (!focusState.isFocused) { validateAndPropagate(value) }
         },
      value = value,
      onValueChange = { it ->
         logDebug(tag, "onValueChange $it")
         onValueChange(it)
                      },
      label = { Text(label) },
      textStyle = MaterialTheme.typography.bodyLarge,
      leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = label) } },
      singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = keyboardType,
                                        imeAction = imeAction),
      keyboardActions = KeyboardActions(
         onAny = {
            validateAndPropagate(value)
            if (!isError) focusManager.clearFocus()
         }
      ),
      isError = isError,
      supportingText = {
         if (isError) Text(text = errorMessage,
                           color = MaterialTheme.colorScheme.error)
      },
      trailingIcon = {
         if (isError) Icon(imageVector = Icons.Filled.Error,
                           contentDescription = errorMessage,
                           tint = MaterialTheme.colorScheme.error)
      }
   )
}
