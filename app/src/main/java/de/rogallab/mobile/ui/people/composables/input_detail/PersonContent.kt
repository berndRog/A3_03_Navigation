package de.rogallab.mobile.ui.people.composables.input_detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.utilities.logComp
import de.rogallab.mobile.ui.people.PersonUiState
import de.rogallab.mobile.ui.people.PersonValidator
import de.rogallab.mobile.ui.people.composables.SelectAndShowImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonContent(
   personUiState: PersonUiState,
   validator: PersonValidator,
   onFirstNameChange: (String) -> Unit,
   onLastNameChange: (String) -> Unit,
   onEmailChange: (String) -> Unit,
   onPhoneChange: (String) -> Unit
) {
   val tag = "<-PersonContent"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { logComp(tag, "Composition #${nComp.value++}") }

   Column {
      InputName(
         name = personUiState.person.firstName,
         onNameChange = onFirstNameChange,
         label = stringResource(R.string.firstName),
         validateName = validator::validateFirstName,
      )
      InputName(
         name = personUiState.person.lastName,
         onNameChange = onLastNameChange,
         label = stringResource(R.string.lastName),
         validateName = validator::validateLastName,
      )
      InputEmail(
         email = personUiState.person.email ?: "",
         onEmailChange = onEmailChange,
         validateEmail = validator::validateEmail
      )
      InputPhone(
         phone = personUiState.person.phone ?: "",
         onPhoneChange = onPhoneChange,
         validatePhone = validator::validatePhone
      )
   }
}
