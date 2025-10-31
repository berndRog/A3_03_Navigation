package de.rogallab.mobile.domain.entities
import kotlinx.serialization.Serializable

@Serializable
data class Person(
   val firstName: String = "",
   val lastName: String = "",
   val email: String? = "",
   val phone:String? = "",
   val imagePath: String? = null,
   val id: String
)