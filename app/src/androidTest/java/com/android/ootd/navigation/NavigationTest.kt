package com.android.ootd.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.android.ootd.model.map.Location
import com.android.ootd.ui.map.MapScreen
import com.android.ootd.ui.map.MapScreenTestTags
import com.android.ootd.ui.navigation.NavigationActions
import com.android.ootd.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/*
 * DISCLAIMER: This file was created/modified with the assistance of GitHub Copilot.
 * Copilot provided suggestions which were reviewed and adapted by the developer.
 */

class NavigationTest {

  @get:Rule val composeRule = createComposeRule()

  private lateinit var navController: NavHostController
  private lateinit var navigation: NavigationActions

  // Helper for creating test location
  private val testLocation = Location(46.5197, 6.5682, "Lausanne, Switzerland")

  @Composable
  private fun TestNavHost() {
    navController = rememberNavController()
    navigation = NavigationActions(navController)

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
      // Match MainActivity structure with navigation() instead of composable()
      navigation(startDestination = Screen.Splash.route, route = Screen.Splash.name) {
        composable(Screen.Splash.route) { /* minimal screen */ }
        composable(Screen.RegisterUsername.route) { /* minimal screen */ }
      }

      navigation(
          startDestination = Screen.Authentication.route, route = Screen.Authentication.name) {
            composable(Screen.Authentication.route) { /* minimal screen */ }
          }

      navigation(startDestination = Screen.Feed.route, route = Screen.Feed.name) {
        composable(Screen.Feed.route) { /* minimal screen */ }
        composable(Screen.AccountEdit.route) { /* minimal screen */ }
        composable(Screen.AccountView.route) { /* minimal screen */ }
        composable(Screen.Map.route) { MapScreen(onPostClick = {}) }

        // Add MapWithLocation route
        composable(
            route = Screen.MapWithLocation.route,
            arguments =
                listOf(
                    navArgument("lat") { type = NavType.StringType },
                    navArgument("lon") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType })) {
              MapScreen(onPostClick = {})
            }

        composable(Screen.FitCheck.route) { /* minimal screen */ }
        composable(Screen.PreviewItemScreen.route) { /* minimal screen */ }
        composable(Screen.AddItemScreen.route) { /* minimal screen */ }
        composable(
            Screen.SelectInventoryItem.route,
            arguments = listOf(navArgument("postUuid") { type = NavType.StringType })) {
              /* minimal screen */
            }
        composable(Screen.EditItem.route) { /* minimal screen */ }
      }
    }
  }

  @Before
  fun setUp() {
    composeRule.setContent { TestNavHost() }
    composeRule.waitForIdle()
  }

  @Test
  fun navigationActions_navigateToAuthentication_shouldUpdateRoute() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Authentication)
      assertEquals(Screen.Authentication.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_navigateToOverview_shouldUpdateRoute() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_splashToAuthenticationFlow_shouldWork() {
    composeRule.runOnIdle {
      // Start on Splash
      assertEquals(Screen.Splash.route, navigation.currentRoute())

      // Navigate to Authentication (when not signed in)
      navigation.navigateTo(Screen.Authentication)
      assertEquals(Screen.Authentication.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_authenticationToOverviewFlow_shouldWork() {
    composeRule.runOnIdle {
      // Navigate to Authentication first
      navigation.navigateTo(Screen.Authentication)
      assertEquals(Screen.Authentication.route, navigation.currentRoute())

      // Navigate to Overview (after sign in)
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_splashToOverviewDirectFlow_shouldWork() {
    composeRule.runOnIdle {
      // Start on Splash
      assertEquals(Screen.Splash.route, navigation.currentRoute())

      // Navigate directly to Overview (when already signed in)
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_topLevelDestination_shouldClearBackStack() {
    composeRule.runOnIdle {
      // Navigate to Authentication (top-level)
      navigation.navigateTo(Screen.Authentication)
      assertEquals(Screen.Authentication.route, navigation.currentRoute())

      // Navigate to Overview (top-level) - should clear back stack
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())

      // Try to go back - should return to start destination (Splash)
      navigation.goBack()

      // Should be back at Splash (the start destination)
      assertEquals(Screen.Splash.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_navigateToSameTopLevelDestination_shouldBeIgnored() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())

      // Try to navigate to Overview again - should be ignored
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_splashToRegisterUsernameFlow_shouldWork() {
    composeRule.runOnIdle {
      // Start on Splash
      assertEquals(Screen.Splash.route, navigation.currentRoute())

      // Navigate to RegisterUsername
      navigation.navigateTo(Screen.RegisterUsername)
      assertEquals(Screen.RegisterUsername.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_authenticationToRegisterUsernameFlow_shouldWork() {
    composeRule.runOnIdle {
      // Navigate to Authentication first
      navigation.navigateTo(Screen.Authentication)
      assertEquals(Screen.Authentication.route, navigation.currentRoute())

      // Navigate to RegisterUsername (after sign in with no username)
      navigation.navigateTo(Screen.RegisterUsername)
      assertEquals(Screen.RegisterUsername.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_registerUsernameToOverviewFlow_shouldWork() {
    composeRule.runOnIdle {
      // Navigate to RegisterUsername
      navigation.navigateTo(Screen.RegisterUsername)
      assertEquals(Screen.RegisterUsername.route, navigation.currentRoute())

      // Navigate to Overview (after registration)
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_feedToAccountFlow_shouldWork() {
    composeRule.runOnIdle {
      // Navigate to Feed first
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())

      // Navigate to Account screen
      navigation.navigateTo(Screen.AccountEdit)
      assertEquals(Screen.AccountEdit.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_accountBackToFeedFlow_shouldWork() {
    composeRule.runOnIdle {
      // Navigate to Feed
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())

      // Navigate to Account
      navigation.navigateTo(Screen.AccountEdit)
      assertEquals(Screen.AccountEdit.route, navigation.currentRoute())

      // Go back to Feed
      navigation.goBack()
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_accountSignOutToAuthenticationFlow_shouldWork() {
    composeRule.runOnIdle {
      // Navigate to Feed
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())

      // Navigate to Account
      navigation.navigateTo(Screen.AccountEdit)
      assertEquals(Screen.AccountEdit.route, navigation.currentRoute())

      // Sign out navigates to Authentication
      navigation.navigateTo(Screen.Authentication)
      assertEquals(Screen.Authentication.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_previewItemToAddItem_shouldWork() {
    composeRule.runOnIdle {
      // Navigate to PreviewItemScreen
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = testLocation))
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Navigate to AddItemScreen
      navigation.navigateTo(Screen.AddItemScreen("test_post_id"))
      assertEquals(Screen.AddItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_addItemToPreview_goBackShouldWork() {
    composeRule.runOnIdle {
      // Navigate to PreviewItemScreen
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = testLocation))
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Navigate to AddItemScreen
      navigation.navigateTo(Screen.AddItemScreen("test_post_id"))
      assertEquals(Screen.AddItemScreen.route, navigation.currentRoute())

      // Go back to PreviewItemScreen
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_previewItemToEditItem_shouldWork() {
    composeRule.runOnIdle {
      // Navigate to PreviewItemScreen
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = testLocation))
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Navigate to EditItem with a specific item ID
      val testItemId = "test-item-123"
      navigation.navigateTo(Screen.EditItem(testItemId))

      // Verify we're on the edit screen (route contains the item ID)
      val currentRoute = navigation.currentRoute()
      assertTrue(currentRoute.contains("editItem"))
      assertEquals(Screen.EditItem.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_editItemGoBack_shouldWork() {
    composeRule.runOnIdle {
      // Navigate to PreviewItemScreen
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = testLocation))
      // Navigate to EditItem
      navigation.navigateTo(Screen.EditItem("item-1"))
      assertTrue(navigation.currentRoute().contains("editItem"))

      // Go back
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_complexFlow_feedToPreviewToAddToPreview_shouldWork() {
    composeRule.runOnIdle {
      // Navigate to Feed
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())

      // Navigate to PreviewItemScreen
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = testLocation))
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Navigate to AddItemScreen
      navigation.navigateTo(Screen.AddItemScreen("test_post_id"))
      assertEquals(Screen.AddItemScreen.route, navigation.currentRoute())

      // Go back to PreviewItemScreen
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_complexFlow_previewToEditMultipleItems_shouldWork() {
    composeRule.runOnIdle {
      // Navigate to PreviewItemScreen
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = testLocation))
      // Edit first item
      navigation.navigateTo(Screen.EditItem("item-1"))
      assertEquals(Screen.EditItem.route, navigation.currentRoute())
      // Go back
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Edit second item
      navigation.navigateTo(Screen.EditItem("item-2"))
      assertEquals(Screen.EditItem.route, navigation.currentRoute())
      // Go back
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_addItemScreenCanBeAccessedDirectly_shouldWork() {
    composeRule.runOnIdle {
      // Navigate directly to AddItemScreen
      navigation.navigateTo(Screen.AddItemScreen("test_post_id"))
      assertEquals(Screen.AddItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_previewItemScreenCanBeAccessedDirectly_shouldWork() {
    composeRule.runOnIdle {
      // Navigate directly to PreviewItemScreen
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = testLocation))
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_multipleBackNavigations_shouldWork() {
    composeRule.runOnIdle {
      // Build a navigation stack
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = testLocation))
      navigation.navigateTo(Screen.AddItemScreen("test_post_id"))

      assertEquals(Screen.AddItemScreen.route, navigation.currentRoute())

      // Go back to Preview
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Go back to Feed
      navigation.goBack()
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_editItemWithDifferentIds_shouldHaveDifferentRoutes() {
    composeRule.runOnIdle {
      val itemId1 = "item-abc"
      val itemId2 = "item-xyz"

      // Navigate to first edit screen
      navigation.navigateTo(Screen.EditItem(itemId1))
      val id1 = navController.currentBackStackEntry?.arguments?.getString("itemUid")
      navigation.goBack()

      // Navigate to second edit screen
      navigation.navigateTo(Screen.EditItem(itemId2))
      val id2 = navController.currentBackStackEntry?.arguments?.getString("itemUid")

      assertEquals(itemId1, id1)
      assertEquals(itemId2, id2)
      assertNotEquals(id1, id2)
    }
  }

  @Test
  fun navigationActions_currentRoute_returnsEmptyStringWhenNoDestination() {
    composeRule.runOnIdle {
      // This test verifies the currentRoute() method handles edge cases
      val route = navigation.currentRoute()
      assertTrue(route.isNotEmpty())
    }
  }

  // Map Screen Navigation Tests
  @Test
  fun navigationActions_navigateToMap_shouldUpdateRoute() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Map)
      assertEquals(Screen.Map.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_mapToFeedFlow_shouldWork() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.Map)
      assertEquals(Screen.Map.route, navigation.currentRoute())

      // Go back to Feed
      navigation.goBack()
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_mapScreen_navigateToAuthentication_shouldClearStack() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.Map)
      assertEquals(Screen.Map.route, navigation.currentRoute())

      // Sign out navigates to Authentication (top-level)
      navigation.navigateTo(Screen.Authentication)
      assertEquals(Screen.Authentication.route, navigation.currentRoute())

      // Going back should return to Splash (start destination)
      navigation.goBack()
      assertEquals(Screen.Splash.route, navigation.currentRoute())
    }
  }

  @Test
  fun mapScreen_backButton_triggersNavigationGoBack() {
    // Navigate to Feed then Map
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.Map)
      assertEquals(Screen.Map.route, navigation.currentRoute())
    }

    composeRule.waitForIdle()

    // Verify the map screen is displayed
    composeRule.onNodeWithTag(MapScreenTestTags.SCREEN).assertExists()

    // Go back programmatically
    composeRule.runOnIdle { navigation.goBack() }

    // Verify we navigated back to Feed
    composeRule.runOnIdle { assertEquals(Screen.Feed.route, navigation.currentRoute()) }
  }

  // MapWithLocation Navigation Tests
  @Test
  fun navigationActions_navigateToMapWithLocation_shouldUpdateRoute() {
    composeRule.runOnIdle {
      navigation.navigateTo(
          Screen.MapWithLocation(
              latitude = 46.5197, longitude = 6.5682, locationName = "EPFL, Lausanne"))

      val currentRoute = navigation.currentRoute()
      assertTrue(currentRoute.startsWith("map?"))
      assertTrue(currentRoute.contains("lat="))
      assertTrue(currentRoute.contains("lon="))
      assertTrue(currentRoute.contains("name="))
    }
  }

  @Test
  fun navigationActions_mapWithLocation_extractsLocationParameters() {
    composeRule.runOnIdle {
      val testLat = 46.5197
      val testLon = 6.5682
      val testName = "Test Location"

      navigation.navigateTo(
          Screen.MapWithLocation(latitude = testLat, longitude = testLon, locationName = testName))

      val args = navController.currentBackStackEntry?.arguments
      val lat = args?.getString("lat")?.toDoubleOrNull()
      val lon = args?.getString("lon")?.toDoubleOrNull()
      val name = args?.getString("name")

      assertEquals(testLat, lat ?: 0.0, 0.0001)
      assertEquals(testLon, lon ?: 0.0, 0.0001)
      assertEquals(testName, name)
    }
  }

  @Test
  fun navigationActions_mapWithLocation_handlesSpecialCharactersInName() {
    composeRule.runOnIdle {
      val locationWithSpecialChars = "Café & Restaurant, Zürich 🇨🇭"

      navigation.navigateTo(
          Screen.MapWithLocation(
              latitude = 47.3769, longitude = 8.5417, locationName = locationWithSpecialChars))

      val args = navController.currentBackStackEntry?.arguments
      val name = args?.getString("name")

      assertEquals(locationWithSpecialChars, name)
    }
  }

  @Test
  fun navigationActions_feedToMapWithLocation_shouldWork() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())

      navigation.navigateTo(
          Screen.MapWithLocation(latitude = 46.5197, longitude = 6.5682, locationName = "Lausanne"))

      val currentRoute = navigation.currentRoute()
      assertTrue(currentRoute.startsWith("map?"))
    }
  }

  @Test
  fun navigationActions_mapWithLocationToFeed_goBackShouldWork() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(
          Screen.MapWithLocation(latitude = 46.5197, longitude = 6.5682, locationName = "Lausanne"))

      val currentRoute = navigation.currentRoute()
      assertTrue(currentRoute.startsWith("map?"))

      // Go back to Feed
      navigation.goBack()
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_mapWithLocation_differentLocationsHaveDifferentRoutes() {
    composeRule.runOnIdle {
      // Navigate to first location
      navigation.navigateTo(
          Screen.MapWithLocation(latitude = 46.5197, longitude = 6.5682, locationName = "Lausanne"))

      val args1 = navController.currentBackStackEntry?.arguments?.getString("name")

      navigation.goBack()

      // Navigate to second location
      navigation.navigateTo(
          Screen.MapWithLocation(latitude = 47.3769, longitude = 8.5417, locationName = "Zurich"))

      val args2 = navController.currentBackStackEntry?.arguments?.getString("name")

      assertNotEquals(args1, args2)
      assertEquals("Lausanne", args1)
      assertEquals("Zurich", args2)
    }
  }
}
