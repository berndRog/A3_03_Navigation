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
   navHandler: INavHandler,
   private val _validator: PersonValidator
): BaseViewModel(navHandler, TAG) {

   init { logDebug(TAG, "init instance=${System.identityHashCode(this)}") }

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
         is PersonIntent.ImageChange -> onImageChange(intent.uriString)

         is PersonIntent.Clear -> clearState()
         is PersonIntent.FetchById -> fetchById(intent.id)
         is PersonIntent.Create -> create()
         is PersonIntent.Update -> update()
         is PersonIntent.Remove -> remove(intent.person)
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
   // endregion

   // region Single-slot UNDO buffer ---------------------------------------------------------------
   private var _removedPerson: Person? = null
   private var _removedPersonIndex: Int = -1 // Store only the index

   /**
    * REMOVE (Optimistic-then-Persist)
    * Step-by-step:
    * 1) Find the index by ID (more robust than instance equality)
    * 2) Store (person, index) in the undo buffer
    * 3) Update UI state immediately (remove from list)
    * 4) Persist the deletion in background (repository call)
    */
   private fun remove(person: Person) {
      logDebug(TAG, "removePerson()")

      // Find index of person to remove
      val currentList = _peopleUiStateFlow.value.people
      val index = currentList.indexOfFirst { it.id == person.id }  // <— statt indexOf(person)
      if (index == -1) return
      _removedPerson = person
      _removedPersonIndex = index

      // Remove person in StateFlow and
      // immediately update UI - without data handling
      val updatedList = currentList.toMutableList().also { it.removeAt(index) }
      updateState(_peopleUiStateFlow) { copy(people = updatedList) }

      // Remove person from repository in background
      _repository.remove(person)
         .onFailure { t -> handleErrorEvent(t) }
   }

   /**
    * UNDO (Optimistic-then-Persist)
    * 1) Read undo buffer; abort if empty
    * 2) Reinsert into current UI at the old index (coerceAtMost avoids OOB if list length changed)
    * 3) Set restoredPersonId so the UI can scroll to it
    * 4) Persist create back in the repository
    * 5) Clear the undo buffer
    */
   private fun undoRemove() {
      // Restore the last removed person if any
      val personToRestore = _removedPerson ?: return
      val indexToRestore = _removedPersonIndex
      if (indexToRestore == -1) return
      logDebug(TAG, "undoRemovePerson: ${personToRestore.id}")

      // Restore person in StateFlow and
      // immediately update UI - without data handling
      val currentList = _peopleUiStateFlow.value.people.toMutableList()
      if (currentList.any { it.id == personToRestore.id }) return // already in the list
      // Reinsert at old index (or at end if list got shorter)
      currentList.add(indexToRestore.coerceAtMost(currentList.size), personToRestore)
      updateState(_peopleUiStateFlow) {
         copy(people = currentList, restoredPersonId = personToRestore.id)
      }

      // Add person back to repository in background
      _repository.create(personToRestore)
         .onFailure { t -> handleErrorEvent(t)
      }
      _removedPerson = null
      _removedPersonIndex = -1
   }

   /** RESTORED (Optimistic-then-Persist)
    * The UI has scrolled to the restored item and acknowledges this by sending PersonIntent.Restored.
    * We then clear the ID in the PeopleUiState to avoid repeated scrolling on recomposition.
    */
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
   // endregion

   companion object {
      private const val TAG = "<-PersonViewModel"
   }
}