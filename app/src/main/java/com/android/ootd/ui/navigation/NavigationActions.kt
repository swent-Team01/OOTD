package com.android.ootd.ui.navigation

import android.net.Uri
import androidx.navigation.NavHostController
import com.android.ootd.model.map.Location

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

  object AccountEdit :
      Screen(route = "accountEdit", name = "Account Edit", isTopLevelDestination = false)

  object AccountView :
      Screen(route = "accountView", name = "Account View", isTopLevelDestination = false)

  data class FitCheck(val postUuid: String = "") :
      Screen(route = "fitCheck?postUuid=$postUuid", name = "FitCheck") {
    companion object {
      const val route = "fitCheck?postUuid={postUuid}"
    }
  }

  data class AddItemScreen(val postUuid: String) :
      Screen(route = "addItem?postUuid=$postUuid", name = "Add Item") {
    companion object {
      const val route = "addItem?postUuid={postUuid}"
    }
  }

  data class PreviewItemScreen(
      val imageUri: String,
      val description: String,
      val location: Location
  ) :
      Screen(
          route =
              "overview?imageUri=${Uri.encode(imageUri)}&description=${Uri.encode(description)}&locationLat=${location.latitude}&locationLon=${location.longitude}&locationName=${Uri.encode(location.name)}",
          name = "Overview") {
    companion object {
      const val route =
          "overview?imageUri={imageUri}&description={description}&locationLat={locationLat}&locationLon={locationLon}&locationName={locationName}"
    }
  }

  data class SeeFitScreen(val postUuid: String) :
      Screen(route = "seeFit/${postUuid}", name = "See Fit", isTopLevelDestination = false) {
    companion object {
      const val route = "seeFit/{postUuid}"
    }
  }

  object Onboarding :
      Screen(route = "onboarding", name = "Onboarding", isTopLevelDestination = true)

  data class Map(
      val latitude: Double? = null,
      val longitude: Double? = null,
      val locationName: String? = null,
      val mapType: String? = null
  ) :
      Screen(
          route = buildMapRoute(latitude, longitude, locationName, mapType),
          name = "Map",
          isTopLevelDestination = false) {
    companion object {
      const val route = "map?lat={lat}&lon={lon}&name={name}&mapType={mapType}"

      private fun buildMapRoute(
          lat: Double?,
          lon: Double?,
          name: String?,
          mapType: String?
      ): String {
        val params = mutableListOf<String>()
        if (lat != null && lon != null && name != null) {
          params.add("lat=$lat")
          params.add("lon=$lon")
          params.add("name=${Uri.encode(name)}")
        }
        if (mapType != null) {
          params.add("mapType=$mapType")
        }
        return if (params.isEmpty()) "map" else "map?${params.joinToString("&")}"
      }
    }
  }

  object SearchScreen : Screen(route = "search", name = "Search", isTopLevelDestination = false)

  object InventoryScreen :
      Screen(route = "inventory", name = "Inventory", isTopLevelDestination = false)

  object NotificationsScreen :
      Screen(route = "notifications", name = "Notifications", isTopLevelDestination = false)

  data class EditItem(val itemUid: String) :
      Screen(route = "editItem/${itemUid}", name = "Edit Item") {
    companion object {
      const val route = "editItem/{itemUid}"
    }
  }

  data class PostView(val postId: String) :
      Screen(route = "postView/${postId}", name = "Post View") {
    companion object {
      const val route = "postView/{postId}"
    }
  }

  data class ViewUser(val userId: String) : Screen(route = "viewUser/$userId", name = "View User") {
    companion object {
      const val ROUTE = "viewUser/{userId}"
    }
  }

  data class SelectInventoryItem(val postUuid: String) :
      Screen(route = "selectInventoryItem/$postUuid", name = "Select Inventory Item") {
    companion object {
      const val route = "selectInventoryItem/{postUuid}"
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
        popUpTo(navController.graph.startDestinationId) { saveState = true }
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
   * Pop up to a specific route in the back stack.
   *
   * @param route The route to pop up to
   */
  fun popUpTo(route: String) {
    navController.popBackStack(route, inclusive = false)
  }

  /**
   * Get the current route of the navigation controller.
   *
   * @return The current route
   */
  open fun currentRoute(): String {
    return navController.currentDestination?.route ?: ""
  }

  /**
   * Navigate to a user's profile screen. If the userId matches the current user, navigate to
   * AccountView. Otherwise, navigate to ViewUser screen.
   *
   * @param userId The ID of the user whose profile to view
   * @param currentUserId The ID of the currently logged-in user
   */
  fun navigateToUserProfile(userId: String, currentUserId: String?) {
    if (userId == currentUserId) {
      navigateTo(Screen.AccountView)
    } else {
      navigateTo(Screen.ViewUser(userId))
    }
  }
}
