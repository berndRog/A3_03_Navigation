package de.rogallab.mobile.ui.people.composables

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.utilities.logComp
import de.rogallab.mobile.domain.utilities.logDebug
import de.rogallab.mobile.ui.errors.ErrorHandler
import de.rogallab.mobile.ui.people.PersonIntent
import de.rogallab.mobile.ui.people.PersonValidator
import de.rogallab.mobile.ui.people.PersonViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
   id: String,
   viewModel: PersonViewModel,
   onNavigateReverse: () -> Unit = {},
) {
   val tag = "<-PersonDetailScreen"
   val nComp = remember{ mutableIntStateOf(1) }
   SideEffect { logComp(tag, "Composition #${nComp.intValue++}") }

   // observe the personUiStateFlow in the ViewModel
   val personUiState by viewModel.personUiStateFlow.collectAsStateWithLifecycle(
      minActiveState = Lifecycle.State.RESUMED,
   )
   SideEffect { logDebug(tag, "PersonUiState: ${personUiState.person}") }

   // fetch person by id
   LaunchedEffect(id) {
      viewModel.handlePersonIntent(PersonIntent.FetchById(id))
   }

   val snackbarHostState = remember { SnackbarHostState() }
   Scaffold(
      contentColor = MaterialTheme.colorScheme.onBackground,
      contentWindowInsets = WindowInsets.safeDrawing, // .safeContent .safeGestures,
      topBar = {
         TopAppBar(
            title = { Text(text = stringResource(R.string.personDetail)) },
            navigationIcon = {
               IconButton(onClick = {
                  if (viewModel.validate()) {
                     viewModel.handlePersonIntent(PersonIntent.Update)
                     onNavigateReverse()
                  }
               }) {
                  Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                     contentDescription = stringResource(R.string.back))
               }
            }
         )
      },
      snackbarHost = {
         SnackbarHost(hostState = snackbarHostState) { data ->
            Snackbar(
               snackbarData = data,
               actionOnNewLine = true
            )
         }
      },
      modifier = Modifier.fillMaxSize()
   ) { innerPadding ->
      Column(
         modifier = Modifier
            .padding(paddingValues = innerPadding)
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
      ) {
         PersonContent(
            personUiState = personUiState,
            validator = koinInject<PersonValidator>(),
            onFirstNameChange = {
               viewModel.handlePersonIntent(PersonIntent.FirstNameChange(it))
            },
            onLastNameChange = {
               viewModel.handlePersonIntent(PersonIntent.LastNameChange(it))
            },
            onEmailChange = {
               viewModel.handlePersonIntent(PersonIntent.EmailChange(it))
            },
            onPhoneChange = {
               viewModel.handlePersonIntent(PersonIntent.PhoneChange(it))
            }
         )
      }
   }

   // Error handling
   ErrorHandler(
      viewModel = viewModel,
      snackbarHostState = snackbarHostState
   )
}