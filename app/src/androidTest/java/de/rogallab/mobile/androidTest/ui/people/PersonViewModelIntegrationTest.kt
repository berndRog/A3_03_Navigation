package de.rogallab.mobile.androidTest.ui.people

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.rogallab.mobile.Globals
import de.rogallab.mobile.androidTest.di.defModulesAndroidTest
import de.rogallab.mobile.data.IDataStore
import de.rogallab.mobile.data.local.Seed
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.ui.navigation.INavHandler
import de.rogallab.mobile.ui.navigation.PeopleList
import de.rogallab.mobile.ui.people.PeopleIntent
import de.rogallab.mobile.ui.people.PersonIntent
import de.rogallab.mobile.ui.people.PersonViewModel
import de.rogallab.mobile.ui.people.composables.PeopleListScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.parameter.parametersOf
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class PersonViewModelIntegrationTest : KoinTest {

   @get:Rule
   val koinRule = KoinTestRule.create {
      androidContext(InstrumentationRegistry.getInstrumentation().targetContext)
      modules(defModulesAndroidTest)
   }

   // DI
   private val _dataStore: IDataStore by inject()
   private val _navHandler: INavHandler by inject{ parametersOf(PeopleList ) }
   private val _viewModel: PersonViewModel by inject{ parametersOf(_navHandler) }
   private val _seed: Seed by inject()

   private lateinit var _seedPeople: List<Person>
   private lateinit var filePath: Path

   @Before
   fun setUp() {
      // no logging
      Globals.isInfo = false
      Globals.isDebug = false
      Globals.isVerbose = false
      Globals.isComp = false

      // create seed after Koin has started
      _seedPeople = _seed.people.toList()

      filePath = _dataStore.filePath
      _dataStore.initialize()
   }

   @After
   fun tearDown() {
      try {
         Files.deleteIfExists(filePath)
      }
      catch (_: Throwable) {
      }
   }

   @Test
   fun fetch_loads_list() = runTest {
      val expected = _seedPeople
      // act: MVI Intent
      _viewModel.handlePeopleIntent(PeopleIntent.Fetch)
      // assert: actual state
      val actualPeopleUiState = _viewModel.peopleUiStateFlow.value
      assertEquals(expected, actualPeopleUiState.people)
   }

   @Test
   fun fetchById_loads_person() = runTest {
      // arrange: fetch people and get id of first person
      _viewModel.handlePeopleIntent(PeopleIntent.Fetch)
      val expected = _viewModel.peopleUiStateFlow.value.people.first()
      val id = expected.id

      // act
      _viewModel.handlePersonIntent(PersonIntent.FetchById(id))

      // assert: actual state
      val actualPersonUiState = _viewModel.personUiStateFlow.value
      assertEquals(expected, actualPersonUiState.person)
   }

   @Test
   fun create_increases_list() = runTest {
      // arrange: fetch people
      _viewModel.handlePeopleIntent(PeopleIntent.Fetch)
      val initialPeopleUiState = _viewModel.peopleUiStateFlow.value
      val expectedFirstName = "Bernd"
      val expectedLastName = "Rogalla"

      // act: create new person via MVI
      _viewModel.handlePersonIntent(PersonIntent.Clear)
      _viewModel.handlePersonIntent(PersonIntent.FirstNameChange(expectedFirstName))
      _viewModel.handlePersonIntent(PersonIntent.LastNameChange(expectedLastName))
      _viewModel.handlePersonIntent(PersonIntent.Create)   // ruft intern fetch()

      // assert: people list is increased by one
      val actualPeopleUiState = _viewModel.peopleUiStateFlow.value
      val actual = actualPeopleUiState.people.first { it.firstName == "Bernd" && it.lastName == "Rogalla" }
      assertEquals(actualPeopleUiState.people.size, initialPeopleUiState.people.size + 1)
      assertEquals(expectedFirstName, actual.firstName)
      assertEquals(expectedLastName, actual.lastName)
   }

   @Test
   fun update_changes_list() = runTest {
      // arrange: fetch people and get id of first person
      _viewModel.handlePeopleIntent(PeopleIntent.Fetch)
      val expected = _viewModel.peopleUiStateFlow.value.people.first()
      val id = expected.id
      val updatedLastName = "Albers"

      // act: get person by id, change lastName, update (fetch() is called internal)
      _viewModel.handlePersonIntent(PersonIntent.FetchById(id))
      _viewModel.handlePersonIntent(PersonIntent.LastNameChange(updatedLastName))
      _viewModel.handlePersonIntent(PersonIntent.Update)

      // assert: people list contains the changed item
      val peopleUiState = _viewModel.peopleUiStateFlow.value
      val actual = peopleUiState.people.first { it.id == id && it.lastName == updatedLastName }
      assertEquals(expected.id, actual.id)
      assertEquals(expected.firstName, actual.firstName)
      assertEquals(updatedLastName, actual.lastName)
   }

   @Test
   fun remove_reduces_list() = runTest {
      // arrange: fetch people
      _viewModel.handlePeopleIntent(PeopleIntent.Fetch)
      val initialPeopleUiState = _viewModel.peopleUiStateFlow.value
      val victim = initialPeopleUiState.people.first()
      val id = victim.id

      // act: run remove ( fetch is called internal)
      _viewModel.handlePersonIntent(PersonIntent.Remove(victim))

      // assert
      val actualPeopleUiState = _viewModel.peopleUiStateFlow.value
      val actual = actualPeopleUiState.people.none { it.id == id }
      assertEquals(actualPeopleUiState.people.size, initialPeopleUiState.people.size - 1)
      assertTrue(actual)
   }
}