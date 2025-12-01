package de.rogallab.mobile.ui.people

import androidx.compose.material3.SnackbarDuration
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.undoredo.SingleSlotUndoBuffer
import de.rogallab.mobile.domain.undoredo.optimisticRemove
import de.rogallab.mobile.domain.undoredo.optimisticUndoRemove
import de.rogallab.mobile.domain.utilities.logDebug
import de.rogallab.mobile.domain.utilities.newUuid
import de.rogallab.mobile.ui.base.BaseViewModel
import de.rogallab.mobile.ui.base.updateState
import de.rogallab.mobile.ui.navigation.INavHandler
import de.rogallab.mobile.ui.navigation.PeopleList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
         is PersonIntent.ImagePathChange -> onImagePathChange(intent.uriString)

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
   private fun onFirstNameChange(firstName: String) {
      val trimmed = firstName.trim()
      if(trimmed == _personUiStateFlow.value.person.firstName) return
      updateState(_personUiStateFlow) { copy(person = person.copy(firstName = trimmed)) }
   }
   private fun onLastNameChange(lastName: String) {
      val trimmed = lastName.trim()
      if (trimmed == _personUiStateFlow.value.person.lastName) return
      updateState(_personUiStateFlow) { copy(person = person.copy(lastName = lastName.trim())) }
   }
   private fun onEmailChange(email: String?) {
      var trimmed = email?.trim()
      if (trimmed == _personUiStateFlow.value.person.email) return
      updateState(_personUiStateFlow) { copy(person = person.copy(email = email?.trim())) }
   }
   private fun onPhoneChange(phone: String?) {
      val trimmed = phone?.trim()
      if (trimmed == _personUiStateFlow.value.person.phone) return
      updateState(_personUiStateFlow) { copy(person = person.copy(phone = trimmed)) }
   }
   private fun onImagePathChange(uriString: String?) {
      val trimmed = uriString?.trim()
      if (trimmed == _personUiStateFlow.value.person.imagePath) return
      updateState(_personUiStateFlow) { copy(person = person.copy(imagePath = trimmed)) }
   }

   // clear person state and prepare for new person input
   private fun clearState() =
      updateState(_personUiStateFlow) { copy(person = Person(id = newUuid())) }
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
   /**
    * Single-slot undo buffer for the last removed Person.
    *
    * This buffer stores:
    *  - the removed Person instance
    *  - the index from which it was removed
    */
   private var _undoBuffer = SingleSlotUndoBuffer<Person>()
   /**
    * Removes a person using the "Optimistic-then-Persist" pattern.
    *
    * Optimistic:
    *  - The UI list is updated immediately so the UI feels instant.
    *  - The removed item and index are stored in the undo buffer.
    *
    * Then-Persist:
    *  - The actual repository deletion happens in the background.
    *  - If persistence fails, an error event is emitted.
    */
   private fun removeUndo(person: Person) {
      logDebug(TAG, "removeUndo(${person.id})")

      val currentList = _peopleUiStateFlow.value.people

      // 1) Pure helper: perform optimistic list change and build undo buffer
      val (updatedList, newBuffer) = optimisticRemove(
         list = currentList,
         item = person,
         getId = { it.id },
         undoBuffer = _undoBuffer
      )

      // If nothing changed (item not found), stop here
      if (updatedList === currentList) return

      // 2) Update undo buffer
      _undoBuffer = newBuffer

      // 3) Immediately update UI state (Optimistic)
      updateState(_peopleUiStateFlow) { copy(people = updatedList) }

      // 4) Persist removal asynchronously (Then-Persist)
      viewModelScope.launch {
         logDebug(TAG, "persistRemove(${person.id})")
         _repository.remove(person)
            .onFailure { t -> handleErrorEvent(t) }
      }
   }
   /**
    * Undoes the last removed person using the pure helper.
    *
    * Optimistic:
    *  - The item is reinserted immediately into the UI list.
    *  - The undo buffer is cleared.
    * Then-Persist:
    *  - The Person is recreated in the repository in the background.
    *
    * If the Person was already present in the list (rare edge case),
    * only the buffer is cleared and no repository call is made.
    */
   private fun undoRemove() {
      logDebug(TAG, "undoRemove()")

      val currentList = _peopleUiStateFlow.value.people

      // 1) Pure helper: restore from undo buffer
      val result = optimisticUndoRemove(
         list = currentList,
         getId = { it.id },
         undoBuffer = _undoBuffer
      )

      // Extract results
      val updatedList = result.updatedList
      val restoredId = result.restoredId

      // 2) Clear or update undo buffer
      _undoBuffer = result.newBuffer

      // If nothing changed, stop here
      if (updatedList === currentList && restoredId == null) return

      // 3) Update UI state immediately (Optimistic)
      updateState(_peopleUiStateFlow) {
         copy(people = updatedList, restoredPersonId = restoredId) }

      // 4) Persist recreation in the background (Then-Persist)
      if (restoredId != null) {
         val restoredPerson = updatedList.firstOrNull { it.id == restoredId }
         if (restoredPerson != null) {
            viewModelScope.launch {
               logDebug(TAG, "persistCreate(${restoredPerson.id})")
               _repository.create(restoredPerson)
                  .onFailure { t -> handleErrorEvent(t) }
            }
         }
      }
   }
   /**
    * Called by the UI (e.g., LazyColumn) once the scroll animation
    * to `restoredPersonId` has finished.
    *
    * This clears the flag so future restore operations can update it again.
    */
   private fun restored() {
      logDebug(TAG, "restored() acknowledged by UI")
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