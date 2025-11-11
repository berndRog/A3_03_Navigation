package de.rogallab.mobile.data.local

import android.content.Context
import de.rogallab.mobile.Globals
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IAppStorage
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.logDebug
import de.rogallab.mobile.domain.utilities.sanitizeDigit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.util.Locale
import kotlin.random.Random

class Seed(
   private val _context: Context,
   private val _isTest: Boolean = false
): KoinComponent {
   private val _fileName = Globals.file_name
   private val _imageDirectoryName = File(_fileName).nameWithoutExtension

   private val _appStorage: IAppStorage by inject()

   var people: MutableList<Person> = mutableListOf<Person>()
   init {
      val firstNames = mutableListOf(
         "Arne", "Berta", "Cord", "Dagmar", "Ernst", "Frieda", "Günter", "Hanna",
         "Ingo", "Johanna", "Klaus", "Luise", "Martin", "Nadja", "Otto", "Patrizia",
         "Quirin", "Rebecca", "Stefan", "Tanja", "Uwe", "Veronika", "Walter", "Xenia",
         "Yannick", "Zwantje")
      val lastNames = mutableListOf(
         "Arndt", "Bauer", "Conrad", "Diehl", "Engel", "Fischer", "Graf", "Hoffmann",
         "Imhoff", "Jung", "Klein", "Lang", "Meier", "Neumann", "Olbrich", "Peters",
         "Quart", "Richter", "Schmidt", "Thormann", "Ulrich", "Vogel", "Wagner", "Xander",
         "Yakov", "Zander")
      val emailProvider = mutableListOf("gmail.com", "icloud.com", "outlook.com", "yahoo.com",
         "t-online.de", "gmx.de", "freenet.de", "mailbox.org", "yahoo.com", "web.de")
      val random = Random(0)
      for (index in firstNames.indices) {
//         var indexFirst = random.nextInt(firstNames.size)
//         var indexLast = random.nextInt(lastNames.size)
         val firstName = firstNames[index]
         val lastName = lastNames[index]
         val email =
            "${sanitizeDigit(firstName.lowercase(locale = Locale.ROOT))}." +
               "${sanitizeDigit(lastName.lowercase(locale = Locale.ROOT))}@" +
               "${emailProvider.random()}"
         val phone: String =
            "0${random.nextInt(1234, 9999)} " +
               "${random.nextInt(100, 999)}-" +
               "${random.nextInt(10, 9999)}"

         val uuid = String.format(Locale.ROOT, "%02d000000-0000-0000-0000-000000000000", index+1)
         val person = Person(firstName, lastName, email, phone, null, uuid)
         people.add(person)
      }

      // convert the drawables into image files
      if(!_isTest) createImages()
   }

   private fun createImages() {
      val drawables = mutableListOf(
         R.drawable.man_01, R.drawable.woman_01, R.drawable.man_02, R.drawable.woman_02,
         R.drawable.man_03, R.drawable.woman_03, R.drawable.man_04, R.drawable.woman_04,
         R.drawable.man_05, R.drawable.woman_05, R.drawable.man_06, R.drawable.woman_06,
         R.drawable.man_07, R.drawable.woman_07, R.drawable.man_08, R.drawable.woman_08,
         R.drawable.man_09, R.drawable.woman_09, R.drawable.man_10, R.drawable.woman_10,
         R.drawable.man_11, R.drawable.woman_11, R.drawable.man_12, R.drawable.woman_12,
         R.drawable.man_13, R.drawable.woman_13
      )

      var index = -1
      drawables.forEach { it: Int ->  // drawable id
         index++
         val uuidString = String.format(Locale.ROOT, "%02d000000-0000-0000-0000-000000000000", index + 1)
         // /data/data/de.rogallab.mobile.images/files/images/filename/
         _appStorage.convertDrawableToAppStorage(
            drawableId = it,
            pathName = _imageDirectoryName,
            uuidString = uuidString
         )?.let { uri ->
            uri.path?.let { uriString ->
               logDebug("<Seed>", "Uri: $uriString")
               people[index] = people[index].copy(imagePath = uriString)
            }
         }
      }
   }
}