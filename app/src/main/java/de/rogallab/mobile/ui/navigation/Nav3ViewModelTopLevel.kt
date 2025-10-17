package de.rogallab.mobile.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.navigation3.runtime.NavKey
import de.rogallab.mobile.domain.utilities.logDebug

/**
 * Nav3ViewModelToplevel
 *
 * ViewModel for Bottom Navigation with independent back stacks per tab.
 *
 * Concepts:
 * - Each top-level tab has its own independent navigation stack
 * - Tabs retain their navigation history when switching
 * - Efficient state management via SnapshotStateList for direct Compose integration
 *
 * @param startDestination The start tab (default: PeopleList)
 */
class Nav3ViewModelToplevel(
   private val startDestination: NavKey = PeopleList
) : ViewModel(), INavHandler {

   private val tag = "<-Nav3ViewModelTpl"

   // Map: Top-Level NavKey -> associated navigation stack
   private val _topLevelBackStacks: MutableMap<NavKey, SnapshotStateList<NavKey>> =
      mutableMapOf(startDestination to mutableStateListOf(startDestination))

   // Currently active top-level tab
   var currentTopLevelKey: NavKey by mutableStateOf(startDestination)
      private set

   // The active stack observed by the UI
   val currentStack: SnapshotStateList<NavKey>
      get() = _topLevelBackStacks.getOrPut(currentTopLevelKey) {
         mutableStateListOf(currentTopLevelKey)
      }

   // Optional: Immutable list for external access
   val currentStackAsList: List<NavKey>
      get() = currentStack.toList()

   /**
    * Switches to another top-level tab.
    * Creates a new stack if necessary.
    *
    * @param key The target tab
    */
   fun switchTopLevel(key: NavKey) {
      if (key != currentTopLevelKey) {
         currentTopLevelKey = key
         logDebug(tag, "switchTopLevel -> $key")
         debugDump()
      } else {
         logDebug(tag, "switchTopLevel: already on $key")
      }
   }

   /**
    * Navigates to a new destination in the current tab stack.
    *
    * @param destination The target destination
    */
   override fun push(destination: NavKey) {
      currentStack.add(destination)
      logDebug(
         tag,
         "push: $destination -> Stack($currentTopLevelKey): ${currentStack.joinToString()}"
      )
      debugDump()
   }

   /**
    * Removes the topmost destination from the current stack.
    * If at root, nothing is removed (system back takes over).
    */
   override fun pop() {
      if (currentStack.size > 1) {
         val removed = currentStack.removeAt(currentStack.lastIndex)
         logDebug(
            tag,
            "pop: removed $removed -> Stack($currentTopLevelKey): ${currentStack.joinToString()}"
         )
         debugDump()
      } else {
         logDebug(tag, "pop: at root of $currentTopLevelKey")
      }
   }

   /**
    * Switches to a root tab and resets its stack.
    *
    * @param rootDestination The target tab
    */
   override fun popToRootAndNavigate(rootDestination: NavKey) {
      switchTopLevel(rootDestination)
      currentStack.clear()
      currentStack.add(rootDestination)
      logDebug(tag, "popToRootAndNavigate -> $rootDestination")
      debugDump()
   }

   /**
    * Debug output: Shows all stacks.
    */
   private fun debugDump() {
      logDebug(tag, "=== Navigation State ===")
      logDebug(tag, "Current Top-Level: $currentTopLevelKey")
      _topLevelBackStacks.forEach { (key, stack) ->
         val marker = if (key == currentTopLevelKey) ">>> " else "    "
         logDebug(tag, "$marker[$key] = ${stack.joinToString(prefix = "[", postfix = "]")}")
      }
      logDebug(tag, "=======================")
   }
}
