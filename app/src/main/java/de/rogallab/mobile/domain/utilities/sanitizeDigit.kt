package de.rogallab.mobile.domain.utilities

import java.text.Normalizer
import java.util.regex.Pattern
import kotlin.text.replace

fun sanitizeDigit(input: String): String {
   val replaced = input
      .replace("ä", "ae")
      .replace("ö", "oe")
      .replace("ü", "ue")
      .replace("Ä", "Ae")
      .replace("Ö", "Oe")
      .replace("Ü", "Ue")
      .replace("ß", "ss")

   val normalized = Normalizer.normalize(replaced, Normalizer.Form.NFD)
   val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
   return pattern.matcher(normalized).replaceAll("")
      .replace(Regex("[^A-Za-z0-9@._-]"), "") // optional: entferne alle nicht erlaubten Zeichen
}