package de.rogallab.mobile.test.domain.utilities

import de.rogallab.mobile.domain.utilities.ZonedDateTime
import de.rogallab.mobile.domain.utilities.now
import de.rogallab.mobile.domain.utilities.toDateString
import de.rogallab.mobile.domain.utilities.toDateTimeString
import de.rogallab.mobile.domain.utilities.toIsoInstantString
import de.rogallab.mobile.domain.utilities.toIsoOffsetString
import de.rogallab.mobile.domain.utilities.toIsoStringWithLocalZone
import de.rogallab.mobile.domain.utilities.toLocalDateTimeFromIso
import de.rogallab.mobile.domain.utilities.toTimeString
import de.rogallab.mobile.domain.utilities.toZonedDateTimeFromIso
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
class ConvertDateTimeUt {

   // region ISO to LocalDateTime ------------------------------------------------------------------
   // 1) ISO with 'Z' (UTC) -> target zone UTC
   @Test
   fun localDateTime_from_IsoWithZ_toUtc() {
      val iso = "2025-01-20T14:30:00Z"

      val result = iso.toLocalDateTimeFromIso("UTC")

      // In UTC, local date-time is identical to the UTC time in the ISO string
      assertEquals(
         LocalDateTime(2025, 1, 20, 14, 30, 0),
         result,
         "LocalDateTime in UTC should match the UTC time in the ISO string"
      )
   }

   // 2) ISO with 'Z' (UTC) -> Europe/Berlin (winter, UTC+1)
   @Test
   fun localDateTime_from_IsoWithZ_toBerlinWinter() {
      val iso = "2025-01-20T14:30:00Z"

      val result = iso.toLocalDateTimeFromIso("Europe/Berlin")

      // January: Berlin is UTC+1 -> 14:30Z becomes 15:30 local time
      assertEquals(
         LocalDateTime(2025, 1, 20, 15, 30, 0),
         result,
         "UTC instant should be converted to Berlin local winter time (UTC+1)"
      )
   }

   //3) ISO with 'Z' (UTC) -> Europe/Berlin (summer, UTC+2)
   @Test
   fun localDateTime_from_IsoWithZ_toBerlinSummer_LocalDateTime() {
      val iso = "2025-07-20T12:30:00Z"

      val result = iso.toLocalDateTimeFromIso("Europe/Berlin")

      // July: Berlin is UTC+2 -> 12:30Z becomes 14:30 local time
      assertEquals(
         LocalDateTime(2025, 7, 20, 14, 30, 0),
         result,
         "UTC instant should be converted to Berlin local summer time (UTC+2)"
      )
   }

   // 4) ISO with positive offset (+02:00) -> Europe/Berlin (winter, UTC+1)
   @Test
   fun localDateTime_from_IsoWithPositiveOffset_toBerlinWinter() {
      val iso = "2025-01-20T14:30:00+02:00"
      // 14:30 at UTC+2 => instant is 12:30Z
      // January in Berlin: UTC+1 => local time = 13:30

      val result = iso.toLocalDateTimeFromIso("Europe/Berlin")

      assertEquals(
         LocalDateTime(2025, 1, 20, 13, 30, 0),
         result,
         "Offset +02:00 should be correctly converted to Berlin winter time (UTC+1)"
      )
   }

   // 5) ISO with positive offset (+02:00) -> Europe/Athens (winter, also UTC+2)
   @Test
   fun localDateTime_from_IsoWithPositiveOffset_toAthensWinter() {
      val iso = "2025-01-20T14:30:00+02:00"
      // January in Athens: UTC+2, same as the offset in the string

      val result = iso.toLocalDateTimeFromIso("Europe/Athens")

      // Local time remains the same as in the ISO string
      assertEquals(
         LocalDateTime(2025, 1, 20, 14, 30, 0),
         result,
         "Offset +02:00 should match Athens winter time (UTC+2) without change"
      )
   }

   // 6) ISO with negative offset (-05:00) -> Europe/Berlin (winter, UTC+1)
   @Test
   fun localDateTime_from_IsoWithNegativeOffset_toBerlinWinter() {
      val iso = "2025-01-20T08:00:00-05:00"
      // 08:00 at UTC-5 => 13:00Z
      // January in Berlin: UTC+1 => 14:00 local

      val result = iso.toLocalDateTimeFromIso("Europe/Berlin")

      assertEquals(
         LocalDateTime(2025, 1, 20, 14, 0, 0),
         result,
         "Offset -05:00 should be correctly converted to Berlin winter time (UTC+1)"
      )
   }

   // 7) ISO without zone -> treated as pure LocalDateTime, zoneId is ignored
   @Test
   fun localDateTime_from_IsoWithoutZone_isParsedAsLocalDateTime() {
      val iso = "2025-01-20T14:30:00"

      val resultBerlin = iso.toLocalDateTimeFromIso("Europe/Berlin")
      val resultTokyo = iso.toLocalDateTimeFromIso("Asia/Tokyo")

      // Without zone info, the string is parsed as a pure LocalDateTime,
      // so the result does not depend on the provided zoneId.
      val expected = LocalDateTime(2025, 1, 20, 14, 30, 0)

      assertEquals(expected, resultBerlin, "LocalDateTime must match the ISO value regardless of zoneId")
      assertEquals(expected, resultTokyo, "LocalDateTime must match the ISO value regardless of zoneId")
   }
   // endregion

   // region ISO to ZonedDateTime ------------------------------------------------------------------
   // 1) ISO with 'Z' (UTC) → ZonedDateTime in Europe/Berlin (winter)
   @Test
   fun zonedDateTime_from_IsoWithZ_toBerlinWinter() {
      // 2025-01-20T14:30Z as an absolute instant
      val iso = "2025-01-20T14:30:00Z"

      val zoned = iso.toZonedDateTimeFromIso("Europe/Berlin")

      // In January, Berlin is UTC+1 -> local time = 15:30
      assertEquals(LocalDateTime(2025, 1, 20, 15, 30, 0), zoned.local, "Local date-time mismatch")
      assertEquals("Europe/Berlin", zoned.zone.id, "Zone ID mismatch")
      assertEquals("+01:00", zoned.offset.toString(), "Offset mismatch")
      // Instant should remain exactly what the ISO string represents in UTC
      assertEquals("2025-01-20T14:30:00Z", zoned.instant.toString(), "Instant ISO mismatch")
   }

   // 2) ISO with +02:00 offset → Europe/Berlin (winter)
   @Test
   fun zonedDateTime_IsoWithPositiveOffset_toBerlinWinter() {
      // 14:30 at UTC+2 => 12:30Z
      val iso = "2025-01-20T14:30:00+02:00"

      val zoned = iso.toZonedDateTimeFromIso("Europe/Berlin")

      // In January, Berlin is UTC+1 -> 12:30Z => 13:30 local
      assertEquals(LocalDateTime(2025, 1, 20, 13, 30, 0), zoned.local, "Local date-time mismatch")
      assertEquals("Europe/Berlin", zoned.zone.id, "Zone ID mismatch")
      assertEquals("+01:00", zoned.offset.toString(), "Offset mismatch")
      // The underlying instant must be 12:30Z
      assertEquals("2025-01-20T12:30:00Z", zoned.instant.toString(), "Instant ISO mismatch")
   }

   // 3) ISO with +02:00 offset → Europe/Athens (winter)
   @Test
   fun zonedDateTime_isoWithPositiveOffset_toAthensWinter() {
      // 14:30 at UTC+2 => 12:30Z
      val iso = "2025-01-20T14:30:00+02:00"

      val zoned = iso.toZonedDateTimeFromIso("Europe/Athens")

      // In January, Athens is also UTC+2 -> local time stays 14:30
      assertEquals(LocalDateTime(2025, 1, 20, 14, 30, 0), zoned.local, "Local date-time mismatch")
      assertEquals("Europe/Athens", zoned.zone.id, "Zone ID mismatch")
      assertEquals("+02:00", zoned.offset.toString(), "Offset mismatch")
      // Instant must still be 12:30Z
      assertEquals("2025-01-20T12:30:00Z", zoned.instant.toString(), "Instant ISO mismatch")
   }

   // 4) ISO without zone → treated as LocalDateTime in target zone
   @Test
   fun zonedDateTime_from_IsoWithoutZone_toBerlinWinter() {
      // Local date-time without zone information
      val iso = "2025-01-20T14:30:00"

      val zoned = iso.toZonedDateTimeFromIso("Europe/Berlin")

      // Local time is taken as-is in Berlin
      assertEquals(LocalDateTime(2025, 1, 20, 14, 30, 0), zoned.local, "Local date-time mismatch")
      assertEquals("Europe/Berlin", zoned.zone.id, "Zone ID mismatch")
      // In January, Berlin is UTC+1 -> 14:30 local => 13:30Z
      assertEquals("+01:00", zoned.offset.toString(), "Offset mismatch")
      assertEquals("2025-01-20T13:30:00Z", zoned.instant.toString(), "Instant ISO mismatch")
   }

   // 5) ISO with 'Z' → Europe/Berlin (summer time)
   @Test
   fun zonedDateTime_from_IsoWithZ_toBerlinSummer() {
      // 12:30Z in July
      val iso = "2025-07-20T12:30:00Z"

      val zoned = iso.toZonedDateTimeFromIso("Europe/Berlin")

      // In July, Berlin is UTC+2 -> local time = 14:30
      assertEquals(LocalDateTime(2025, 7, 20, 14, 30, 0), zoned.local, "Local date-time mismatch")
      assertEquals("Europe/Berlin", zoned.zone.id, "Zone ID mismatch")
      assertEquals("+02:00", zoned.offset.toString(), "Offset mismatch")
      assertEquals("2025-07-20T12:30:00Z", zoned.instant.toString(), "Instant ISO mismatch")
   }

   // 6) ISO with negative offset → Europe/Berlin (winter)
   @Test
   fun zonedDateTime_from_IsoWithNegativeOffset_toBerlinWinter() {
      // 08:00 at UTC-5 => 13:00Z
      val iso = "2025-01-20T08:00:00-05:00"

      val zoned = iso.toZonedDateTimeFromIso("Europe/Berlin")

      // In January, Berlin is UTC+1 -> 13:00Z => 14:00 local
      assertEquals(LocalDateTime(2025, 1, 20, 14, 0, 0), zoned.local, "Local date-time mismatch")
      assertEquals("Europe/Berlin", zoned.zone.id, "Zone ID mismatch")
      assertEquals("+01:00", zoned.offset.toString(), "Offset mismatch")
      assertEquals("2025-01-20T13:00:00Z", zoned.instant.toString(), "Instant ISO mismatch")
   }

   // 7) Round-trip: LocalDateTime in zone → Instant ISO → back to ZonedDateTime
   @Test
   fun zonedDateTime_roundTrip_localToInstantIso_backToZonedDateTime() {
      val zoneId = "Europe/Berlin"
      val zone = TimeZone.of(zoneId)

      // Local date-time in Berlin (winter, UTC+1)
      val local = LocalDateTime(2025, 1, 20, 14, 30, 0)

      // Convert LocalDateTime to instant using the zone
      val instant = local.toInstant(zone)

      // Serialize instant to ISO string (UTC with 'Z')
      val isoInstant = instant.toString()

      // Parse back to ZonedDateTime in the same zone
      val zoned = isoInstant.toZonedDateTimeFromIso(zoneId)

      // The instant must be identical
      assertEquals(instant.toString(), zoned.instant.toString(), "Instant mismatch after round-trip")

      // The local time recovered from the instant for the same zone must match zoned.local
      assertEquals(zoned.local, zoned.instant.toLocalDateTime(zone), "Local time inconsistent with zone")
   }
   // endregion

   // region ZonedDateTime → ISO 8601 --------------------------------------------------------------
   // 8) ZonedDateTime.toIsoOffsetString should combine local date-time and offset.
   @Test
   fun zonedDateTime_to_IsoOffsetString() {
      val zone = TimeZone.of("Europe/Berlin")
      val local = LocalDateTime(2025, 1, 20, 14, 30, 0)
      val instant = local.toInstant(zone) // 13:30Z in January
      val offset = zone.offsetAt(instant)

      val zoned = ZonedDateTime(local, instant, zone, offset)

      val isoOffset = zoned.toIsoOffsetString()

      // LocalDateTime.toString() -> "2025-01-20T14:30"
      // Offset.toString()        -> "+01:00"
      assertEquals("2025-01-20T14:30+01:00", isoOffset, "ISO offset string mismatch")
   }

   // 9) ZonedDateTime.toIsoInstantString should be the UTC instant string.
   @Test
   fun zonedDateTime_to_IsoInstantString() {
      val zone = TimeZone.of("Europe/Berlin")
      val local = LocalDateTime(2025, 1, 20, 14, 30, 0)
      val instant = local.toInstant(zone) // 13:30Z in January
      val offset = zone.offsetAt(instant)

      val zoned = ZonedDateTime(local, instant, zone, offset)

      val isoInstant = zoned.toIsoInstantString()

      assertEquals("2025-01-20T13:30:00Z", isoInstant, "ISO instant string mismatch")
   }
   // endregion

   // region LocalDateTime → ISO / formatted strings -----------------------------------------------
    /*
    * 10) LocalDateTime.toIsoStringWithLocalZone should match manual instant conversion.
    *
    * This test does not assume a specific system time zone.
    * It just checks that the helper behaves the same as the "manual" path.
    */
   @Test
   fun localDateTime_to_IsoStringWithLocalZone_consistency() {
      val systemZone = TimeZone.currentSystemDefault()
      val local = LocalDateTime(2025, 1, 20, 14, 30, 0)

      // Manual path: LocalDateTime -> Instant(systemZone) -> ISO string
      val manualInstant = local.toInstant(systemZone)
      val manualIso = manualInstant.toString()

      // Helper under test
      val helperIso = local.toIsoStringWithLocalZone()

      assertEquals(manualIso, helperIso, "toIsoStringWithLocalZone must match the manual conversion logic")
   }

   /**
    * 11) LocalDateTime.toDateString for German and English locales.
    */
   @Test
   fun localDateTime_to_DateString_de_and_en() {
      val local = LocalDateTime(2025, 1, 2, 3, 4, 5, 6_000_000)

      val de = local.toDateString("de")
      val en = local.toDateString("en")

      assertEquals("02.01.2025", de, "German date format mismatch")
      assertEquals("01/02/2025", en, "English date format mismatch")
   }

   // 12) LocalDateTime.toTimeString should produce HH:mm:ss.
   @Test
   fun localDateTime_to_TimeString_basic() {
      val local = LocalDateTime(2025, 1, 2, 3, 4, 5, 6_000_000)

      val time = local.toTimeString()

      assertEquals("03:04:05", time, "Time format mismatch")
   }

   // 13) LocalDateTime.toDateTimeString for German and English locales.
   @Test
   fun localDateTime_to_DateTimeString_de_and_en() {
      val local = LocalDateTime(2025, 1, 2, 3, 4, 5, 6_000_000)

      val de = local.toDateTimeString("de")
      val en = local.toDateTimeString("en")

      assertEquals("02.01.2025 03:04:05", de, "German date-time format mismatch")
      assertEquals("01/02/2025 03:04:05", en, "English date-time format mismatch")
   }
   // endregion

   // region LocalDateTime.now ---------------------------------------------------------------------

   /**
    * 14) LocalDateTime.now should be consistent with Clock.System.now + system zone.
    *
    * We do not assert an exact timestamp, only that both ways map the same instant to the same local time.
    */
   @Test
   fun localDateTime_now_consistency() {
      val systemZone = TimeZone.currentSystemDefault()

      // Reference: current instant and its LocalDateTime
      val instantNow: Instant = Clock.System.now()
      val referenceLocal = instantNow.toLocalDateTime(systemZone)

      // Under test: LocalDateTime.now()
      val nowFromHelper = LocalDateTime.now()

      // We cannot guarantee millisecond-perfect equality due to timing,
      // but it should at least match year/month/day and be close in time.
      assertEquals(referenceLocal.date, nowFromHelper.date, "Date part should match the reference LocalDateTime date")

      val diffSeconds =
         abs(referenceLocal.time.toSecondOfDay() - nowFromHelper.time.toSecondOfDay())

      assertTrue(
         diffSeconds <= 5,
         "Difference between reference and now() should be within a few seconds (was $diffSeconds seconds)"
      )
   }

   // endregion
}