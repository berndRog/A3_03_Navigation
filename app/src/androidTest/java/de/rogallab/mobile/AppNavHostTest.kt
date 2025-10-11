//// AppNavHostTest.kt
package de.rogallab.mobile.ui.navigation
//
//import androidx.compose.ui.test.junit4.createComposeRule
//import androidx.navigation.NavHostController
//import androidx.navigation.compose.rememberNavController
//import de.rogallab.mobile.ui.navigation.composables.AppNavHost
//import de.rogallab.mobile.ui.people.PersonViewModel
//import org.junit.Rule
//import org.junit.Test
//import org.koin.test.KoinTest
//import org.koin.test.inject
//
//class AppNavHostTest : KoinTest {
//
//   @get:Rule
//   val composeTestRule = createComposeRule()
//
//   private val viewModel: PersonViewModel by inject()
//
////   @Test
////   fun testNavigationToPeopleList() {
////
////      var navController: NavHostController? = null
////      composeTestRule.setContent {
////         navController = rememberNavController()
////         AppNavHost(peopleViewModel = viewModel)
////      }
////
////      // Perform navigation action
////      composeTestRule.runOnIdle {
////         viewModel.onNavigate(NavEvent.NavigateForward(NavScreen.PeopleList.route))
////      }
////
////      // Verify the current route
////      composeTestRule.runOnIdle {
////         assert(navController?.currentDestination?.route == NavScreen.PeopleList.route)
////      }
////   }
///*
//   @Test
//   fun testNavigationToPersonInput() {
//      composeTestRule.setContent {
//         val navController = rememberNavController()
//         AppNavHost(viewModel = viewModel)
//      }
//
//      // Perform navigation action
//      composeTestRule.runOnIdle {
//         viewModel.navigationChannelFlow.tryEmit(NavEvent.ToPersonInput)
//      }
//
//      // Verify the current route
//      composeTestRule.runOnIdle {
//         assert(navController.currentDestination?.route == NavScreen.PersonInput.route)
//      }
//   }
//
//   @Test
//   fun testNavigationToPersonDetail() {
//      val personId = UUID.randomUUID().toString()
//      composeTestRule.setContent {
//         val navController = rememberNavController()
//         AppNavHost(viewModel = viewModel)
//      }
//
//      // Perform navigation action
//      composeTestRule.runOnIdle {
//         viewModel.navigationChannelFlow.tryEmit(NavEvent.ToPersonDetail(personId))
//      }
//
//      // Verify the current route
//      composeTestRule.runOnIdle {
//         assert(navController.currentDestination?.route == "${NavScreen.PersonDetail.route}/$personId")
//      }
//   }
//
// */
//}