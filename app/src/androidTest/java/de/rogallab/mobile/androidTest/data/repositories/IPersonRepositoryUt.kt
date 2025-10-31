package de.rogallab.mobile.androidTest.data.repositories

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.rogallab.mobile.Globals
import de.rogallab.mobile.androidTest.di.defModulesAndroidTest
import de.rogallab.mobile.data.IDataStore
import de.rogallab.mobile.data.local.Seed
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail

@RunWith(AndroidJUnit4::class)
class IPersonRepositoryUt : KoinTest {

   @get:Rule
   val koinRule = KoinTestRule.create {
      androidContext(InstrumentationRegistry.getInstrumentation().targetContext)
      modules(defModulesAndroidTest)
   }

   // --- DI ---
   private val _context: Context by inject()
   private val _dataStore: IDataStore by inject()
   private val _repository: IPersonRepository by inject()
   private val _seed: Seed by inject()

   // --- Test data ---
   private lateinit var  _seedPeople: List<Person>

   // --- For cleanup ---
   private var _filePathNio: Path? = null

   @Before
   fun setup() {
      // no logging during testing
      Globals.isInfo = false
      Globals.isDebug = false
      Globals.isVerbose = false
      Globals.isComp = false

      // create seed after Koin has started
      _seedPeople = _seed.people.toList()

      // capture file path
      _filePathNio = _dataStore.filePath

      // Prepare the test store
      _dataStore.initialize()
   }

   @After
   fun tearDown() {
      // Delete the file created during the test (whichever type you use)
      _filePathNio?.let {
         try { Files.deleteIfExists(it) }
         catch (_: Exception) { /* ignore */ }
      }
   }

   @Test
   fun getAllUt_ok() {
      val expected = _seedPeople
      _repository.getAll()
         .onSuccess { actual -> assertContentEquals(expected, actual) }
         .onFailure { fail(it.message) }
   }

   @Test
   fun getAllSortByUt_ok() {
      // arrange
      val expected = _seedPeople.sortedBy { it.firstName }
      // act / assert
      _repository.getAllSortedBy { it.firstName }
         .onSuccess { actual -> assertContentEquals(expected, actual) }
         .onFailure { fail(it.message) }
   }

   @Test
   fun getWhereUt_ok() {
      // arrange
      val expected = _seedPeople.filter { it.lastName.contains("mann", true) }
      // act / assert  --> Hoffmann
      _repository.getWhere { it.lastName.contains("mann", true) }
         .onSuccess { actual -> assertContentEquals(expected, actual) }
         .onFailure { fail(it.message) }
   }

   @Test
   fun findByIdUt_ok() {
      // arrange
      val id = "01000000-0000-0000-0000-000000000000"
      val expected = _seedPeople.firstOrNull { person -> person.id == id }
      // act / assert
      val result = _repository.findById(id)
      assert(result.isSuccess)
      assertEquals(expected, result.getOrThrow())
   }

   @Test
   fun findByUt_ok() {
      // arrange
      val expected = _seedPeople.firstOrNull { person ->
         person.lastName.contains("mann", true) ?: false
      }
      // act / assert
      val result = _repository.findBy { it.lastName.contains("mann", true) ?: false }
      assert(result.isSuccess)
      assertEquals(expected, result.getOrThrow())
   }

   @Test
   fun insertUt_ok() {
      // arrange
      val person = Person(
         "Bernd", "Rogalla", id = "00000001-0000-0000-0000-000000000000")
      // act
      val createResult = _repository.create(person)
      assert(createResult.isSuccess)
      // assert
      val result = _repository.findById(person.id)
      assert(result.isSuccess)
      assertEquals(person, result.getOrThrow())
   }

   @Test
   fun updateUt_ok() {
      // arrange
      val id = "01000000-0000-0000-0000-000000000000"
      var person = requireNotNull(_repository.findById(id).getOrThrow())
      // act
      val updated = person.copy(lastName = "Albers")
      val updateResult = _repository.update(updated)
      assert(updateResult.isSuccess)
      // assert
      val result = _repository.findById(id)
      assert(result.isSuccess)
      assertEquals(updated, result.getOrThrow())
   }

   @Test
   fun deleteUt_ok() {
      // arrange
      val id = "01000000-0000-0000-0000-000000000000"
      val person = requireNotNull(_repository.findById(id).getOrThrow())
      // act
      val result = _repository.remove(person)
      assert(result.isSuccess)
      // assert
      assertNull(_repository.findById(person.id).getOrThrow())
   }
}
