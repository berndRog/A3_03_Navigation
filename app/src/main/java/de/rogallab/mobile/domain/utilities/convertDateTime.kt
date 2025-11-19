@file:OptIn(ExperimentalTime::class)

package de.rogallab.mobile.domain.utilities

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import java.util.Locale
import kotlin.text.any
import kotlin.text.contains
import kotlin.text.padStart
import kotlin.text.substringAfter
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// Multiplatform for java.util.Locale
// commonMain
// expect fun currentLocale(): String

// androidMain
// actual fun currentLocale(): String =
fun currentLocale(): String =
   Locale.getDefault().language

// iosMain
//actual fun currentLocale(): String =
//   NSLocale.currentLocale.languageCode

// jsMain
//actual fun currentLocale(): String =
//   NSLocale.currentLocale.languageCode


// DateTimeUtils.kt

// region ISO 8601 to LocalDateTime and ZonedDateTime

/**
 * Parses this ISO-8601 string into a [LocalDateTime].
 *
 * Behavior:
 * - If the string contains time zone information (a 'Z' suffix or ±hh:mm offset),
 *   it is first parsed as an [Instant] and then converted to a [LocalDateTime]
 *   using the given [zoneId].
 *   Example: "2025-01-20T14:30:00Z" or "2025-01-20T14:30:00+02:00"
 *
 * - If the string does NOT contain any time zone information,
 *   it is treated as a pure local date-time and parsed directly via [LocalDateTime.parse],
 *   independent of the given [zoneId].
 *   Example: "2025-01-20T14:30:00"
 */
fun String.toLocalDateTimeFromIso(zoneId: String = "UTC"): LocalDateTime {
   return if (hasZoneInfo()) {
      // Example: "2025-01-20T14:30:00Z" or "2025-01-20T14:30:00+02:00"
      val instant: Instant = Instant.parse(this)
      val zone = TimeZone.of(zoneId)
      instant.toLocalDateTime(zone)
   } else {
      // Example: "2025-01-20T14:30:00" -> pure local date-time (no zone)
      LocalDateTime.parse(this)
   }
}

/**
 * Returns true if this ISO-like string contains zone information:
 * - 'Z' anywhere in the string, or
 * - a '+' or '-' character in the time part (after 'T'), indicating an offset.
 */
private fun String.hasZoneInfo(): Boolean {
   val timePart = substringAfter('T', missingDelimiterValue = "")
   return 'Z' in this || timePart.any { it == '+' || it == '-' }
}

/**
 * Represents a date-time with full zone information.
 *
 * @property local  Local date-time in the target zone.
 * @property instant The absolute instant (UTC-based point in time).
 * @property zone   The IANA time zone used to interpret the instant.
 * @property offset Zone offset at this instant (e.g. +01:00, +02:00).
 *
 * @property epochMillis Epoch milliseconds of [instant].
 * @property nanos Nanoseconds within the second of [instant].
 */
data class ZonedDateTime(
   val local: LocalDateTime,
   val instant: Instant,
   val zone: TimeZone,
   val offset: UtcOffset,
) {
   val epochMillis: Long = instant.toEpochMilliseconds()
   val nanos: Int = instant.nanosecondsOfSecond
}

/**
 * Converts an ISO-8601 string into a [ZonedDateTime] using the provided IANA time zone.
 *
 * The ISO string may include:
 * - a 'Z' suffix (UTC)
 * - an offset (e.g. +02:00, -05:00)
 * - or no time zone at all (then it is treated as a [LocalDateTime] in the target zone)
 *
 * Examples:
 *  - "2025-01-20T14:30:00Z"
 *  - "2025-01-20T14:30:00+02:00"
 *  - "2025-01-20T14:30:00"
 */
fun String.toZonedDateTimeFromIso(targetZoneId: String): ZonedDateTime {
   val targetZone = TimeZone.of(targetZoneId)

   return if (hasZoneInfo()) {
      // Case 1: ISO string already contains Z or an offset
      val instant: Instant = Instant.parse(this)

      // Convert the instant into local time for the target zone
      val local: LocalDateTime = instant.toLocalDateTime(targetZone)

      // Determine the actual offset for this instant in the target zone
      val offset: UtcOffset = targetZone.offsetAt(instant)

      ZonedDateTime(local, instant, targetZone, offset)
   } else {
      // Case 2: ISO string has no zone → treat it as a LocalDateTime in the target zone
      val local: LocalDateTime = LocalDateTime.parse(this)

      // Convert LocalDateTime to an Instant using the target zone
      val instant: Instant = local.toInstant(targetZone)

      // Determine the offset in that zone at this instant
      val offset: UtcOffset = targetZone.offsetAt(instant)

      ZonedDateTime(local, instant, targetZone, offset)
   }
}

// endregion

// region LocalDateTime / ZonedDateTime to ISO 8601

/**
 * Interprets this [LocalDateTime] in the system's default time zone
 * and returns an ISO-8601 string of the corresponding [Instant] in UTC.
 *
 * Note:
 * - The returned string is always in UTC and ends with 'Z'.
 * - Example: for Europe/Berlin (UTC+1 in January), a local
 *   2025-01-20T14:30 will become "2025-01-20T13:30:00Z".
 */
fun LocalDateTime.toIsoStringWithLocalZone(): String {
   val systemZone = TimeZone.currentSystemDefault()
   val instant: Instant = this.toInstant(systemZone)
   return instant.toString() // ISO-8601 in UTC (Z)
}

/**
 * Returns an ISO-8601 string of this [ZonedDateTime] using the local date-time
 * and the zone offset, e.g. "2025-01-20T14:30:00+01:00".
 *
 * This is useful if you want a human-readable local time plus explicit offset,
 * instead of a pure UTC representation.
 */
fun ZonedDateTime.toIsoOffsetString(): String {
   // LocalDateTime.toString() → "YYYY-MM-DDTHH:MM:SS[.nnn...]"
   // UtcOffset.toString()      → "+HH:MM" or "-HH:MM"
   return "${local}$offset"
}

/**
 * Returns the ISO-8601 representation of the underlying [instant] in UTC.
 * This is effectively the same as [Instant.toString].
 *
 * Example: "2025-01-20T13:30:00Z".
 */
fun ZonedDateTime.toIsoInstantString(): String =
   instant.toString()

// endregion

// region LocalDateTime → Date/Time formatted strings

/**
 * Formats this [LocalDateTime] as a date string according to the given [locale].
 *
 * Supported formats:
 * - "de": dd.MM.yyyy
 * - "en": MM/dd/yyyy
 * - default: dd.MM.yyyy
 */
fun LocalDateTime.toDateString(
   locale: String = currentLocale()
): String {
   val dts: DateTimeString = this.formatted()
   return when (locale) {
      "de" -> "${dts.day}.${dts.month}.${dts.year}"
      "en" -> "${dts.month}/${dts.day}/${dts.year}"
      else -> "${dts.day}.${dts.month}.${dts.year}"
   }
}

/**
 * Formats this [LocalDateTime] as a time string in the form HH:mm:ss.
 */
fun LocalDateTime.toTimeString(): String {
   val dts: DateTimeString = this.formatted()
   return "${dts.hour}:${dts.min}:${dts.sec}"
}

/**
 * Formats this [LocalDateTime] as a combined date-time string according to [locale].
 *
 * Supported formats:
 * - "de": dd.MM.yyyy HH:mm:ss
 * - "en": MM/dd/yyyy HH:mm:ss
 * - default: dd.MM.yyyy HH:mm:ss
 */
fun LocalDateTime.toDateTimeString(
   locale: String = currentLocale()
): String {
   val dts: DateTimeString = this.formatted()
   val time = "${dts.hour}:${dts.min}:${dts.sec}"
   return when (locale) {
      "de" -> "${dts.day}.${dts.month}.${dts.year} $time"
      "en" -> "${dts.month}/${dts.day}/${dts.year} $time"
      else -> "${dts.day}.${dts.month}.${dts.year} $time"
   }
}

/**
 * Internal helper that breaks a [LocalDateTime] into a preformatted struct
 * with zero-padded components (year, month, day, hour, minute, second, millis).
 */
private fun LocalDateTime.formatted(): DateTimeString =
   DateTimeString(
      year = this.date.year.toString(),
      month = this.date.monthNumber.toString().padStart(2, '0'),
      day = this.date.dayOfMonth.toString().padStart(2, '0'),
      dayOfWeek = this.date.dayOfWeek.name,
      hour = this.time.hour.toString().padStart(2, '0'),
      min = this.time.minute.toString().padStart(2, '0'),
      sec = this.time.second.toString().padStart(2, '0'),
      mil = (this.time.nanosecond / 1_000_000).toString().padStart(3, '0')
   )

// endregion

// region LocalDateTime "now"

/**
 * Returns the current [LocalDateTime] in the system's default time zone.
 *
 * This is a convenience "static" extension on [LocalDateTime.Companion].
 */
fun LocalDateTime.Companion.now(): LocalDateTime {
   // Current instant in UTC (kotlin.time.Instant)
   val instantNow: Instant = Clock.System.now()
   // Convert the instant to a LocalDateTime in the system's default time zone
   return instantNow.toLocalDateTime(TimeZone.currentSystemDefault())
}

// endregion

/**
 * Internal structure used for locale-dependent formatted date/time strings.
 */
private data class DateTimeString(
   val year: String,
   val month: String,
   val day: String,
   val dayOfWeek: String,
   val hour: String,
   val min: String,
   val sec: String,
   val mil: String
)
