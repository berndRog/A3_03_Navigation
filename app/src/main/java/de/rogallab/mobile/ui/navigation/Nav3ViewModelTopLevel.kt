package de.rogallab.mobile.ui.navigation // Assuming package from other files
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey // Assuming PeopleList is a NavKey
import de.rogallab.mobile.domain.utilities.logDebug
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/**
 * Nav3ViewModelToplevel
 *
 * Teaching version:
 * - One independent back stack per top-level destination (e.g. bottom nav tab).
 * - Each tab remembers its own navigation history.
 * - Compose observes state directly (no Flow layer here).
 *
 * Key concepts:
 * - _topLevelBackStacks: Map of tab root -> its navigation stack (list of NavKey).
 * - currentTopLevelKey: which tab is active.
 * - currentStack: active tab's stack (mutable list, observed).
 * - backStack: effective list for UI (can merge start tab + current tab).
 */
class Nav3ViewModelToplevel2(
   private val defaultStartKey: NavKey = PeopleList
) : ViewModel(), INavHandler {
   private val tag = "<-Nav3ViewModelTpl"

   // Top-level key -> its own navigation stack (always starts with its root key).
   private val _topLevelBackStacks: MutableMap<NavKey, SnapshotStateList<NavKey>> =
      mutableMapOf(defaultStartKey to mutableStateListOf(defaultStartKey))

   // Currently selected top-level destination.
   var currentTopLevelKey: NavKey by mutableStateOf(defaultStartKey)
      private set

   // Public: raw mutable stack of the active top-level destination.
   val currentStack: SnapshotStateList<NavKey>
      get() = stackFor(currentTopLevelKey)

   // Public: effective stack exposed to UI.
   // When not on the start tab, we prepend the start tab's chain (excluding duplicate roots).
   val backStack: List<NavKey>
      get() {
         val active = currentStack
         return if (currentTopLevelKey == defaultStartKey) {
            active.toList()
         } else {
            val start = stackFor(defaultStartKey)
            start.toList() + active.drop(1)
         }
      }

   // Ensure a stack exists for a top-level key (lazy init).
   private fun stackFor(key: NavKey): SnapshotStateList<NavKey> =
      _topLevelBackStacks.getOrPut(key) { mutableStateListOf(key) }

   /**
    * Switch to another top-level destination.
    * Creates its stack on first visit.
    */
   fun switchTopLevel(key: NavKey) {
      if (key == currentTopLevelKey) {
         logDebug(tag, "switchTopLevel: already on $key")
         return
      }
      stackFor(key) // ensure stack
      currentTopLevelKey = key
      logDebug(tag, "switchTopLevel -> $key")
      debugDump()
   }

   /**
    * Push a destination onto the active stack.
    */
   override fun push(destination: NavKey) {
      currentStack.add(destination)
      logDebug(tag, "push: $destination (on $currentTopLevelKey)")
      debugDump()
   }

   /**
    * Pop one entry. If at root of a non-start tab, go back to start tab.
    */
   override fun pop() {
      when {
         currentStack.size > 1 -> {
            val removed = currentStack.removeAt(currentStack.lastIndex)
            logDebug(tag, "pop: removed $removed")
         }
         currentTopLevelKey != defaultStartKey -> {
            currentTopLevelKey = defaultStartKey
            logDebug(tag, "pop: returned to start key $defaultStartKey")
         }
         else -> logDebug(tag, "pop: at root of start stack")
      }
      debugDump()
   }

   /**
    * Clear current stack and navigate to given rootDestination.
    */
   override fun popToRootAndNavigate(rootDestination: NavKey) {
      replaceStack(rootDestination)
      logDebug(tag, "popToRootAndNavigate: $rootDestination")
      debugDump()
   }

   /**
    * Replace entire active stack with given keys (or reset to its root).
    */
   private fun replaceStack(vararg keys: NavKey) {
      val stack = currentStack
      stack.clear()
      if (keys.isNotEmpty()) stack.addAll(keys) else stack.add(currentTopLevelKey)
      logDebug(tag, "replaceStack: ${stack.joinToString()}")
   }

   /**
    * Debug dump: all stacks + effective stack.
    */
   private fun debugDump() {
      _topLevelBackStacks.forEach { (k, s) ->
         logDebug(tag, "stack[$k]=${s.joinToString(prefix = "[", postfix = "]")}")
      }
      logDebug(tag, "effectiveBackStack=${backStack.joinToString(prefix = "[", postfix = "]")}")
   }
}

/**
 * ViewModel for managing navigation state with multiple top-level entries,
 * suitable for scenarios like bottom navigation.
 * Each top-level entry maintains its own independent back stack.
 */
class Nav3ViewModelToplevel(
   private val defaultStartKey: NavKey = PeopleList // The primary "home" or start tab
) : ViewModel(), INavHandler {

   private val _tag: String = "<-Nav3ViewModelTpl"

   // Each top-level entry (identified by NavKey) has its own backstack list.
   // Initialized with the defaultStartKey and its own stack.
   private val _topLevelBackStacks: MutableMap<NavKey, SnapshotStateList<NavKey>> =
      mutableMapOf<NavKey, SnapshotStateList<NavKey>>(
         defaultStartKey to mutableStateListOf(defaultStartKey)
      )

   // The current active top-level key. Determines which back stack is being manipulated.
   var currentTopLevelKey: NavKey by mutableStateOf(defaultStartKey)
      private set // Can only be changed via switchTopLevel

   // The exposed backStack StateFlow represents the "effective" backstack for NavDisplay.
   // When on a non-default tab, it might show the defaultStartKey's stack followed by the current tab's stack.
   // This behavior should align with how NavDisplay consumes it.
   val backStack: StateFlow<List<NavKey>> = snapshotFlow {

      val currentActiveStackList = _topLevelBackStacks[currentTopLevelKey]?.toList()
         ?: listOf(currentTopLevelKey) // Fallback if stack is somehow missing

      // If the current top level is not the default start key, and you want to show
      // the defaultStartKey's stack as a base, then combine them.
      // Otherwise, just show the currentActiveStackList.
      // The current logic prefixes with startKey's stack. This is a specific UX choice.
      if (currentTopLevelKey == defaultStartKey) {
         currentActiveStackList
      } else {
         val startStackList = _topLevelBackStacks[defaultStartKey]?.toList()
            ?: listOf(defaultStartKey) // Fallback for start stack
         startStackList + currentActiveStackList.drop(1) // Avoid duplicating currentTopLevelKey if it's the root of its own stack
      }
   }
      .onEach { newBackStack ->
         logDebug(_tag, "Effective Backstack updated (size: ${newBackStack.size}):")
         newBackStack.forEachIndexed { index, key ->
            logDebug(_tag, "  [$index] $key")
         }
         logDebug(_tag, "Current TopLevelKey: $currentTopLevelKey")
         _topLevelBackStacks.forEach { (navKey, stack) ->
            logDebug(_tag, "  Stack for $navKey (size ${stack.size}): ${stack.joinToString()}")
         }
      }
      .stateIn(
         scope = viewModelScope,
         started = SharingStarted.Lazily,
         initialValue = _topLevelBackStacks[defaultStartKey]?.toList() ?: listOf(defaultStartKey)
      )

   /**
   // Switches the active top-level destination.
    * If the [key] has not been visited before, its backstack is initialized with the [key] itself.
    */
   fun switchTopLevel(key: NavKey) {
      if (key == currentTopLevelKey) {
         logDebug(_tag, "switchTopLevel: already on $key. No change.")
         return
      }

      _topLevelBackStacks.putIfAbsent(key, mutableStateListOf(key))
      currentTopLevelKey = key
      logDebug(_tag, "switchTopLevel: -> $key")
   }

   // New public navigation API (operates on the currentTopLevelKey's stack)

   /**
    * Pushes a new destination [NavKey] onto the backstack of the [currentTopLevelKey].
    * This is the primary method for navigating to a new screen within the current top-level stack.
    */
   override fun push(destination: NavKey) {
      logDebug(_tag, "pop: $destination to $currentTopLevelKey stack")
      val currentStack = _topLevelBackStacks[currentTopLevelKey]
      if (currentStack != null) {
         currentStack.add(destination)
      } else {
         logDebug(_tag, "pop: Error! No stack found for currentTopLevelKey: $currentTopLevelKey to add $destination.")
         // Handle error: maybe initialize stack or log severe warning
      }
   }

   /**
    * Pops the current top destination from the backstack of the [currentTopLevelKey].
    * If the stack becomes empty (only contains its root), and it's not the [defaultStartKey]'s stack,
    * it switches the [currentTopLevelKey] back to the [defaultStartKey].
    */
   override fun pop() {
      logDebug(_tag, "pop: from $currentTopLevelKey stack")
      val currentStack = _topLevelBackStacks[currentTopLevelKey]

      if (currentStack == null) {
         logDebug(_tag, "pop: Error! No stack found for $currentTopLevelKey. Resetting to $defaultStartKey.")
         // This is a recovery mechanism. Ideally, currentTopLevelKey always has a stack.
         currentTopLevelKey = defaultStartKey
         _topLevelBackStacks.putIfAbsent(defaultStartKey, mutableStateListOf(defaultStartKey))
         return
      }

      if (currentStack.size > 1) {
         val removedKey = currentStack.removeLastOrNull()
         logDebug(_tag, "pop: Removed $removedKey from $currentTopLevelKey stack.")
      } else { // Current stack has only 1 item (the topLevelKey itself)
         logDebug(_tag, "pop: Stack for $currentTopLevelKey has only its root.")
         if (currentTopLevelKey != defaultStartKey) {
            logDebug(_tag, "pop: Switching to defaultStartKey ($defaultStartKey) as current stack for $currentTopLevelKey is now at its root and it's not the default start key.")
            currentTopLevelKey = defaultStartKey
         } else {
            logDebug(_tag, "pop: On defaultStartKey ($defaultStartKey) and at its root. No further action.")
         }
      }
   }
   /**
    * Replaces the current top-level backstack with the [rootDestination] as its only entry.
    */
   override fun popToRootAndNavigate(rootDestination: NavKey) {
      logDebug(_tag, "popToRootAndNavigate to: $rootDestination (on $currentTopLevelKey stack)")
      replaceStack(rootDestination)
   }

   /**
    * Replaces the backstack of the [currentTopLevelKey] with the provided [keys].
    * If [keys] is empty, the stack for the [currentTopLevelKey] is reset to itself as the root.
    */
   private fun replaceStack(vararg keys: NavKey) {
      val currentStack = _topLevelBackStacks[currentTopLevelKey]
      if (currentStack == null) {
         logDebug(_tag, "replaceStack: Error! No stack found for currentTopLevelKey: $currentTopLevelKey")
         // Potentially re-initialize or throw error, depending on desired robustness
         _topLevelBackStacks[currentTopLevelKey] = mutableStateListOf(currentTopLevelKey)
         // return // or proceed with the newly created stack
      }

      logDebug(_tag, "replaceStack for $currentTopLevelKey with ${keys.size} keys: ${keys.joinToString()}")
      currentStack?.clear()
      if (keys.isNotEmpty()) {
         currentStack?.addAll(keys)
      } else {
         // Ensure current top-level stack is not empty after replace; reset to its root.
         currentStack?.add(currentTopLevelKey)
         logDebug(_tag, "replaceStack: Provided keys were empty, stack for $currentTopLevelKey reset to itself.")
      }
   }

}

