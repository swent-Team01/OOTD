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
 * * Portions adapted from Bootcamp Week 3 Solutions (source:
 * * https://github.com/swent-epfl/bootcamp-25-B3-Solution.git)
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
  object Authentication :
      Screen(route = "authentication", name = "Authentication", isTopLevelDestination = true)

  object RegisterUsername :
      Screen(route = "register", name = "Register", isTopLevelDestination = true)

  /** Splash / launch screen. Marked as a top\-level destination. */
  object Splash : Screen(route = "splash", name = "Splash", isTopLevelDestination = false)

  object Feed : Screen(route = "feed", name = "Feed", isTopLevelDestination = true)

  object Account : Screen(route = "account", name = "Account", isTopLevelDestination = false)

  // TODO: add routes for Search Screen and Profile Screen

  data class EditItem(val itemUid: String) :
      Screen(route = "editItem/${itemUid}", name = "Edit Item") {
    companion object {
      const val route = "editItem/{itemUid}"
    }
  }
}

/**
 * High-level navigation helper around [NavHostController].
 *
 * Encapsulates common navigation behavior and enforces consistent handling of top-level
 * destinations:
 * - Re-navigation to the current top-level route is ignored to avoid duplicates.
 * - Navigating to a top-level [Screen] uses `launchSingleTop` and clears the back stack up to the
 *   destination via `popUpTo(destination) { inclusive = true }`.
 *
 * Threading: calls must happen on the main thread as they delegate to [NavHostController]. Scope:
 * keep one instance per `NavHostController` (e.g., hoisted to a ViewModel or a CompositionLocal in
 * Compose).
 *
 * @param navController The [NavHostController] used to perform navigation actions.
 */
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
        // Clear back stack to start destination when navigating to top-level screens
        popUpTo(navController.graph.startDestinationId) {
          inclusive = true
          // Don't save state when navigating to Authentication (e.g., after sign-out)
          saveState = screen !is Screen.Authentication
        }
      }

      if (screen !is Screen.Authentication) {
        // Restore state when reselecting a previously selected item
        restoreState = true
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
