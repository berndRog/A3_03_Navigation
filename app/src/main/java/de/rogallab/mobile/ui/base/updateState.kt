package de.rogallab.mobile.ui.base

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

inline fun <T> updateState(
   mutableStateFlow: MutableStateFlow<T>,
   block: T.() -> T
) {
   mutableStateFlow.update(block)
}