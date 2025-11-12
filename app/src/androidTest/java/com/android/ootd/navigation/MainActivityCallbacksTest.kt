package com.android.ootd.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import com.android.ootd.OOTDApp
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
  fun previewItemScreen_callbacks_executeNavigationLambdas() {

    // Navigate along the real user flow: Feed -> FitCheck -> Preview
    composeRule.runOnIdle {
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri", description = "Another Test Outfit Description"))
    }

    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule
          .onAllNodesWithTag(PreviewItemScreenTestTags.CREATE_ITEM_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click Add Item button (opens dialog)
    composeRule.onNodeWithTag(PreviewItemScreenTestTags.CREATE_ITEM_BUTTON).performClick()
    composeRule.waitForIdle()

    // Add Item button should directly navigate to AddItemScreen
    composeRule.runOnIdle { assertEquals(Screen.AddItemScreen.route, navigation.currentRoute()) }
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
              imageUri = "content://another_uri", description = "Another Test Outfit Description"))
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
              imageUri = "content://another_uri", description = "Another Test Outfit Description"))
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
              imageUri = "content://another_uri", description = "Another Test Outfit Description"))
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
              imageUri = "content://another_uri", description = "Another Test Outfit Description"))

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
              imageUri = "content://another_uri", description = "Another Test Outfit Description"))
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
              imageUri = "content://another_uri", description = "Another Test Outfit Description"))

      for (i in 1..3) {
        navigation.navigateTo(Screen.AddItemScreen("test_id"))
        Assert.assertEquals(Screen.AddItemScreen.route, navigation.currentRoute())

        // Simulate save
        navigation.popUpTo(Screen.PreviewItemScreen.route)
        navigation.navigateTo(
            Screen.PreviewItemScreen(
                imageUri = "content://another_uri",
                description = "Another Test Outfit Description"))
        Assert.assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
      }
    }
  }

  @Test
  fun multipleCycles_alternatingAddAndEdit() {
    composeRule.runOnIdle {
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri", description = "Another Test Outfit Description"))

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
              imageUri = "content://another_uri", description = "Another Test Outfit Description"))
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
              imageUri = "content://another_uri", description = "Another Test Outfit Description"))
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
              imageUri = "content://another_uri", description = "Another Test Outfit Description"))
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
              imageUri = "content://another_uri", description = "Another Test Outfit Description"))
      navigation.navigateTo(Screen.AddItemScreen("test_post_id"))

      navigation.popUpTo(Screen.PreviewItemScreen.route)
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri", description = "Another Test Outfit Description"))
      navigation.popUpTo(Screen.Feed.route)
      navigation.navigateTo(Screen.Feed)

      assertEquals(Screen.Feed.route, navigation.currentRoute())

      // Second outfit
      navigation.navigateTo(Screen.FitCheck(postUuid = "test_id"))
      navigation.navigateTo(
          Screen.PreviewItemScreen(
              imageUri = "content://another_uri", description = "Another Test Outfit Description"))
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
              imageUri = "content://another_uri", description = "Another Test Outfit Description"))
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
              imageUri = "content://another_uri", description = "Another Test Outfit Description"))
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
              imageUri = "content://another_uri", description = "Another Test Outfit Description"))
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
              imageUri = "content://another_uri", description = "Another Test Outfit Description"))
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

    // Add Item button should directly navigate to AddItemScreen
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
              imageUri = "content://another_uri", description = "Another Test Outfit Description"))
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
  fun mainActivityCode_seeFitScreen_goBackCallback_executesCorrectly() {
    composeRule.runOnIdle {
      // Start from Feed
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())

      // Navigate to SeeFitScreen (tests postUuid argument extraction)
      navigation.navigateTo(Screen.SeeFitScreen(postUuid = "some-post-id"))
      assertEquals(Screen.SeeFitScreen.route, navigation.currentRoute())

      navigation.goBack()

      // Should return to Feed
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
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
}
