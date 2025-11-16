package de.rogallab.mobile.di

import androidx.navigation3.runtime.NavKey
import de.rogallab.mobile.data.IDataStore
import de.rogallab.mobile.data.local.Seed
import de.rogallab.mobile.data.local.appstorage.AppStorage
import de.rogallab.mobile.data.local.datastore.DataStore
import de.rogallab.mobile.data.repositories.PersonRepository
import de.rogallab.mobile.domain.IAppStorage
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.utilities.logInfo
import de.rogallab.mobile.ui.navigation.INavHandler
import de.rogallab.mobile.ui.navigation.Nav3ViewModel
import de.rogallab.mobile.ui.navigation.PeopleList
import de.rogallab.mobile.ui.people.PersonValidator
import de.rogallab.mobile.ui.people.PersonViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel      // für viewModel { ... }
import org.koin.dsl.bind
import org.koin.dsl.module

val defModules: Module = module {
    val tag = "<-defModules"

    // data modules
   logInfo(tag, "single    -> Seed")
   single<Seed> {
      Seed(
         _context = androidContext(),
         _isTest = false
      )
   }

   logInfo(tag, "single    -> DataStore: IDataStore")
   single<IAppStorage> {
      AppStorage( _context = androidContext() )
   }

   logInfo(tag, "single    -> DataStore: IDataStore")
   single<IDataStore> {
      DataStore(
         directoryName = null,
         fileName = null,
         _context = androidContext(),
         _seed = get<Seed>()
      )
   }

    logInfo(tag, "single    -> PersonRepository: IPersonRepository")
    single<IPersonRepository> {
        PersonRepository(_dataStore = get<IDataStore>())
    }

    // ui modules
    logInfo(tag, "single    -> PersonValidator")
    single<PersonValidator> {
        PersonValidator(_context = androidContext())
    }

    logInfo(tag, "viewModel -> Nav3ViewModel as INavHandler (with params)")
    viewModel { (startDestination: NavKey) ->  // Parameter for startDestination
        Nav3ViewModel(startDestination = startDestination)
    } bind INavHandler::class
    
    logInfo(tag, "viewModel -> PersonViewModel")
    viewModel { (navHandler: INavHandler) ->
        PersonViewModel(
           _repository = get<IPersonRepository>(),
           _navHandler = navHandler,
           _validator = get<PersonValidator>()
        )
    }
}

val appModules: Module = module {

    try {
        val testedModules = defModules
        requireNotNull(testedModules) {
            "definedModules is null"
        }
        includes(
           testedModules,
           //useCaseModules
        )
    } catch (e: Exception) {
        logInfo("<-appModules", e.message!!)
    }
}
