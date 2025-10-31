package de.rogallab.mobile.domain

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri

interface IAppStorage {

   // Converts a drawable resource to an image file  the app's storage
   fun convertDrawableToAppStorage(
      drawableId: Int,
      pathName: String,  // images/people31
      uuidString: String?
   ): Uri?

}
