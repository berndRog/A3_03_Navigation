package de.rogallab.mobile.test

import android.app.Application
import org.koin.core.context.stopKoin

class TestApplication : Application() {
   override fun onCreate() {
      super.onCreate()
      // try { stopKoin() } catch (e: Exception) { }
   }
}