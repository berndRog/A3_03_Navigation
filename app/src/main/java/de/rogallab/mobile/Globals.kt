package de.rogallab.mobile

import androidx.compose.material3.SnackbarDuration

object Globals {
   const val directory_name = "android"
   const val file_name = "people33.json"

   const val ANIMATION_DURATION = 500
   val snackbarDuration = SnackbarDuration.Long  // 10000 ms

   var isDebug = true
   var isInfo = true
   var isVerbose = false
   var isComp = true
}