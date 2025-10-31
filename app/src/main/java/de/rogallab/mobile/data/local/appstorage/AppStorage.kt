package de.rogallab.mobile.data.local.appstorage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import de.rogallab.mobile.domain.IAppStorage
import de.rogallab.mobile.domain.exceptions.IoException
import de.rogallab.mobile.domain.utilities.newUuid
import java.io.File
import java.io.FileOutputStream

class AppStorage(
   private val _context: Context
) : IAppStorage {

   // Convert a drawable resource to an image file in app's private storage
   override fun convertDrawableToAppStorage(
      drawableId: Int,
      pathName: String,
      uuidString: String?
   ): Uri?  {

      var bitmap: Bitmap? = null

      try {
         var uuidLocal = if(uuidString.isNullOrBlank()) newUuid() else uuidString

         // Load bitmap from drawable resource
         bitmap = BitmapFactory.decodeResource(_context.resources, drawableId)
            ?: throw IllegalArgumentException("Failed to decode drawable resource: $drawableId")

         // Prepare destination directory and file
         val imagesDir = File(_context.filesDir, pathName).apply { if (!exists()) mkdirs() }
         val imageFile = File(imagesDir, "$uuidLocal.jpg")

         // Save bitmap to app's private files directory
         FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
         }
         return Uri.fromFile(imageFile)
      } catch (e: Exception) {
         throw IoException("Failed to convert drawable to app storage: ${e.message}")
      } finally {
         bitmap?.recycle()
      }
   }

   companion object {
      private const val TAG = "<-AppStorage"
   }
}