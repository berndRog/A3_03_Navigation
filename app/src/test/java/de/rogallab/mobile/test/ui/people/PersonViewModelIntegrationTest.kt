package de.rogallab.mobile.test.ui.people

import android.content.Context
import app.cash.turbine.test
import de.rogallab.mobile.data.IDataStore
import de.rogallab.mobile.data.local.Seed
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.ui.people.PersonViewModel
import de.rogallab.mobile.test.MainDispatcherRule
import de.rogallab.mobile.test.TestApplication
import de.rogallab.mobile.test.di.defModulesTest
import de.rogallab.mobile.ui.navigation.INavHandler
import de.rogallab.mobile.ui.navigation.PeopleList
import de.rogallab.mobile.ui.people.PeopleIntent
import de.rogallab.mobile.ui.people.PersonIntent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.parameter.parametersOf
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import org.robolectric.annotation.Config
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(org.robolectric.RobolectricTestRunner::class)
@Config(sdk = [35], application = TestApplication::class)
class PersonViewModelIntegrationTest : KoinTest {

   @get:Rule
   val main = MainDispatcherRule()

   @get:Rule
   val koinRule = KoinTestRule.create {
      modules(defModulesTest)
   }

   // DI
   private val _context: Context by inject()
   private val _dataStore: IDataStore by inject()
   private val _navHandler: INavHandler by inject{ parametersOf(PeopleList ) }
   private val _viewModel: PersonViewModel by inject{ parametersOf(_navHandler) }

   private val _seed: Seed by inject()

   private lateinit var _seedPeople: List<Person>
   private lateinit var _filePath: Path

   @Before
   fun setUp() {
      // no logging
      de.rogallab.mobile.Globals.isInfo = false
      de.rogallab.mobile.Globals.isDebug = false
      de.rogallab.mobile.Globals.isVerbose = false
      de.rogallab.mobile.Globals.isComp = false

      _seedPeople = _seed.people.toList()

      _filePath = _dataStore.filePath
      _dataStore.initialize()
   }

   @After
   fun tearDown() {
      try {
         Files.deleteIfExists(_filePath)
      }
      catch (_: Throwable) {
      }
   }

   @Test
   fun fetch_loads_list() = runTest {
      val expected = _seedPeople
      _viewModel.peopleUiStateFlow.test {
         // initial state
         val initialPeopleUiState = awaitItem()
         // act: MVI Intent
         _viewModel.handlePeopleIntent(PeopleIntent.Fetch)
         // assert: actual state
         val actualPeopleUiState = awaitItem()
         assertEquals(expected, actualPeopleUiState.people)

         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun fetchById_loads_person() = runTest {
      // fetch people and get id of first person
      _viewModel.handlePeopleIntent(PeopleIntent.Fetch)
      val id = _viewModel.peopleUiStateFlow.value.people.first().id

      _viewModel.personUiStateFlow.test {
         // initial
         val initialPersonUiState = awaitItem()
         // act
         _viewModel.handlePersonIntent(PersonIntent.FetchById(id))
         val actualPersonUiState= awaitItem()
         // assert
         assertEquals(id, actualPersonUiState.person.id)
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun create_increases_list() = runTest {
      // fetch people and evaluate size of people list
      _viewModel.handlePeopleIntent(PeopleIntent.Fetch)
      val sizeBefore = _viewModel.peopleUiStateFlow.value.people.size

      // act: create new person via MVI
      _viewModel.handlePersonIntent(PersonIntent.FirstNameChange("Bernd"))
      _viewModel.handlePersonIntent(PersonIntent.LastNameChange("Rogalla"))
      _viewModel.handlePersonIntent(PersonIntent.Create)   // ruft intern fetch()

      // assert: people list is increased by one
      _viewModel.peopleUiStateFlow.test {
         val peopleUiState = awaitItem()
         val sizeIncreasedByOne = peopleUiState.people.size == sizeBefore + 1
         val found = peopleUiState.people.any { it.firstName == "Bernd" && it.lastName == "Rogalla" }
         assertTrue(sizeIncreasedByOne)
         assertTrue(found)
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun update_changes_list() = runTest {
      // fetch people and get id of first person
      _viewModel.handlePeopleIntent(PeopleIntent.Fetch)
      val id = _viewModel.peopleUiStateFlow.value.people.first().id

      // act: get person by id, change lastName, update (call fetch() internal)
      _viewModel.handlePersonIntent(PersonIntent.FetchById(id))
      _viewModel.handlePersonIntent(PersonIntent.LastNameChange("Albers"))
      _viewModel.handlePersonIntent(PersonIntent.Update)

      // assert: people list contains the changed item
      _viewModel.peopleUiStateFlow.test {
         val peopleUiState = awaitItem()
         val foundUpdated = peopleUiState.people.any { it.id == id && it.lastName == "Albers" }
         assertTrue(foundUpdated)
         cancelAndIgnoreRemainingEvents()
      }
   }

   @Test
   fun remove_reduces_list() = runTest {
      // arrange: fetch people
      _viewModel.handlePeopleIntent(PeopleIntent.Fetch)
      val victim = _viewModel.peopleUiStateFlow.value.people.first()

      // act: run remove ( fetch is called internal)
      _viewModel.handlePersonIntent(PersonIntent.Remove(victim))

      // assert: nicht mehr vorhanden
      _viewModel.peopleUiStateFlow.test {
         val peopleUiState = awaitItem()
         val isDeleted = peopleUiState.people.none { it.id == victim.id }
         assertTrue(isDeleted)
         cancelAndIgnoreRemainingEvents()
      }
   }


}
