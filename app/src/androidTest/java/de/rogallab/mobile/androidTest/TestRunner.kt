package de.rogallab.mobile.androidTest

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class TestRunner : AndroidJUnitRunner() {
  override fun newApplication(cl: ClassLoader, className: String, context: Context): Application {
    // Erzwingt die TestApplication statt MainApplication
    return super.newApplication(cl, "de.rogallab.mobile.androidTest.TestApplication", context)
  }
}
