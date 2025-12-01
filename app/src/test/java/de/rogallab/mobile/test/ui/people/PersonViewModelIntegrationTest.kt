package de.rogallab.mobile.test.ui.people

import android.content.Context
import app.cash.turbine.test
import de.rogallab.mobile.data.IDataStore
import de.rogallab.mobile.data.local.Seed
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.ui.navigation.INavHandler
import de.rogallab.mobile.ui.navigation.PeopleList
import de.rogallab.mobile.ui.people.PeopleIntent
import de.rogallab.mobile.ui.people.PersonIntent
import de.rogallab.mobile.ui.people.PersonViewModel
import de.rogallab.mobile.test.MainDispatcherRule
import de.rogallab.mobile.test.TestApplication
import de.rogallab.mobile.test.di.defModulesTest
import de.rogallab.mobile.test.domain.utilities.setupConsoleLogger
import kotlinx.coroutines.test.advanceUntilIdle
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Robolectric integration test for PersonViewModel using Turbine for StateFlow assertions.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = TestApplication::class)
class PersonViewModelIntegrationTest : KoinTest {

   // Replaces Dispatchers.Main with a TestDispatcher for deterministic coroutine execution
   @get:Rule
   val main = MainDispatcherRule()

   // Starts Koin with test modules
   @get:Rule
   val koinRule = KoinTestRule.create {
      modules(defModulesTest)
   }

   // --- DI ---
   private val _context: Context by inject()
   private val _dataStore: IDataStore by inject()
   private val _navHandler: INavHandler by inject { parametersOf(PeopleList) }
   private val _viewModel: PersonViewModel by inject { parametersOf(_navHandler) }
   private val _seed: Seed by inject()

   private lateinit var _seedPeople: List<Person>
   private lateinit var _filePath: Path

   @Before
   fun setUp() {
      // Configure logging (can be turned off if too noisy)
      de.rogallab.mobile.Globals.isInfo = true
      de.rogallab.mobile.Globals.isDebug = true
      de.rogallab.mobile.Globals.isVerbose = false
      de.rogallab.mobile.Globals.isComp = false
      setupConsoleLogger()

      // Seed data created after Koin has started
      _seedPeople = _seed.people.toList()

      _filePath = _dataStore.filePath
      _dataStore.initialize()
   }

   @After
   fun tearDown() {
      try {
         Files.deleteIfExists(_filePath)
      } catch (_: Throwable) {
      }
   }

   // ------------------------------------------------------------------------
   // Fetch: emits loading state first, then final state with people list
   // ------------------------------------------------------------------------
   @Test
   fun fetch_emits_loading_then_final_list() = runTest {
      val expected = _seedPeople

      _viewModel.peopleUiStateFlow.test {

         // (1) INITIAL STATE
         // StateFlow always emits its current value immediately on collection.
         // Expected:
         //   isLoading = false
         //   people    = empty or last known snapshot
         val initial = awaitItem()
         assertFalse(initial.isLoading, "Initial state should not be loading")

         // Trigger fetch via MVI intent
         _viewModel.handlePeopleIntent(PeopleIntent.Fetch)

         // (2) LOADING STATE (from onStart in fetch pipeline)
         // Emitted by:
         //   .onStart { updateState { copy(isLoading = true) } }
         val loading = awaitItem()
         assertTrue(loading.isLoading, "Fetch should emit a loading state first")

         // (3) FINAL STATE (from collectLatest → onSuccess)
         // Emitted by:
         //   collectLatest { result.onSuccess { updateState { copy(isLoading = false, people = snapshot) } } }
         val final = awaitItem()
         assertFalse(final.isLoading, "Final state should not be loading")
         assertEquals(expected, final.people, "People list should match seeded data")

         cancelAndIgnoreRemainingEvents()
      }
   }

   // ------------------------------------------------------------------------
   // FetchById: loads a single person into PersonUiState
   // ------------------------------------------------------------------------

   @Test
   fun fetchById_loads_person() = runTest {
      // Precondition: list must be loaded first
      _viewModel.handlePeopleIntent(PeopleIntent.Fetch)
      advanceUntilIdle()

      val id = _viewModel.peopleUiStateFlow.value.people.first().id

      _viewModel.personUiStateFlow.test {

         // (1) INITIAL PERSON STATE (usually an empty template Person)
         val initial = awaitItem()

         // Trigger FetchById via MVI
         _viewModel.handlePersonIntent(PersonIntent.FetchById(id))

         // (2) UPDATED PERSON STATE
         // fetchById updates only the PersonUiState, no loading flag here.
         val loaded = awaitItem()
         assertEquals(id, loaded.person.id, "PersonUiState should contain the requested person")

         cancelAndIgnoreRemainingEvents()
      }
   }

   // ------------------------------------------------------------------------
   // Create: increases list size and contains the new person
   // ------------------------------------------------------------------------

   @Test
   fun create_increases_list() = runTest {
      // Precondition: load initial list so we know the starting size
      _viewModel.handlePeopleIntent(PeopleIntent.Fetch)
      advanceUntilIdle()

      val sizeBefore = _viewModel.peopleUiStateFlow.value.people.size
      val firstName = "Bernd"
      val lastName = "Rogalla"

      _viewModel.peopleUiStateFlow.test {

         // (1) BASELINE STATE BEFORE CREATE
         val baseline = awaitItem()
         assertEquals(sizeBefore, baseline.people.size, "Baseline size should match pre-fetched size")

         // Prepare PersonUiState for new input
         _viewModel.handlePersonIntent(PersonIntent.Clear)
         _viewModel.handlePersonIntent(PersonIntent.FirstNameChange(firstName))
         _viewModel.handlePersonIntent(PersonIntent.LastNameChange(lastName))

         // Trigger Create → internally calls fetch()
         _viewModel.handlePersonIntent(PersonIntent.Create)

         // (2) LOADING STATE from fetch().onStart
         val loading = awaitItem()
         assertTrue(loading.isLoading, "Create → fetch should emit a loading state")

         // (3) FINAL STATE from fetch().collectLatest
         val final = awaitItem()
         assertFalse(final.isLoading, "Final state after create should not be loading")

         val sizeIncreasedByOne = final.people.size == sizeBefore + 1
         val found = final.people.any { it.firstName == firstName && it.lastName == lastName }

         assertTrue(sizeIncreasedByOne, "People list size should be increased by one after create")
         assertTrue(found, "Newly created person should be present in the list")

         cancelAndIgnoreRemainingEvents()
      }
   }

   // ------------------------------------------------------------------------
   // Update: list contains the person with updated last name
   // ------------------------------------------------------------------------

   @Test
   fun update_changes_list() = runTest {
      // Precondition: load initial list
      _viewModel.handlePeopleIntent(PeopleIntent.Fetch)
      advanceUntilIdle()

      val id = _viewModel.peopleUiStateFlow.value.people.first().id
      val newLastName = "Albers"

      _viewModel.peopleUiStateFlow.test {

         // (1) BASELINE STATE BEFORE UPDATE
         val baseline = awaitItem()

         // Load person into PersonUiState
         _viewModel.handlePersonIntent(PersonIntent.FetchById(id))
         advanceUntilIdle()

         // Trigger update (this calls fetch() internally)
         _viewModel.handlePersonIntent(PersonIntent.LastNameChange(newLastName))
         _viewModel.handlePersonIntent(PersonIntent.Update)

         // (2) LOADING STATE from fetch().onStart
         val loading = awaitItem()
         assertTrue(loading.isLoading, "Update → fetch should emit a loading state")

         // (3) FINAL STATE from fetch().collectLatest
         val final = awaitItem()
         assertFalse(final.isLoading, "Final state after update should not be loading")

         val updated = final.people.first { it.id == id }
         assertEquals(newLastName, updated.lastName, "Updated person should have the new last name")

         cancelAndIgnoreRemainingEvents()
      }
   }

   // ------------------------------------------------------------------------
   // Update: list size remains same and PersonUiState is consistent with PeopleUiState
   // ------------------------------------------------------------------------

   @Test
   fun update_keeps_list_size_and_updates_person_in_personUiState_and_peopleUiState() = runTest {
      // Precondition: initial fetch to get list and size
      _viewModel.handlePeopleIntent(PeopleIntent.Fetch)
      advanceUntilIdle()

      val initialPeopleState = _viewModel.peopleUiStateFlow.value
      val initialSize = initialPeopleState.people.size

      val original = initialPeopleState.people.first()
      val id = original.id
      val newLastName = "UpdatedLastName"

      // Load person into PersonUiState
      _viewModel.handlePersonIntent(PersonIntent.FetchById(id))
      advanceUntilIdle()

      val personStateBeforeUpdate = _viewModel.personUiStateFlow.value
      assertEquals(id, personStateBeforeUpdate.person.id, "PersonUiState should hold the correct person before update")
      assertEquals(original.lastName, personStateBeforeUpdate.person.lastName)

      _viewModel.peopleUiStateFlow.test {

         // (1) BASELINE STATE, size should match initial
         val baseline = awaitItem()
         assertEquals(initialSize, baseline.people.size, "Baseline list size should match initial size")

         // Trigger update: change last name and call Update
         _viewModel.handlePersonIntent(PersonIntent.LastNameChange(newLastName))
         _viewModel.handlePersonIntent(PersonIntent.Update)

         // (2) LOADING STATE from fetch().onStart
         val loading = awaitItem()
         assertTrue(loading.isLoading, "Update → fetch should emit loading state")

         // (3) FINAL STATE from fetch().collectLatest
         val final = awaitItem()
         assertFalse(final.isLoading, "Final state should not be loading")
         assertEquals(initialSize, final.people.size, "List size should remain unchanged after update")

         // Person in people list must be updated
         val updatedFromList = final.people.first { it.id == id }
         assertEquals(original.firstName, updatedFromList.firstName, "First name should not change")
         assertEquals(newLastName, updatedFromList.lastName, "Last name should be updated")

         // PersonUiState must also be updated
         val personStateAfterUpdate = _viewModel.personUiStateFlow.value
         assertEquals(id, personStateAfterUpdate.person.id)
         assertEquals(newLastName, personStateAfterUpdate.person.lastName)

         cancelAndIgnoreRemainingEvents()
      }
   }

   // ------------------------------------------------------------------------
   // Remove (without Undo): list size is reduced and victim is gone
   // ------------------------------------------------------------------------

   @Test
   fun remove_reduces_list() = runTest {
      // Precondition: fetch initial list
      _viewModel.handlePeopleIntent(PeopleIntent.Fetch)
      advanceUntilIdle()

      val initialState = _viewModel.peopleUiStateFlow.value
      val victim = initialState.people.first()
      val victimId = victim.id
      val initialSize = initialState.people.size

      _viewModel.peopleUiStateFlow.test {

         // (1) BASELINE STATE
         val baseline = awaitItem()
         assertEquals(initialSize, baseline.people.size)

         // Trigger remove() → internally calls fetch()
         _viewModel.handlePersonIntent(PersonIntent.Remove(victim))

         // (2) LOADING STATE from fetch().onStart
         val loading = awaitItem()
         assertTrue(loading.isLoading, "Remove → fetch should emit loading state")

         // (3) FINAL STATE from fetch().collectLatest
         val final = awaitItem()
         assertFalse(final.isLoading, "Final state should not be loading")
         assertEquals(initialSize - 1, final.people.size, "List size should be reduced by one after remove")
         val isDeleted = final.people.none { it.id == victimId }
         assertTrue(isDeleted, "Removed person should no longer be present in the list")

         cancelAndIgnoreRemainingEvents()
      }
   }

   // ------------------------------------------------------------------------
   // RemoveUndo: optimistic UI remove only (no fetch(), no loading)
   // ------------------------------------------------------------------------

   @Test
   fun removeUndo_temporarily_removes_person_from_list() = runTest {
      // Precondition: fetch initial list
      _viewModel.handlePeopleIntent(PeopleIntent.Fetch)
      advanceUntilIdle()

      val initialState = _viewModel.peopleUiStateFlow.value
      val initialSize = initialState.people.size
      val victim = initialState.people.first()
      val victimId = victim.id

      _viewModel.peopleUiStateFlow.test {

         // (1) BASELINE STATE AFTER FETCH
         val baseline = awaitItem()
         assertEquals(initialSize, baseline.people.size)

         // Trigger optimistic remove with undo buffer
         _viewModel.handlePersonIntent(PersonIntent.RemoveUndo(victim))

         // (2) REMOVEUNDO STATE
         // removeUndo() updates UI only, no loading state, no fetch().
         val afterRemove = awaitItem()
         assertEquals(initialSize - 1, afterRemove.people.size, "List size should be reduced by one")
         assertTrue(afterRemove.people.none { it.id == victimId }, "Victim should be removed from the UI list")
         assertEquals(null, afterRemove.restoredPersonId, "restoredPersonId should not be set yet")

         cancelAndIgnoreRemainingEvents()
      }
   }

   // ------------------------------------------------------------------------
   // Undo: restores removed person and sets restoredPersonId
   // ------------------------------------------------------------------------

   @Test
   fun undoRestores_removed_person_and_sets_restoredPersonId() = runTest {
      // Precondition: fetch + RemoveUndo
      _viewModel.handlePeopleIntent(PeopleIntent.Fetch)
      advanceUntilIdle()

      val initialState = _viewModel.peopleUiStateFlow.value
      val initialSize = initialState.people.size
      val victim = initialState.people.first()
      val victimId = victim.id

      _viewModel.handlePersonIntent(PersonIntent.RemoveUndo(victim))
      advanceUntilIdle()

      _viewModel.peopleUiStateFlow.test {

         // (1) STATE AFTER REMOVEUNDO
         val afterRemove = awaitItem()
         assertEquals(initialSize - 1, afterRemove.people.size)
         assertTrue(afterRemove.people.none { it.id == victimId })

         // Trigger Undo → restores item from internal undo buffer
         _viewModel.handlePersonIntent(PersonIntent.Undo)

         // (2) UNDO STATE
         // Expected:
         //   list size = initialSize
         //   victim restored
         //   restoredPersonId = victimId
         val afterUndo = awaitItem()
         assertEquals(initialSize, afterUndo.people.size, "List size should be restored")
         assertTrue(afterUndo.people.any { it.id == victimId }, "Victim should be restored in the list")
         assertEquals(victimId, afterUndo.restoredPersonId, "restoredPersonId should point to restored person")

         cancelAndIgnoreRemainingEvents()
      }
   }

   // ------------------------------------------------------------------------
   // Restored: clears restoredPersonId while keeping list unchanged
   // ------------------------------------------------------------------------

   @Test
   fun restored_clears_restoredPersonId_but_keeps_list_unchanged() = runTest {
      // Precondition: fetch, RemoveUndo, Undo
      _viewModel.handlePeopleIntent(PeopleIntent.Fetch)
      advanceUntilIdle()

      val initialState = _viewModel.peopleUiStateFlow.value
      val initialSize = initialState.people.size
      val victim = initialState.people.first()
      val victimId = victim.id

      _viewModel.handlePersonIntent(PersonIntent.RemoveUndo(victim))
      _viewModel.handlePersonIntent(PersonIntent.Undo)
      advanceUntilIdle()

      _viewModel.peopleUiStateFlow.test {

         // (1) STATE AFTER UNDO (restoredPersonId is set)
         val afterUndo = awaitItem()
         assertEquals(victimId, afterUndo.restoredPersonId)
         assertEquals(initialSize, afterUndo.people.size)

         // Trigger Restored once UI has scrolled to the restored item
         _viewModel.handlePersonIntent(PersonIntent.Restored)

         // (2) RESTORED STATE
         // Expected:
         //   restoredPersonId = null
         //   people list unchanged
         val afterRestored = awaitItem()
         assertEquals(null, afterRestored.restoredPersonId, "restoredPersonId should be cleared")
         assertEquals(initialSize, afterRestored.people.size, "List size should remain unchanged")
         assertTrue(afterRestored.people.any { it.id == victimId }, "Restored person must still be in the list")

         cancelAndIgnoreRemainingEvents()
      }
   }

   // ------------------------------------------------------------------------
   // Full RemoveUndo → Undo → Restored sequence in a single flow
   // ------------------------------------------------------------------------

   @Test
   fun removeUndo_then_undo_restores_person_and_clears_restored_flag() = runTest {
      // Precondition: fetch initial list
      _viewModel.handlePeopleIntent(PeopleIntent.Fetch)
      advanceUntilIdle()

      val initialState = _viewModel.peopleUiStateFlow.value
      val initialList = initialState.people
      val initialSize = initialList.size
      val index = if (initialSize > 1) 1 else 0
      val victim = initialList[index]

      _viewModel.peopleUiStateFlow.test {

         // (1) BASELINE STATE AFTER FETCH
         val baseline = awaitItem()
         assertEquals(initialSize, baseline.people.size)

         // Step 1: RemoveUndo
         _viewModel.handlePersonIntent(PersonIntent.RemoveUndo(victim))

         // (2) STATE AFTER REMOVEUNDO (victim removed from list)
         val afterRemove = awaitItem()
         val afterRemoveList = afterRemove.people
         assertEquals(initialSize - 1, afterRemoveList.size, "List size should be reduced by one")
         assertTrue(afterRemoveList.none { it.id == victim.id }, "Victim should be removed")

         // Step 2: Undo
         _viewModel.handlePersonIntent(PersonIntent.Undo)

         // (3) STATE AFTER UNDO (victim restored and restoredPersonId set)
         val afterUndo = awaitItem()
         val afterUndoList = afterUndo.people
         assertEquals(initialSize, afterUndoList.size, "List size should be restored")
         assertEquals(victim.id, afterUndoList[index].id, "Victim should be restored at the same index")
         assertEquals(victim.id, afterUndo.restoredPersonId, "restoredPersonId should point to restored person")

         // Step 3: Restored (UI acknowledges scroll completion)
         _viewModel.handlePersonIntent(PersonIntent.Restored)

         // (4) FINAL STATE (restoredPersonId cleared, list unchanged)
         val final = awaitItem()
         val finalList = final.people
         assertEquals(initialSize, finalList.size, "List size should remain unchanged")
         assertEquals(victim.id, finalList[index].id, "Victim should still be present in the list")
         assertEquals(null, final.restoredPersonId, "restoredPersonId should be cleared")

         cancelAndIgnoreRemainingEvents()
      }
   }
}
