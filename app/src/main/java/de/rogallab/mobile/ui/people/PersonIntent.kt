package de.rogallab.mobile.ui.people

import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.ui.errors.ErrorState

sealed class PersonIntent {
   data class  FirstNameChange(val firstName: String) : PersonIntent()
   data class  LastNameChange(val lastName: String) : PersonIntent()
   data class  EmailChange(val email: String) : PersonIntent()
   data class  PhoneChange(val phone: String) : PersonIntent()

   data object Clear : PersonIntent()
   data class  FetchById(val id: String) : PersonIntent()
   data object Create : PersonIntent()
   data object Update : PersonIntent()
   data class  Remove(val person: Person) : PersonIntent()
   data object Undo : PersonIntent()
   data object Restored : PersonIntent()

   data class HandleUndoEvent(val errorState: ErrorState) : PersonIntent()

}