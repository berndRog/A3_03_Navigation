package de.rogallab.mobile

import android.app.Application
import de.rogallab.mobile.data.IDataStore
import de.rogallab.mobile.di.appModules
import de.rogallab.mobile.domain.utilities.logInfo
import de.rogallab.mobile.ui.people.PersonViewModel
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import kotlin.getValue

class MainApplication : Application() {

   override fun onCreate() {
      super.onCreate()
      logInfo(TAG, "onCreate()")

      // Initialize Koin dependency injection
      logInfo(TAG, "onCreate(): startKoin{...}")
      startKoin {
         androidLogger(Level.DEBUG)
         // Reference to Android context
         androidContext(androidContext = this@MainApplication)
         // Load modules
         modules(appModules)
      }

      val _dataStore: IDataStore by inject()
      _dataStore.initialize()
   }

   companion object {
      private const val TAG = "<-MainApplication"
   }

}
