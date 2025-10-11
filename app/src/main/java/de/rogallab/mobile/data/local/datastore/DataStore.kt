package de.rogallab.mobile.data.local.datastore

import android.content.Context
import de.rogallab.mobile.Globals.DIRECTORY_NAME
import de.rogallab.mobile.Globals.FILE_NAME
import de.rogallab.mobile.data.IDataStore
import de.rogallab.mobile.data.local.Seed
import de.rogallab.mobile.domain.IAppStorage
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.logDebug
import de.rogallab.mobile.domain.utilities.logError
import de.rogallab.mobile.domain.utilities.logVerbose
import kotlinx.serialization.json.Json
import java.io.File

class DataStore(
   private val _context: Context,
   private val _appStorage: IAppStorage,
   directoryName: String?,
   fileName: String?,
   private val _isTest: Boolean = false
) : IDataStore {

   // directory and file name for the dataStore from MainApplication
   // get the Apps home directory
   private val _appHome:String = _context.filesDir.toString()

   val filePath = getOrCreateFilePath(
      appHome =  _appHome,
      directoryName = directoryName ?: DIRECTORY_NAME,
      fileName = fileName ?: FILE_NAME
   )

   // mutable set of people internal
   private var _people: MutableSet<Person> = mutableSetOf()
   // immutable list of people external
   val people: List<Person>
      get() = _people.toList()

   // Json serializer
   private val _json = Json {
      prettyPrint = true
      ignoreUnknownKeys = true
   }

   override fun initialize() {
      logDebug(TAG, "init: read datastore")
      _people.clear()

      // /users/home/documents/android/peoplek08.json
      var file = File(filePath)
      if (!file.exists() || file.readText().isBlank()) {
         // seed _people with some data
         val seed = Seed(_context, _appStorage, _isTest)
         _people.addAll(seed.people)
         logVerbose(TAG, "create(): seedData ${_people.size} people")
         write()
      }
      // read people from JSON file
      read()
   }

   override fun selectAll(): List<Person> =
      _people.toList()

   // sort case-insensitive by selector
   override fun selectAllSortedBy(selector: (Person) -> String?): List<Person> =
      _people.sortedBy { person -> selector(person)?.lowercase() }
         .toList()

   override fun selectWhere(predicate: (Person) -> Boolean): List<Person> =
      _people.filter(predicate)
         .toList()

   override fun findById(id: String): Person? =
      _people.firstOrNull { it: Person -> it.id == id }

   override fun findBy(predicate: (Person) -> Boolean): Person? =
      _people.firstOrNull(predicate)

   override fun insert(person: Person) {
      logVerbose(TAG, "insert: $person")
      if (_people.any { it.id == person.id }) return
      // throw IllegalArgumentException("Person with id ${person.id} already exists")
      _people.add(person)
      write()
   }

   override fun update(person: Person) {
      logVerbose(TAG, "update: $person")
      val existing = _people.firstOrNull { it.id == person.id }
      if (existing == null)
         throw IllegalArgumentException("Person with id ${person.id} does not exist")
      _people.remove(existing)
      _people.add(person)
      write()
   }

   override fun delete(person: Person) {
      logVerbose(TAG, "delete: $person")
      if (_people.none { it.id == person.id })
         throw IllegalArgumentException("Person with id ${person.id} does not exist")
      _people.remove(person)
      write()
   }

   // list of people is saved as JSON file to the user's home directory
   private fun read() {
      try {
         // val filePath = getOrCreateFilePath(_appHome, directoryName, fileName)
         // read json from a file and convert to a list of people
         val jsonString = File(filePath).readText()
         logVerbose(TAG, jsonString)
         _people = _json.decodeFromString<MutableSet<Person>>(jsonString)
         logDebug(TAG, "read(): decode JSON ${_people.size} Ppeople")
      } catch (e: Exception) {
         logError(TAG, "Failed to read: ${e.message}")
         throw e
      }
   }

   // write the list of people to the dataStore as JSON file
   private fun write() {
      try {
         // val filePath = getOrCreateFilePath(_appHome, directoryName, fileName)
         val jsonString = _json.encodeToString(_people)
         logDebug(TAG, "write(): encode JSON ${_people.size} people")
         // save to a file
         File(filePath).writeText(jsonString)
         logVerbose(TAG, jsonString)
      } catch (e: Exception) {
         logError(TAG, "Failed to write: ${e.message}")
         throw e
      }
   }

   companion object {

      private const val TAG = "<-DataStore"

      // get the file path for the dataStore
      // UserHome/documents/android/people.json
      fun getOrCreateFilePath(
         appHome: String,
         directoryName: String,
         fileName: String
      ): String {
         try {
            // the directory must exist, if not create it
            val directoryPath = "$appHome/documents/$directoryName"
            if (!directoryExists(directoryPath)) {
               val result = createDirectory(directoryPath)
               if (!result) {
                  throw Exception("Failed to create directory: $directoryPath")
               }
            }

            // create the file path
            val filePath = "$directoryPath/$fileName"
            // create the file if it doesn't exist
            val file = File(filePath)
            if (!file.exists()) {
               file.createNewFile()
               logDebug(TAG, "Created new file: $filePath")
            }
            // return the file path
            return filePath
         } catch (e: Exception) {
            logError(TAG, "Failed to getFilePath or create directory; ${e.message}")
            throw e
         }
      }

      private fun directoryExists(directoryPath: String): Boolean {
         val directory = File(directoryPath)
         return directory.exists() && directory.isDirectory
      }

      private fun createDirectory(directoryPath: String): Boolean {
         val directory = File(directoryPath)
         return directory.mkdirs()
      }
   }
}