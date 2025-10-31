package de.rogallab.mobile.data.local.datastore

import android.content.Context
import de.rogallab.mobile.Globals
import de.rogallab.mobile.data.IDataStore
import de.rogallab.mobile.data.local.Seed
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.logDebug
import de.rogallab.mobile.domain.utilities.logError
import de.rogallab.mobile.domain.utilities.logVerbose
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class DataStore(
   directoryName: String? = null,
   fileName:String? = null,
   private val _context: Context,
   private val _seed: Seed
) : IDataStore {

   // directory and file name for the dataStore from MainApplication
   private val _appHome:String = _context.filesDir.toString()
   private var _directoryName = directoryName ?: Globals.directory_name
   private val _fileName = fileName ?: Globals.file_name

   override val filePath: Path = getOrCreateFilePath(
      appHome = _appHome,
      directoryName = _directoryName,
      fileName = _fileName
   )

   // Internal lock for thread safety
   private val _lock = Any()
   // Internal: mutable set of people
   private var _people: MutableSet<Person> = mutableSetOf()
   // External: immutable list of people external
   val people: List<Person>
      get() = synchronized(_lock) { _people.toList() }

   // Json serializer
   private val _jsonSettings = Json {
      prettyPrint = true
      ignoreUnknownKeys = true
   }

   override fun initialize() {
      logDebug(TAG, "init: read datastore")
      synchronized(_lock) {
         _people.clear()

         // /users/home/documents/android/peoplek08.json
         val exists = Files.exists(filePath)
         val size = if (exists) try { Files.size(filePath) } catch (_: Exception) { 0L } else 0L

         if (!exists || size == 0L) {
            // seed _people with some data (if not used in tests)
            _people.addAll(_seed.people)
            logVerbose(TAG, "create(): seedData ${_people.size} people")
            writeInternal()
         }
         // read people from JSON file
         readInternal()
      }
   }

   override fun selectAll(): List<Person> =
      synchronized(_lock) { _people.toList() }

   // sort case-insensitive by selector
   override fun selectAllSortedBy(selector: (Person) -> String?): List<Person> =
      synchronized(_lock) {
         _people.sortedBy { person -> selector(person)?.lowercase() }
            .toList()
      }

   override fun selectWhere(predicate: (Person) -> Boolean): List<Person> =
      synchronized(_lock) {
         _people.filter(predicate)
            .toList()
      }

   override fun findById(id: String): Person? =
      synchronized(_lock) {
         _people.firstOrNull { it: Person -> it.id == id }
      }

   override fun findBy(predicate: (Person) -> Boolean): Person? =
      synchronized(_lock) {
         _people.firstOrNull(predicate)
      }

   override fun insert(person: Person) {
      synchronized(_lock) {
         if (_people.any { it.id == person.id }) return
         // throw IllegalArgumentException("Person with id ${person.id} already exists")
         _people.add(person)
         writeInternal()
      }
      logVerbose(TAG, "insert: $person")
   }

   override fun update(person: Person) {
      synchronized(_lock) {
         val existing = _people.firstOrNull { it.id == person.id }
         if (existing == null)
            throw IllegalArgumentException("Person with id ${person.id} does not exist")
         _people.remove(existing)
         _people.add(person)
         writeInternal()
      }
      logVerbose(TAG, "update: $person")
   }

   override fun delete(person: Person) {
      synchronized(_lock) {
         if (_people.none { it.id == person.id })
            throw IllegalArgumentException("Person with id ${person.id} does not exist")
         _people.remove(person)
         writeInternal()
      }
      logVerbose(TAG, "delete: $person")
   }

   // ---------- Internal I/O (always called under lock) ----------
   private fun readInternal() {
      try {
         // read json from a file and convert to a list of people
         val jsonString = try {
            // Files.readString(filePath) // Android doesn't fully support Java 11 NIO APIs.
            File(filePath.toString()).readText()
         } catch (e: IOException) {
            logError(TAG, "Failed to read file: ${e.message}")
            throw e
         }
         if (jsonString.isBlank()) {
            _people.clear()
            logDebug(TAG, "read(): empty file → 0 people")
            return
         }
         logVerbose(TAG, jsonString)

         _people = _jsonSettings.decodeFromString(jsonString)
         logDebug(TAG, "read(): decode JSON ${_people.size} Ppeople")
      }
      catch (e: Exception) {
         logError(TAG, "Failed to read: ${e.message}")
         throw e
      }
   }

   // write the list of people to the dataStore as JSON file
   private fun writeInternal() {
      try {
         val jsonString = _jsonSettings.encodeToString(_people)
         logDebug(TAG, "write(): encode JSON ${_people.size} people")

//         // Ensure directory exists (idempotent)
//         Files.createDirectories(filePath.parent)
//         // Atomic write: create temp file, then move it (REPLACE_EXISTING + ATOMIC_MOVE if supported)
//         val tmp = filePath.resolveSibling("${filePath.fileName}.tmp")
//         // Files.writeString(tmp, jsonString) // Android doesn't fully support Java 11 NIO APIs.
//         try {
//            Files.move(tmp, filePath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
//         } catch (_: Exception) {
//            // Fallback if ATOMIC_MOVE is not supported on this filesystem
//            Files.move(tmp, filePath, StandardCopyOption.REPLACE_EXISTING)
//         }

         // Ensure directory exists
         val dir = File(filePath.parent.toString())
         if (!dir.exists()) {
            dir.mkdirs()
         }
         // Atomic write: create temp file, then rename it
         val targetFile = File(filePath.toString())
         val tmpFile = File(targetFile.parent, "${targetFile.name}.tmp")
         tmpFile.writeText(jsonString)
         // Atomic rename (replaces existing file)
         if (!tmpFile.renameTo(targetFile)) {
            // Fallback if rename fails
            tmpFile.copyTo(targetFile, overwrite = true)
            tmpFile.delete()
         }


         // File(filePath.toString()).writeText(jsonString)
         logVerbose(TAG, jsonString)
      }
      catch (e: Exception) {
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
      ): Path {
         try {
            val dir: Path = Paths.get(appHome)
               .resolve("Documents") // capitalized, platform-friendly
               .resolve(directoryName)

            Files.createDirectories(dir)
            return dir.resolve(fileName)
         }
         catch (e: Exception) {
            logError(TAG, "Failed to create directory or build path: ${e.message}")
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