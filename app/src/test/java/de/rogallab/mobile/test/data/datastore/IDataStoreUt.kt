package de.rogallab.mobile.test.data.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import de.rogallab.mobile.Globals
import de.rogallab.mobile.data.IDataStore
import de.rogallab.mobile.data.local.Seed
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.test.TestApplication
import de.rogallab.mobile.test.di.defModulesTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

// problems with java version 17 and android sdk 36
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestApplication::class) // <- nutzt deine TestApp
class IDataStoreUt: KoinTest {

   @get:Rule
   val koinRule = KoinTestRule.create {
      androidContext(ApplicationProvider.getApplicationContext<Context>())
      modules(defModulesTest)   // deine Test-Module
   }

   // --- DI ---
   private val _dataStore: IDataStore by inject()
   private val _seed: Seed by inject()

   // --- Test data ---
   private lateinit var _seedPeople: List<Person>

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
   fun selectAll_ok() {
      // arrange
      val expected = _seedPeople
      // act
      val actual = _dataStore.selectAll()
      // assert
      assertContentEquals(expected, actual)
   }

   @Test
   fun selectAllSortBy_ok() {
      // arrange
      val expected = _seedPeople.sortedBy { it.firstName }
      // act
      val actual = _dataStore.selectAllSortedBy { it.firstName }
      // assert
      assertContentEquals(expected, actual)
   }

   @Test
   fun selectWhere_ok() {
      // arrange
      val expected = _seedPeople.filter{
         it.lastName?.contains("mann",true) ?: false }
      // act
      val actual = _dataStore.selectWhere {
         it.lastName?.contains("mann",true) ?: false }
      // assert
      assertContentEquals(expected, actual)
   }

   @Test
   fun findById_ok() {
      // arrange
      val id = "01000000-0000-0000-0000-000000000000"
      val expected = _seedPeople.first { it.id == id  }
      // act
      val actual = _dataStore.findById(id)
      // assert
      assertEquals(expected, actual)
   }

   @Test
   fun findBy_ok() {
      // arrange
      val firstName = "Arne"
      val expected = _seedPeople.first { it.firstName == firstName }
      // act
      val actual = _dataStore.findBy { it.firstName == firstName }
      // assert
      assertEquals(expected, actual)
   }


   @Test
   fun insert_ok() {
      // arrange
      val person = Person("Bernd", "Rogalla", "b-u.rogalla@ostfalia.de",
         id = "00000001-0000-0000-0000-000000000000")
      // act
      _dataStore.insert(person)
      // assert
      assertEquals(person, _dataStore.findById(person.id))
   }

   @Test
   fun update_ok() {
      // arrange
      val id = "01000000-0000-0000-0000-000000000000"
      val person = requireNotNull(_dataStore.findById(id))
      // act
      val updated = person.copy(lastName ="Albers", email = "a.albers@gmx.de")
      _dataStore.update(updated)
      // assert
      assertEquals(updated, _dataStore.findById(person.id))
   }

   @Test
   fun delete_ok() {
      // arrange
      val id = "01000000-0000-0000-0000-000000000000"
      val person = requireNotNull(_dataStore.findById(id))
      // act
      _dataStore.delete(person)
      // assert
      assertNull(_dataStore.findById(person.id))
   }
}