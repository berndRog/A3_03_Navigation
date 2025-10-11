package de.rogallab.mobile.domain.utilities

import java.util.Locale
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/*
// UUID is handled as String
@OptIn(ExperimentalUuidApi::class)
fun Uuid.as8(): String = this.toString().substring(0..7)+"..."

@OptIn(ExperimentalUuidApi::class)
fun newUuid(): Uuid = Uuid.random()


@OptIn(ExperimentalUuidApi::class)
fun emptyUuid(): Uuid = Uuid.parse("00000000-0000-0000-0000-000000000000")
*/

//// UUID is handled as String
//fun String.as8(): String =
//   if(this.length < 8) this
//   else this.substring(0..7) + "..."

fun newUuid(): String = UUID.randomUUID().toString()

fun emptyUuid(): String = "00000000-0000-0000-0000-000000000000"

fun createUuid(number:Int, value:Int): String =
   String.format(Locale.ROOT, "%08d-%04d-0000-0000-000000000000", number, value)
