package de.rogallab.mobile.ui.base.composables

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import de.rogallab.mobile.domain.utilities.logDebug
import de.rogallab.mobile.domain.utilities.logError
import de.rogallab.mobile.domain.utilities.logVerbose

/**
 * ScrollToItemIfNotVisible — Auto-scroll helper for LazyColumn/LazyRow
 *
 * PURPOSE
 * -------
 * Ensures that a specific item inside a LazyList becomes visible when triggered
 * by an external key. If the item is already visible, no scroll occurs.
 * If not, the list scrolls (animated or instant) to bring it into view.
 *
 * This is typically used for:
 *  - Jump-to-selected-item after user interaction
 *  - Restoring scroll position after navigation
 *  - Highlighting or focusing an item that was updated
 *  - Synchronizing selection across multiple UI components
 *
 * HOW IT WORKS
 * -------------
 * 1. `LaunchedEffect(targetKey)` reacts whenever a new key is provided.
 * 2. It finds the index of the associated item in the list.
 * 3. It checks if the item is currently visible in the viewport.
 * 4. If not visible → scroll or animate to the item.
 * 5. Regardless of success or error, `acknowledge()` is called to clear the trigger.
 *
 * PARAMETERS
 * ----------
 * @param listState      LazyListState controlling scroll & layout.
 * @param targetKey      Key of the target item or null to do nothing.
 * @param items          Full list of items backing the LazyList.
 * @param keyOf          Function that extracts the key from an item.
 * @param acknowledge    Callback executed after processing the scroll attempt.
 * @param animate        True → animateScrollToItem, False → scrollToItem.
 * @param scrollOffset   Additional pixel offset applied to the target index.
 * @param fullyVisible   True → require item completely visible.
 *                       False → allow partial visibility.
 *
 * LIFECYCLE NOTES
 * ----------------
 * - No UI is emitted; this is a pure side-effect Composable.
 * - Never throws; failures are logged.
 * - Works with both LazyColumn and LazyRow.
 */
@Composable
fun <I, K> ScrollToItemIfNotVisible(
   listState: LazyListState,
   targetKey: K?,
   items: List<I>,
   keyOf: (I) -> K,
   acknowledge: () -> Unit,
   animate: Boolean = true,
   scrollOffset: Int = 0,
   fullyVisible: Boolean = false
) {       //12345678901234567890123
   val tag = "<-ScrToItemIfNotVis"

   // Trigger whenever targetKey changes
   LaunchedEffect(targetKey) {
      val key = targetKey ?: return@LaunchedEffect
      logDebug(tag, "Triggered for key: $key | animate=$animate fullyVisible=$fullyVisible")

      // Try to find the index of the item with the matching key
      val index = items.indexOfFirst { keyOf(it) == key }
      if (index < 0) {
         logError(tag, "Key not found in items → acknowledge()")
         acknowledge()
         return@LaunchedEffect
      }
      logDebug(tag, "Found key at index: $index")

      // Determine whether the target item is currently visible
      val isVisible = isIndexVisible(listState, index, fullyVisible)
      logVerbose(tag, "Visibility check: isVisible=$isVisible")
      if (!isVisible) {
         logDebug(tag, "Item not visible → scrolling (offset=$scrollOffset)")

         try {
            if (animate) {
               listState.animateScrollToItem(index, scrollOffset)
               logDebug(tag, "animateScrollToItem completed")
            } else {
               listState.scrollToItem(index, scrollOffset)
               logDebug(tag, "scrollToItem completed")
            }
         } catch (t: Throwable) {
            logError(tag, "Scrolling failed: ${t.message}")
         }
      } else {
         logDebug(tag, "Item already visible → no action required")
      }

      // Always call acknowledge, even in error scenarios
      acknowledge()
      logVerbose(tag, "Acknowledge executed")
   }
}

/**
 * Checks whether an item at a given index is currently visible
 * inside the LazyList viewport.
 *
 * @param fullyVisible If true → item must be completely visible.
 *                     If false → any partial visibility counts.
 */
private fun isIndexVisible(
   listState: LazyListState,
   index: Int,
   fullyVisible: Boolean
): Boolean {
   val tag = "<-isIndexVisible"

   val infos = listState.layoutInfo.visibleItemsInfo

   // No visible items → nothing can be visible
   if (infos.isEmpty()) {
      logVerbose(tag, "No visibleItemsInfo available (viewport empty)")
      return false
   }

   // Simple partial visibility
   if (!fullyVisible) {
      val partiallyVisible = infos.any { it.index == index }
      logVerbose(tag, "Partially visible result: $partiallyVisible")
      return partiallyVisible
   }

   // Full visibility check: requires the whole item to be inside the viewport
   val viewportStart = listState.layoutInfo.viewportStartOffset
   val viewportEnd = listState.layoutInfo.viewportEndOffset

   val fullyVisibleCheck = infos.any { item ->
      item.index == index &&
         item.offset >= viewportStart &&
         (item.offset + item.size) <= viewportEnd
   }

   logVerbose(tag, "Full visibility result: $fullyVisibleCheck (viewport: $viewportStart..$viewportEnd)")
   return fullyVisibleCheck
}