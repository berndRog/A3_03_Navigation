package de.rogallab.mobile.domain.undoredo

// Single-slot UNDO buffer for any item type T
data class SingleSlotUndoBuffer<T>(
   val removedItem: T? = null,
   val removedIndex: Int = -1
) {
   val hasUndo: Boolean
      get() = removedItem != null && removedIndex >= 0

   fun cleared(): SingleSlotUndoBuffer<T> = SingleSlotUndoBuffer()
}


/**
* Pure helper:
*  - finds the item by ID
*  - removes it from the list
*  - returns new list + updated undo buffer
*/
fun <T> optimisticRemove(
   list: List<T>,
   item: T,
   getId: (T) -> String,
   undoBuffer: SingleSlotUndoBuffer<T>
): Pair<List<T>, SingleSlotUndoBuffer<T>> {

   val index = list.indexOfFirst { getId(it) == getId(item) }
   if (index == -1) {
      // Nothing removed → return original state unchanged
      return list to undoBuffer
   }

   val updatedList = list.toMutableList().also {
      it.removeAt(index)
   }.toList()

   val newBuffer = SingleSlotUndoBuffer(
      removedItem = item,
      removedIndex = index
   )

   return updatedList to newBuffer
}