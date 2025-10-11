package de.rogallab.mobile.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// Navigation destinations as NavKeys
@Serializable
data object PeopleList : NavKey
@Serializable
data object PersonInput : NavKey
@Serializable
data class PersonDetail(val id: String) : NavKey