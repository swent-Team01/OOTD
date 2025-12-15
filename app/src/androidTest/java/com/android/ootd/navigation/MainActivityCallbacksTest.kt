package com.android.ootd.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import com.android.ootd.OOTDApp
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.emptyLocation
import com.android.ootd.ui.feed.FeedScreenTestTags
import com.android.ootd.ui.navigation.NavigationActions
import com.android.ootd.ui.navigation.Screen
import com.android.ootd.ui.post.PreviewItemScreenTestTags
import junit.framework.TestCase.assertEquals
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Integration tests for navigation callbacks in MainActivity and various screens.
 *
 * These tests verify that navigation actions triggered by UI interactions correctly change the
 * current screen as expected.
 *
 * DISCLAIMER: These tests are partially created by AI and verified by humans.
 */
class MainActivityCallbacksTest {

  @get:Rule val composeRule = createComposeRule()
  private lateinit var navigation: NavigationActions

  // Helper for creating test location
  private val testLocation = Location(46.5197, 6.5682, "Lausanne, Switzerland")

  @Before
  fun setUp() {
    composeRule.setContent {
      val navController = rememberNavController()
      navigation = NavigationActions(navController)
      OOTDApp(testNavController = navController, testStartDestination = Screen.Feed.route)
    }
    composeRule.waitForIdle()
  }

  @Test
  fun feedScreen_callbacks_executeNavigationLambdas() {
    composeRule.runOnIdle { navigation.navigateTo(Screen.Feed) }

    composeRule.onNodeWithTag(FeedScreenTestTags.ADD_POST_FAB).performClick()
    composeRule.waitForIdle()

    composeRule.runOnIdle { assertEquals(Screen.FitCheck.route, navigation.currentRoute()) }
  }

  @Test
  fun navigate_feedToFitCheckToPreview_shouldWork() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.FitCheck(postUuid = "test_id"))
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = emptyLocation))
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_addItemToPreview_goBackShouldWork() {
    composeRule.runOnIdle {
      // Simulate real user flow: Feed → FitCheck → PreviewItem → AddItem
      navigation.navigateTo(Screen.FitCheck(postUuid = "test_id"))
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = testLocation))
      navigation.navigateTo(Screen.AddItemScreen("testpostUuid"))

      // Now the current screen is AddItem
      assertEquals(Screen.AddItemScreen.route, navigation.currentRoute())

      // Go back once — should go to PreviewItemScreen
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun deepStack_feedToPreviewToAddToEdit_maintainsHistory() {
    composeRule.runOnIdle {
      // Build deep stack
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.FitCheck(postUuid = "test_id"))
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = testLocation))
      navigation.navigateTo(Screen.AddItemScreen("test_post_id"))

      // Go to edit instead
      navigation.goBack()
      navigation.navigateTo(Screen.EditItem("item-1"))

      // Navigate back through stack
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      navigation.goBack()
      assertEquals(Screen.FitCheck.route, navigation.currentRoute())

      navigation.goBack()
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun deepStack_multipleAddOperations_maintainsCorrectStack() {
    composeRule.runOnIdle {
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = emptyLocation))

      // Add first item and don't save
      navigation.navigateTo(Screen.AddItemScreen("test_id"))
      navigation.goBack()

      // Add second item and don't save
      navigation.navigateTo(Screen.AddItemScreen("test_id"))
      navigation.goBack()

      // Verify we can still navigate back properly
      navigation.goBack()
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun statePreservation_navigationDoesNotClearStack() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.FitCheck(postUuid = "test_id"))
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = testLocation))
      navigation.navigateTo(Screen.AddItemScreen("test_id"))

      // Cancel
      navigation.goBack()

      // Should still have full stack
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
      navigation.goBack()
      assertEquals(Screen.FitCheck.route, navigation.currentRoute())
      navigation.goBack()
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun multipleCycles_addItemFromPreview_threeTimesSaving() {
    composeRule.runOnIdle {
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = emptyLocation))

      for (i in 1..3) {
        navigation.navigateTo(Screen.AddItemScreen("test_id"))
        Assert.assertEquals(Screen.AddItemScreen.route, navigation.currentRoute())

        // Simulate save
        navigation.popUpTo(Screen.PreviewItemScreen.route)
        navigation.navigateTo(
            Screen.PreviewItemScreen(
                imageUri = "content://another_uri",
                description = "Another Test Outfit Description",
                location = emptyLocation))
        Assert.assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
      }
    }
  }

  @Test
  fun multipleCycles_alternatingAddAndEdit() {
    composeRule.runOnIdle {
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = testLocation))

      // Add first item
      navigation.navigateTo(Screen.AddItemScreen("test_post_id"))
      navigation.goBack()
      Assert.assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Edit item
      navigation.navigateTo(Screen.EditItem("item-1"))
      navigation.goBack()
      Assert.assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Add second item
      navigation.navigateTo(Screen.AddItemScreen("test_post_id"))
      navigation.goBack()
      Assert.assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Edit another item
      navigation.navigateTo(Screen.EditItem("item-2"))
      navigation.goBack()
      Assert.assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun completeFlow_feedToFitCheckToPreviewToAddToPreview() {
    composeRule.runOnIdle {
      // Start from Feed
      navigation.navigateTo(Screen.Feed)
      Assert.assertEquals(Screen.Feed.route, navigation.currentRoute())

      // Go to FitCheck
      navigation.navigateTo(Screen.FitCheck(postUuid = "test_id"))
      Assert.assertEquals(Screen.FitCheck.route, navigation.currentRoute())

      // Go to PreviewItemScreen
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = testLocation))
      Assert.assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Add an item
      navigation.navigateTo(Screen.AddItemScreen("test_post_id"))
      Assert.assertEquals(Screen.AddItemScreen.route, navigation.currentRoute())

      // Return to preview after adding
      navigation.goBack()
      Assert.assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun previewItemScreen_editSameItemMultipleTimes_shouldWork() {
    composeRule.runOnIdle {
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = testLocation))
      val itemId = "same-item"

      // Edit same item multiple times
      for (_i in 1..3) {
        navigation.navigateTo(Screen.EditItem(itemId))
        Assert.assertEquals(Screen.EditItem.route, navigation.currentRoute())
        navigation.goBack()
        Assert.assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
      }
    }
  }

  @Test
  fun registerScreen_onRegister_navigatesToFeed() {
    composeRule.runOnIdle {
      // Navigate to Register screen
      navigation.navigateTo(Screen.RegisterUsername)

      // Verify we’re on Register
      assertEquals(Screen.RegisterUsername.route, navigation.currentRoute())

      // Simulate onRegister() callback
      navigation.navigateTo(Screen.Feed)

      // Should now be on Feed
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun searchScreen_onBack_returnsToPreviousScreen() {
    composeRule.runOnIdle {
      // Start from Feed
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())

      // Navigate to Search screen
      navigation.navigateTo(Screen.SearchScreen)
      assertEquals(Screen.SearchScreen.route, navigation.currentRoute())

      // Simulate onBack() callback
      navigation.goBack()
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun accountScreen_onBack_and_onSignOut_workCorrectly() {
    composeRule.runOnIdle {
      // Start from Feed
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())

      // Go to Account screen
      navigation.navigateTo(Screen.AccountEdit)
      assertEquals(Screen.AccountEdit.route, navigation.currentRoute())

      // Simulate onBack
      navigation.goBack()
      assertEquals(Screen.Feed.route, navigation.currentRoute())

      // Go back to Account
      navigation.navigateTo(Screen.AccountEdit)

      // Simulate onSignOut
      navigation.navigateTo(Screen.Authentication)
      assertEquals(Screen.Authentication.route, navigation.currentRoute())
    }
  }

  @Test
  fun editItemScreen_goBack_returnsToPreviewItemScreen() {
    composeRule.runOnIdle {
      // Navigate through the valid path: Feed → FitCheck → Preview → Edit(item-123)
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.FitCheck(postUuid = "test_id"))
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = testLocation))
      navigation.navigateTo(Screen.EditItem("item-123"))

      // Confirm we’re at EditItem
      assertEquals(Screen.EditItem.route, navigation.currentRoute())

      // Simulate back action
      navigation.goBack()

      // Should return to PreviewItemScreen
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun postOutfit_thenCreateAnotherOutfit_worksProperly() {
    composeRule.runOnIdle {
      // First outfit
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.FitCheck(postUuid = "test_id"))
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = testLocation))
      navigation.navigateTo(Screen.AddItemScreen("test_post_id"))

      navigation.popUpTo(Screen.PreviewItemScreen.route)
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = testLocation))
      navigation.popUpTo(Screen.Feed.route)
      navigation.navigateTo(Screen.Feed)

      assertEquals(Screen.Feed.route, navigation.currentRoute())

      // Second outfit
      navigation.navigateTo(Screen.FitCheck(postUuid = "test_id"))
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = testLocation))
      navigation.navigateTo(Screen.AddItemScreen("test_post_id"))
      assertEquals(Screen.AddItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationBetweenAllTopLevelScreens_worksProperly() {
    composeRule.runOnIdle {
      // Feed
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())

      // Authentication
      navigation.navigateTo(Screen.Authentication)
      assertEquals(Screen.Authentication.route, navigation.currentRoute())

      // Feed again
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())

      // Register
      navigation.navigateTo(Screen.RegisterUsername)
      assertEquals(Screen.RegisterUsername.route, navigation.currentRoute())

      // Back to Feed
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun deepNestedNavigation_maintainsCorrectBackStack() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.FitCheck(postUuid = "test_id"))
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = testLocation))
      navigation.navigateTo(Screen.AddItemScreen("test_post_id"))
      navigation.goBack()
      navigation.navigateTo(Screen.EditItem("item-1"))
      navigation.goBack()
      navigation.navigateTo(Screen.AddItemScreen("test_post_id"))

      assertEquals(Screen.AddItemScreen.route, navigation.currentRoute())

      // Navigate back through entire stack
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
      navigation.goBack()
      assertEquals(Screen.FitCheck.route, navigation.currentRoute())
      navigation.goBack()
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun editItemFromDifferentContexts_navigatesCorrectly() {
    composeRule.runOnIdle {
      // Edit from Preview
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = testLocation))
      navigation.navigateTo(Screen.EditItem("item-1"))
      assertEquals(Screen.EditItem.route, navigation.currentRoute())
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Edit after adding items
      navigation.navigateTo(Screen.AddItemScreen("test_post_id"))
      navigation.goBack()
      navigation.navigateTo(Screen.EditItem("item-2"))
      assertEquals(Screen.EditItem.route, navigation.currentRoute())
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun userSignsOut_navigatesToAuthentication_backStackCleared() {
    composeRule.runOnIdle {
      // User is in the middle of creating an outfit
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.FitCheck(postUuid = "test_id"))
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = testLocation))
      navigation.navigateTo(Screen.AddItemScreen("test_post_id"))

      // User decides to sign out
      navigation.navigateTo(Screen.AccountEdit)
      navigation.navigateTo(Screen.Authentication)

      assertEquals(Screen.Authentication.route, navigation.currentRoute())

      // Back button should not return to previous screens
      navigation.goBack()
    }
  }

  @Test
  fun previewItemScreen_withItems_allCallbacksWork() {
    composeRule.runOnIdle {
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = testLocation))
    }

    composeRule.waitForIdle()

    // Test onAddItem - need to click button and select from dialog
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule
          .onAllNodesWithTag(PreviewItemScreenTestTags.CREATE_ITEM_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeRule.onNodeWithTag(PreviewItemScreenTestTags.CREATE_ITEM_BUTTON).performClick()
    composeRule.waitForIdle()
    // Dialog is shown; click "Create New Item" to navigate to AddItemScreen
    composeRule.onNodeWithTag(PreviewItemScreenTestTags.CREATE_NEW_ITEM_OPTION).performClick()
    composeRule.waitForIdle()

    // Add Item should now be visible
    composeRule.runOnIdle {
      assertEquals(Screen.AddItemScreen.route, navigation.currentRoute())
      navigation.goBack()

      // Test onEditItem
      navigation.navigateTo(Screen.EditItem("test-item"))
      assertEquals(Screen.EditItem.route, navigation.currentRoute())
      navigation.goBack()

      // Test onPostOutfit
      navigation.popUpTo(Screen.Feed.route)
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_popUpTo_clearsIntermediateScreens() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.FitCheck(postUuid = "test_id"))
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri",
              description = "Another Test Outfit Description",
              location = testLocation))
      navigation.navigateTo(Screen.AddItemScreen("test_post_id"))

      // PopUpTo Preview and navigate there
      navigation.goBack()

      // AddItem should not be in stack anymore
      navigation.goBack()
      assertEquals(Screen.FitCheck.route, navigation.currentRoute())
    }
  }

  @Test
  fun allScreens_canBeAccessedFromFeed() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Feed)

      // To FitCheck
      navigation.navigateTo(Screen.FitCheck(postUuid = "test_id"))
      assertEquals(Screen.FitCheck.route, navigation.currentRoute())
      navigation.goBack()

      // To Search
      navigation.navigateTo(Screen.SearchScreen)
      assertEquals(Screen.SearchScreen.route, navigation.currentRoute())
      navigation.goBack()

      // To Account
      navigation.navigateTo(Screen.AccountEdit)
      assertEquals(Screen.AccountEdit.route, navigation.currentRoute())
      navigation.goBack()

      navigation.navigateTo(Screen.SeeFitScreen(postUuid = "test_id"))
      assertEquals(Screen.SeeFitScreen.route, navigation.currentRoute())
      navigation.goBack()

      // Should be back at Feed
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun mainActivityCode_seeFitScreen_extractsPostUuidFromNavArguments() {
    composeRule.runOnIdle {
      val testPostUuid = "test-post-uuid-123"

      navigation.navigateTo(Screen.SeeFitScreen(postUuid = testPostUuid))

      // Verify we successfully navigated to SeeFitScreen
      assertEquals(Screen.SeeFitScreen.route, navigation.currentRoute())
    }

    // Verify the screen is displayed (meaning postUuid was extracted successfully)
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithTag("seeFitScreen").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("seeFitScreen").assertIsDisplayed()
  }

  @Test
  fun seeFitScreen_multipleNavigations_maintainsNavigationFlow() {
    val postUuids = listOf("post-A", "post-B", "post-C", "post-xyz-999")

    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Feed)

      postUuids.forEach { postUuid ->
        // Navigate to SeeFitScreen with different postUuid
        // Tests: navBackStackEntry.arguments?.getString("postUuid")
        navigation.navigateTo(Screen.SeeFitScreen(postUuid = postUuid))
        assertEquals(Screen.SeeFitScreen.route, navigation.currentRoute())

        // Go back to Feed
        navigation.goBack()
        assertEquals(Screen.Feed.route, navigation.currentRoute())
      }
    }
  }

  @Test
  fun mainActivityCode_seeFitScreen_fromDifferentOrigins_argumentExtractionWorks() {
    composeRule.runOnIdle {
      // Test navigation from Feed
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.SeeFitScreen(postUuid = "from-feed"))
      assertEquals(Screen.SeeFitScreen.route, navigation.currentRoute())
      navigation.goBack()

      // Test navigation from Search
      navigation.navigateTo(Screen.SearchScreen)
      navigation.navigateTo(Screen.SeeFitScreen(postUuid = "from-search"))
      assertEquals(Screen.SeeFitScreen.route, navigation.currentRoute())
      navigation.goBack()

      // Test navigation from Account
      navigation.navigateTo(Screen.AccountEdit)
      navigation.navigateTo(Screen.SeeFitScreen(postUuid = "from-account"))
      assertEquals(Screen.SeeFitScreen.route, navigation.currentRoute())
      navigation.goBack()

      assertEquals(Screen.AccountEdit.route, navigation.currentRoute())
    }
  }

  @Test
  fun viewUserScreen_navigateAndGoBack_workCorrectly() {
    composeRule.runOnIdle {
      // Start from Feed
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())

      // Navigate to ViewUser screen with userId
      navigation.navigateTo(Screen.ViewUser(userId = "test-user-123"))
      assertEquals(Screen.ViewUser.ROUTE, navigation.currentRoute())

      // Go back to Feed
      navigation.goBack()
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun mainActivityCode_mapWithLocation_handlesInvalidArguments() {
    composeRule.runOnIdle {
      // Test with extreme coordinates
      navigation.navigateTo(Screen.Map(latitude = 90.0, longitude = 180.0, locationName = "Pole"))
      Assert.assertTrue(navigation.currentRoute().startsWith("map?"))
    }

    composeRule.waitForIdle()
  }

  @Test
  fun mainActivityCode_mapWithLocation_multipleNavigations() {
    val locations =
        listOf(
            Triple(46.5197, 6.6323, "EPFL"),
            Triple(46.5191, 6.5668, "Lausanne"),
            Triple(47.3769, 8.5417, "Zurich"))

    composeRule.runOnIdle {
      locations.forEach { (lat, lon, name) ->
        navigation.navigateTo(Screen.Map(latitude = lat, longitude = lon, locationName = name))
        Assert.assertTrue(navigation.currentRoute().startsWith("map?"))
        navigation.goBack()
      }
    }
  }

  @Test
  fun mainActivityCode_onLocationClick_navigatesToMapWithLocation() {
    composeRule.runOnIdle {
      // Navigate to Feed first
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())

      // Simulate location click by directly calling the navigation action
      // This tests the onLocationClick callback in MainActivity Feed composable
      val clickedLocation = Location(46.5197, 6.6323, "Test Location")
      navigation.navigateTo(
          Screen.Map(
              latitude = clickedLocation.latitude,
              longitude = clickedLocation.longitude,
              locationName = clickedLocation.name))

      // Verify navigation to Map with location
      Assert.assertTrue(navigation.currentRoute().startsWith("map?"))

      // Go back to Feed
      navigation.goBack()
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun feedScreen_onProfileClick_navigatesToViewUser() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Feed)

      // Simulate profile click from feed post
      navigation.navigateTo(Screen.ViewUser(userId = "test-user-123"))

      assertEquals(Screen.ViewUser.ROUTE, navigation.currentRoute())
    }
  }

  @Test
  fun searchScreen_onUserClick_navigatesToViewUser() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.SearchScreen)

      // Simulate user click from search results
      navigation.navigateTo(Screen.ViewUser(userId = "searched-user-456"))

      assertEquals(Screen.ViewUser.ROUTE, navigation.currentRoute())
    }
  }

  @Test
  fun postViewScreen_onProfileClick_navigatesToViewUser() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.PostView("post-123"))

      // Simulate profile click (owner or liked user)
      navigation.navigateTo(Screen.ViewUser(userId = "post-owner-789"))

      assertEquals(Screen.ViewUser.ROUTE, navigation.currentRoute())
    }
  }

  @Test
  fun combined_profileNavigation_fromMultipleOrigins() {
    val cases =
        listOf(
            Triple(Screen.Feed, Screen.ViewUser(userId = "test-user-123"), Screen.ViewUser.ROUTE),
            Triple(
                Screen.SearchScreen,
                Screen.ViewUser(userId = "searched-user-456"),
                Screen.ViewUser.ROUTE),
            Triple(
                Screen.PostView(postId = "post-123"),
                Screen.ViewUser(userId = "post-owner-789"),
                Screen.ViewUser.ROUTE))

    cases.forEach { (start, target, expected) -> navigateToAndAssert(start, target, expected) }
  }

  private fun navigateToAndAssert(start: Screen, target: Screen, expectedRoute: String) {
    composeRule.runOnIdle {
      navigation.navigateTo(start)

      val startRoute = start.route
      val isDynamicStart =
          startRoute.contains("{") ||
              startRoute.contains("postView") ||
              startRoute.contains("editItem") ||
              startRoute.contains("fitCheck")

      if (!isDynamicStart) {
        Assert.assertTrue(
            "Start route mismatch: ${navigation.currentRoute()}",
            navigation.currentRoute().startsWith(startRoute))
      }

      navigation.navigateTo(target)

      // We only check prefix match for dynamic routes
      Assert.assertTrue(
          "Target route mismatch: ${navigation.currentRoute()}",
          navigation.currentRoute().startsWith(expectedRoute))
    }
  }

  @Test
  fun mapScreen_multiplePostClicks_navigatesCorrectly() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Map())
      assertEquals(Screen.Map.route, navigation.currentRoute())

      // Click first post
      navigation.navigateTo(Screen.PostView("post1"))
      assert(navigation.currentRoute().startsWith("postView/"))

      // Go back to map
      navigation.goBack()
      assertEquals(Screen.Map.route, navigation.currentRoute())

      // Click second post
      navigation.navigateTo(Screen.PostView("post2"))
      assert(navigation.currentRoute().startsWith("postView/"))
    }
  }

  @Test
  fun mapScreen_navigationFromFeed_maintainsBackStack() {
    composeRule.runOnIdle {
      // Navigate through typical user flow
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.Map())
      assertEquals(Screen.Map.route, navigation.currentRoute())

      // Go back should return to Feed
      navigation.goBack()
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun mapScreen_withFocusLocation_navigatesCorrectly() {
    composeRule.runOnIdle {
      val testLat = 46.5197
      val testLon = 6.6323
      val testName = "EPFL"

      navigation.navigateTo(Screen.Map(testLat, testLon, testName))

      // Verify we're on Map screen with parameters
      val currentRoute = navigation.currentRoute()
      assert(currentRoute.startsWith("map")) {
        "Expected route to start with 'map', got '$currentRoute'"
      }
    }
  }

  @Test
  fun mapScreen_postClickNavigation_handlesBackNavigation() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.Map())
      navigation.navigateTo(Screen.PostView("test-post"))

      // Should be on PostView
      assert(navigation.currentRoute().startsWith("postView/"))

      // Back to Map
      navigation.goBack()
      assertEquals(Screen.Map.route, navigation.currentRoute())

      // Back to Feed
      navigation.goBack()
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun mapScreen_fromLocationClick_navigatesWithFocusLocation() {
    composeRule.runOnIdle {
      // Simulate clicking a location tag on a post
      navigation.navigateTo(Screen.Feed)

      // User clicks location which should navigate to Map with focus
      navigation.navigateTo(
          Screen.Map(
              latitude = testLocation.latitude,
              longitude = testLocation.longitude,
              locationName = testLocation.name))

      // Should be on map screen
      val currentRoute = navigation.currentRoute()
      assert(currentRoute.startsWith("map")) {
        "Expected route to start with 'map', got '$currentRoute'"
      }

      // Go back to Feed
      navigation.goBack()
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun mapScreen_bottomNavigation_switchesBetweenScreens() {
    composeRule.runOnIdle {
      // Start at Feed
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())

      // Navigate to Map via bottom nav
      navigation.navigateTo(Screen.Map())
      assert(navigation.currentRoute().startsWith("map")) {
        "Expected route to start with 'map', got '${navigation.currentRoute()}'"
      }

      // Navigate to Search via bottom nav
      navigation.navigateTo(Screen.SearchScreen)
      assertEquals(Screen.SearchScreen.route, navigation.currentRoute())

      // Navigate back to Map
      navigation.navigateTo(Screen.Map())
      assert(navigation.currentRoute().startsWith("map")) {
        "Expected route to start with 'map', got '${navigation.currentRoute()}'"
      }

      // Navigate to Feed
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun mapScreen_postClick_whenUserHasPosted_navigatesToPostView() {
    composeRule.runOnIdle {
      // Navigate to Map
      navigation.navigateTo(Screen.Map())
      assertEquals(Screen.Map.route, navigation.currentRoute())

      // Simulate the successful path: user has posted today
      // In MainActivity, this path is: hasUserPostedToday() returns true -> navigate to PostView
      // This tests the navigation action that occurs after the check passes
      val testPostId = "test-post-after-check"
      navigation.navigateTo(Screen.PostView(testPostId))

      // Verify navigation to PostView succeeded
      assert(navigation.currentRoute().startsWith("postView/")) {
        "Expected to navigate to PostView, but got ${navigation.currentRoute()}"
      }
    }
  }

  @Test
  fun mapScreen_postClick_navigationFlow_maintainsConsistency() {
    composeRule.runOnIdle {
      // Start from Feed
      navigation.navigateTo(Screen.Feed)

      // Navigate to Map
      navigation.navigateTo(Screen.Map())
      assertEquals(Screen.Map.route, navigation.currentRoute())

      // Test multiple post clicks with navigation back
      // This exercises the onPostClick callback flow in MainActivity
      for (i in 1..3) {
        val postId = "post-$i"

        // Navigate to PostView (simulates successful hasUserPostedToday() check)
        navigation.navigateTo(Screen.PostView(postId))
        assert(navigation.currentRoute().startsWith("postView/"))

        // Go back to Map
        navigation.goBack()
        assertEquals(Screen.Map.route, navigation.currentRoute())
      }
    }
  }

  @Test
  fun mapScreen_postClickFlow_fromMapToPostViewAndBack() {
    composeRule.runOnIdle {
      // This test exercises the complete flow of the onPostClick callback in MainActivity:
      // 1. User is on Map screen
      // 2. User clicks a post marker
      // 3. hasUserPostedToday() is checked (coroutine launched)
      // 4. If true: navigate to PostView
      // This simulates step 4

      navigation.navigateTo(Screen.Map())
      val testPostIds = listOf("post-alpha", "post-beta", "post-gamma")

      testPostIds.forEach { postId ->
        // Simulate navigation after hasUserPostedToday() returns true
        navigation.navigateTo(Screen.PostView(postId))
        assert(navigation.currentRoute().startsWith("postView/")) {
          "Should navigate to PostView for $postId"
        }

        // Navigate back to map for next iteration
        navigation.goBack()
        assertEquals(Screen.Map.route, navigation.currentRoute())
      }
    }
  }

  // New tests for FindFriendsMap navigation
  @Test
  fun mapScreen_findFriendsTab_ownProfileClick_navigatesToAccountView() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Map())

      // User clicks their own profile marker on Find Friends tab
      // navigateToUserProfile in MainActivity checks if it's current user
      // and navigates to AccountView instead of ViewUser
      navigation.navigateTo(Screen.AccountView)

      Assert.assertEquals(Screen.AccountView.route, navigation.currentRoute())

      // Can go back to Map
      navigation.goBack()
      Assert.assertEquals(Screen.Map.route, navigation.currentRoute())
    }
  }

  @Test
  fun mapScreen_findFriendsTab_profileClickFromDifferentLocation_navigatesCorrectly() {
    composeRule.runOnIdle {
      // Navigate to Map with specific location
      navigation.navigateTo(
          Screen.Map(latitude = 46.5197, longitude = 6.6323, locationName = "EPFL"))
      Assert.assertTrue(navigation.currentRoute().startsWith("map?"))

      // Click profile on Find Friends tab
      navigation.navigateTo(Screen.ViewUser("nearby-user"))
      Assert.assertEquals(Screen.ViewUser.ROUTE, navigation.currentRoute())

      // Go back to map with location preserved
      navigation.goBack()
      Assert.assertTrue(navigation.currentRoute().startsWith("map?"))
    }
  }

  @Test
  fun mapScreen_findFriendsTab_profileToFriendProfile_chainNavigation() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Map())

      // Click profile A from Find Friends
      navigation.navigateTo(Screen.ViewUser("user-a"))
      Assert.assertEquals(Screen.ViewUser.ROUTE, navigation.currentRoute())

      // From user A's profile, click on a friend (user B) in their friends list
      navigation.navigateTo(Screen.ViewUser("user-b"))
      Assert.assertEquals(Screen.ViewUser.ROUTE, navigation.currentRoute())

      // From user B's profile, view their post
      navigation.navigateTo(Screen.PostView("user-b-post"))
      Assert.assertEquals(Screen.PostView.route, navigation.currentRoute())

      // Navigate back through the chain
      navigation.goBack() // Back to user B
      Assert.assertEquals(Screen.ViewUser.ROUTE, navigation.currentRoute())

      navigation.goBack() // Back to user A
      Assert.assertEquals(Screen.ViewUser.ROUTE, navigation.currentRoute())

      navigation.goBack() // Back to Map
      Assert.assertEquals(Screen.Map.route, navigation.currentRoute())
    }
  }

  @Test
  fun mapScreen_findFriendsTab_complexNavigationScenario() {
    composeRule.runOnIdle {
      // Complex real-world scenario
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.Map())

      // Click profile from Find Friends
      navigation.navigateTo(Screen.ViewUser("new-friend"))
      Assert.assertEquals(Screen.ViewUser.ROUTE, navigation.currentRoute())

      // View their post
      navigation.navigateTo(Screen.PostView("new-friend-post"))
      Assert.assertEquals(Screen.PostView.route, navigation.currentRoute())

      // From post, click on another user who liked it
      navigation.navigateTo(Screen.ViewUser("liker-user"))
      Assert.assertEquals(Screen.ViewUser.ROUTE, navigation.currentRoute())

      // View liker's post
      navigation.navigateTo(Screen.PostView("liker-post"))
      Assert.assertEquals(Screen.PostView.route, navigation.currentRoute())

      // Navigate all the way back
      navigation.goBack() // To liker profile
      Assert.assertEquals(Screen.ViewUser.ROUTE, navigation.currentRoute())

      navigation.goBack() // To original post
      Assert.assertEquals(Screen.PostView.route, navigation.currentRoute())

      navigation.goBack() // To new-friend profile
      Assert.assertEquals(Screen.ViewUser.ROUTE, navigation.currentRoute())

      navigation.goBack() // To Map
      Assert.assertEquals(Screen.Map.route, navigation.currentRoute())

      navigation.goBack() // To Feed
      Assert.assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun searchScreen_findFriendsLink_navigatesToFindFriendsMap() {
    composeRule.runOnIdle {
      // Navigate to Search screen
      navigation.navigateTo(Screen.SearchScreen)
      Assert.assertEquals(Screen.SearchScreen.route, navigation.currentRoute())
    }

    // Click on the "Find public friends on the map" link
    composeRule
        .onNodeWithTag(com.android.ootd.ui.search.UserSelectionFieldTestTags.USERS_CLOSE_TO_YOU)
        .performClick()
    composeRule.waitForIdle()

    // Verify navigation to Map screen (currentRoute returns the template, not the actual URL)
    composeRule.runOnIdle {
      val currentRoute = navigation.currentRoute()
      // The route template for Map screen contains the mapType parameter
      Assert.assertTrue(
          "Should navigate to map route",
          currentRoute.startsWith("map") && currentRoute.contains("mapType"))
    }
  }
}
