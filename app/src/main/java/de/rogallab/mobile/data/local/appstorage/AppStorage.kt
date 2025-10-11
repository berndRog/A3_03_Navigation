package de.rogallab.mobile.data.local.appstorage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import de.rogallab.mobile.domain.IAppStorage
import de.rogallab.mobile.domain.exceptions.IoException
import de.rogallab.mobile.domain.utilities.logError
import de.rogallab.mobile.domain.utilities.newUuid
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AppStorage(
   private val _context: Context
) : IAppStorage {

   // Convert a drawable resource to an image file in app's private storage
   override fun convertDrawableToAppStorage(
      context: Context,
      drawableId: Int,
      pathName: String,
      uuidString: String?
   ): Uri? {
      return try {

         var uuidStringLocal = uuidString
         if(uuidStringLocal.isNullOrBlank()) uuidStringLocal = newUuid()
         // Load bitmap from drawable resource
         val resources = context.resources
         val bitmap = BitmapFactory.decodeResource(resources, drawableId)
            ?: throw IllegalArgumentException("Failed to decode drawable resource: $drawableId")
         // Save bitmap to app's private files directory
         val imagesDir = File(context.filesDir, "images/$pathName").apply { if (!exists()) mkdirs() }
         val imageFile = File(imagesDir, "$uuidStringLocal.jpg")
         FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
         }
         Uri.fromFile(imageFile)
      } catch (e: Exception) {
         throw IoException("Failed to convert drawable to app storage: ${e.message}")
      } finally {
         // Note: bitmap might be null if decodeResource failed, so check before recycling
         val bitmap = BitmapFactory.decodeResource(context.resources, drawableId)
         bitmap?.recycle()
      }
   }

   // Load bitmap from any URI (MediaStore, file URI, content URI)
   override fun loadImageFromAppStorage(uri: Uri): Bitmap? {
      return try {
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(_context.contentResolver, uri) // Fix: _context
            ImageDecoder.decodeBitmap(source)
         } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(_context.contentResolver, uri) // Fix: _context
         }
      } catch (e: Exception) {
         logError("<-deleteImageOnAppStorage",
            e.localizedMessage ?: "Failed to load image from app storage")
         null
      }
   }

   override fun deleteImageOnAppStorage(pathName:String) {
      try {
         // pathName for the image file
         File(pathName).apply {
            this.absoluteFile.delete()
         }
      } catch(e:IOException ) {
         logError("<-deleteImageOnAppStorage",
            e.localizedMessage ?: "Failed to delete file: $pathName")
         throw e
      }
   }



   companion object {
      private const val TAG = "<-MediaStore"
   }
}