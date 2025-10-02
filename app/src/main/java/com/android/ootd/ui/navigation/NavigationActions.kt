package com.android.ootd.ui.navigation

import androidx.navigation.NavHostController

/**
 * Represents a navigation destination in the app.
 *
 * Each [Screen] defines:
 * - a unique navigation `route` used by the NavController,
 * - a human\-readable `name` for UI/analytics/accessibility,
 * - whether it is a top\-level destination (`isTopLevelDestination`).
 *
 * Top\-level destinations are typically roots in the app's navigation graph
 *
 * @property route Unique route string used by the navigation graph.
 * @property name Human\-readable screen name for accessibility and analytics.
 * @property isTopLevelDestination If true, navigation to this screen should clear the back stack
 *   and use single\-top behavior.
 */
sealed class Screen(
    val route: String,
    val name: String,
    val isTopLevelDestination: Boolean = false
) {
  /** Sign in screen. Marked as a top\-level destination. */
  object SignIn : Screen(route = "sign_in", name = "Sign In", isTopLevelDestination = true)

  /** Splash / launch screen. Marked as a top\-level destination. */
  object Splash : Screen(route = "splash", name = "Splash", isTopLevelDestination = false)
}

open class NavigationActions(
    private val navController: NavHostController,
) {
  /**
   * Navigate to the specified screen.
   *
   * @param screen The screen to navigate to
   */
  open fun navigateTo(screen: Screen) {
    if (screen.isTopLevelDestination && currentRoute() == screen.route) {
      // If the user is already on the top-level destination, do nothing
      return
    }
    navController.navigate(screen.route) {
      if (screen.isTopLevelDestination) {
        launchSingleTop = true
        popUpTo(screen.route) { inclusive = true }
      }
    }
  }

  /** Navigate back to the previous screen. */
  open fun goBack() {
    navController.popBackStack()
  }

  /**
   * Get the current route of the navigation controller.
   *
   * @return The current route
   */
  open fun currentRoute(): String {
    return navController.currentDestination?.route ?: ""
  }
}
