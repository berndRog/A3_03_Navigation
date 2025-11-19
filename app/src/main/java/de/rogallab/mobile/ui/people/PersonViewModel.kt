package de.rogallab.mobile.ui.people

import androidx.compose.material3.SnackbarDuration
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.logDebug
import de.rogallab.mobile.domain.utilities.newUuid
import de.rogallab.mobile.ui.base.BaseViewModel
import de.rogallab.mobile.ui.base.updateState
import de.rogallab.mobile.ui.navigation.INavHandler
import de.rogallab.mobile.ui.navigation.PeopleList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PersonViewModel(
   private val _repository: IPersonRepository,
   private val _navHandler: INavHandler,
   private val _validator: PersonValidator
): BaseViewModel(_navHandler, TAG) {

   init { logDebug(TAG, "init instance=${System.identityHashCode(this)}") }

   override fun onCleared() {
      logDebug(TAG, "onCleared instance=${System.identityHashCode(this)}")
      super.onCleared()
   }

   // region StateFlows and Intent handlers --------------------------------------------------------
   // StateFlow for PeopleListScreen ---------------------------------------------------------------
   private val _peopleUiStateFlow: MutableStateFlow<PeopleUiState> =
      MutableStateFlow(PeopleUiState())
   val peopleUiStateFlow: StateFlow<PeopleUiState> =
      _peopleUiStateFlow.asStateFlow()

   // Transform PeopleIntent into an action
   fun handlePeopleIntent(intent: PeopleIntent) {
      when (intent) {
         is PeopleIntent.Fetch -> fetch()
         is PeopleIntent.Clean -> cleanUp()
      }
   }

   // StateFlow for PersonInput-/ PersonDetailScreen -----------------------------------------------
   private val _personUiStateFlow: MutableStateFlow<PersonUiState> =
      MutableStateFlow(PersonUiState())
   val personUiStateFlow: StateFlow<PersonUiState> =
      _personUiStateFlow.asStateFlow()

   // Transform PersonIntent into an action --------------------------------------------------------
   fun handlePersonIntent(intent: PersonIntent) {
      when (intent) {
         is PersonIntent.FirstNameChange -> onFirstNameChange(intent.firstName)
         is PersonIntent.LastNameChange -> onLastNameChange(intent.lastName)
         is PersonIntent.EmailChange -> onEmailChange(intent.email)
         is PersonIntent.PhoneChange -> onPhoneChange(intent.phone)
         is PersonIntent.ImagePathChange -> onImageChange(intent.uriString)

         is PersonIntent.Clear -> clearState()
         is PersonIntent.FetchById -> fetchById(intent.id)
         is PersonIntent.Create -> create()
         is PersonIntent.Update -> update()
         is PersonIntent.Remove -> remove(intent.person)

         is PersonIntent.RemoveUndo -> removeUndo(intent.person)
         is PersonIntent.Undo -> undoRemove()
         is PersonIntent.Restored -> restored()

         is PersonIntent.ErrorEvent -> handleErrorEvent(message = intent.message)
         is PersonIntent.UndoEvent -> handleUndoEvent(intent.errorState)
      }
   }
   // endregion

   // region Input updates (immutable copy, trimmed) -----------------------------------------------
   private fun onFirstNameChange(firstName: String) =
      updateState(_personUiStateFlow) {
         copy(person = person.copy(firstName = firstName.trim()))
      }
   private fun onLastNameChange(lastName: String) =
      updateState(_personUiStateFlow) {
         copy(person = person.copy(lastName = lastName.trim()))
      }
   private fun onEmailChange(email: String?) =
      updateState(_personUiStateFlow) {
         copy(person = person.copy(email = email?.trim()))
      }
   private fun onPhoneChange(phone: String?) =
      updateState(_personUiStateFlow) {
         copy(person = person.copy(phone = phone?.trim()))
      }
   private fun onImageChange(uriString: String?) =
      updateState(_personUiStateFlow) {
         copy(person = person.copy(imagePath = uriString?.trim()))
      }
   // clear person state and prepare for new person input
   private fun clearState() =
      updateState(_personUiStateFlow) {
         copy(person = Person(id = newUuid() ))
      }
   // endregion

   // region Fetch by id (error → navigate back to list) -------------------------------------------
   private fun fetchById(id: String) {
      logDebug(TAG, "fetchById() $id")
      _repository.findById(id)
         .onSuccess { person ->
            if(person != null) {
               logDebug(TAG, "fetchPersonById")
               updateState(_personUiStateFlow) { copy(person = person) }
            } else {
               handleErrorEvent(
                  message = "Person not found",
                  navKey = PeopleList, // navigate to PeopleListScreen
               )
            }
         }
         .onFailure { t ->
            handleErrorEvent(
               throwable = t,
               navKey = PeopleList, // navigate to PeopleListScreen
            )
         }
   }
   // endregion

   // region Create/Update (persist then refresh list) --------------------------
   private fun create() {
      logDebug(TAG, "createPerson")
      _repository.create(_personUiStateFlow.value.person)
         .onSuccess { fetch() } // reread all people
         .onFailure { t -> handleErrorEvent(t) }
   }
   private fun update() {
      logDebug(TAG, "updatePerson()")
      _repository.update(_personUiStateFlow.value.person)
         .onSuccess { fetch() } // reread all people
         .onFailure { t -> handleErrorEvent(t) }
   }
   private fun remove(person: Person) {
      logDebug(TAG, "removePerson()")
      _repository.remove(person)
         .onSuccess { fetch() } // reread all people
         .onFailure { t -> handleErrorEvent(t) }
   }
   // endregion

   // region Single-slot UNDO buffer ---------------------------------------------------------------
   private var _removedPerson: Person? = null
   private var _removedPersonIndex: Int = -1 // Store only the index

   private fun removeUndo(person: Person) {
      logDebug(TAG, "removePerson()")
      removeItem(
         item = person,
         currentList = _peopleUiStateFlow.value.people,
         getId = { it.id },
         onRemovedItem = { _removedPerson = it as? Person },
         onRemovedItemIndex = { _removedPersonIndex = it },
         updateUi = { updatedList ->
            updateState(_peopleUiStateFlow) { copy(people = updatedList) }
         },
         persistRemove = { _repository.remove(it) },
         tag = TAG
      )
   }

   private fun undoRemove() {
      undoItem(
         currentList = _peopleUiStateFlow.value.people,
         getId = { it.id },
         removedItem = _removedPerson,
         removedIndex = _removedPersonIndex,
         updateUi = { restoredList, restoredId ->
            updateState(_peopleUiStateFlow) { copy(people = restoredList, restoredPersonId = restoredId) }
         },
         persistCreate = { _repository.create(it) },
         onReset = {
            _removedPerson = null
            _removedPersonIndex = -1
         },
         tag = TAG
      )
   }

   private fun restored() {
      logDebug(TAG, "restored() acknowledged by UI")
      // The UI has finished scrolling, so we clear the ID
      updateState(_peopleUiStateFlow) { copy(restoredPersonId = null) }
   }
   // endregion

   // region Validation ----------------------------------------------------------------------------
   // validate all input fields after user finished input into the form
   fun validate(): Boolean {
      val person = _personUiStateFlow.value.person
      // only one error message can be processed at a time
      if(!validateAndLogError(_validator.validateFirstName(person.firstName)))
         return false
      if(!validateAndLogError(_validator.validateLastName(person.lastName)))
         return false
      if(!validateAndLogError(_validator.validateEmail(person.email)))
         return false
      if(!validateAndLogError(_validator.validatePhone(person.phone)))
         return false
      return true // all fields are valid
   }

   private fun validateAndLogError(validationResult: Pair<Boolean, String>): Boolean {
      val (error, message) = validationResult
      if (error) {
         handleErrorEvent(
            message = message,
            withDismissAction = true,
            onDismissed = { /* no op, Unit returned */ },
            duration = SnackbarDuration.Long,
            navKey = null, // stay on the screen
         )
         return false
      }
      return true
   }
   // endregion

   // region Fetch all (persisted → UI) ------------------------------------------------------------
   // read all people from repository
   private fun fetch() {
      logDebug(TAG, "fetch")
      updateState(_peopleUiStateFlow) { copy(isLoading = true) }

      _repository.getAllSortedBy { it.firstName }
         .onSuccess { people ->
            val snapshot = people.toList()
            updateState(_peopleUiStateFlow) {
               logDebug(TAG, "apply PeopleUiState: isLoading=false size=${snapshot.size}")
               copy(
                  isLoading = false,
                  people = snapshot //new instance of a list
               )
            }
         }
         .onFailure { t -> handleErrorEvent(t) }
   }

   private fun cleanUp() {
      logDebug(TAG, "cleanUp()")
      updateState(_peopleUiStateFlow) { copy(isLoading = false) }
   }
   // endregion

   companion object {
      private const val TAG = "<-PersonViewModel"
   }
}