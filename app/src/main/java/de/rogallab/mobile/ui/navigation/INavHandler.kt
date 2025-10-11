package de.rogallab.mobile.ui.navigation

import androidx.navigation3.runtime.NavKey

interface INavHandler {
   fun push(destination: NavKey)
   fun pop() // pop the last entry from the back stack
   fun popToRootAndNavigate(rootDestination: NavKey)
   // You could add other common navigation actions here if needed
   // fun navigateWithArguments(destination: NavKey, args: Bundle) // Example
}