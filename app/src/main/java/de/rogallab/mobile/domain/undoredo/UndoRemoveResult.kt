package de.rogallab.mobile.domain.undoredo

/**
 * Result type for undo helper:
 *  - updatedList: list after re-inserting the item (if possible)
 *  - newBuffer: usually the cleared buffer
 *  - restoredId: ID of the restored item (for scroll/highlight), or null
 */
data class UndoRemoveResult<T>(
   val updatedList: List<T>,
   val newBuffer: SingleSlotUndoBuffer<T>,
   val restoredId: String?
)

/**
 * Pure helper:
 *  - uses undo buffer to reinsert removed item into list
 *  - clears the buffer
 *  - if item is already in list → only clears buffer, list unchanged
 */
fun <T> optimisticUndoRemove(
   list: List<T>,
   getId: (T) -> String,
   undoBuffer: SingleSlotUndoBuffer<T>
): UndoRemoveResult<T> {

   val removedItem = undoBuffer.removedItem
   val removedIndex = undoBuffer.removedIndex

   if (removedItem == null || removedIndex == -1) {
      // Nothing to undo
      return UndoRemoveResult(
         updatedList = list,
         newBuffer = undoBuffer,  // unchanged
         restoredId = null
      )
   }

   val itemId = getId(removedItem)

   // If the item is already present, just clear the buffer
   if (list.any { getId(it) == itemId }) {
      return UndoRemoveResult(
         updatedList = list,
         newBuffer = undoBuffer.cleared(),
         restoredId = null
      )
   }

   val mutable = list.toMutableList()
   val safeIndex = removedIndex.coerceAtMost(mutable.size)
   mutable.add(safeIndex, removedItem)

   return UndoRemoveResult(
      updatedList = mutable.toList(),
      newBuffer = undoBuffer.cleared(),
      restoredId = itemId
   )
}
