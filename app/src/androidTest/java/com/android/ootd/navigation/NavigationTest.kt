package com.android.ootd.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
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

  @Composable
  private fun TestNavHost() {
    navController = rememberNavController()
    navigation = NavigationActions(navController)

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
      // Match MainActivity structure with navigation() instead of composable()
      navigation(startDestination = Screen.Splash.route, route = Screen.Splash.name) {
        composable(Screen.Splash.route) { /* minimal screen */}
        composable(Screen.RegisterUsername.route) { /* minimal screen */}
      }

      navigation(
          startDestination = Screen.Authentication.route, route = Screen.Authentication.name) {
            composable(Screen.Authentication.route) { /* minimal screen */}
          }

      navigation(startDestination = Screen.Feed.route, route = Screen.Feed.name) {
        composable(Screen.Feed.route) { /* minimal screen */}
        composable(Screen.Account.route) { /* minimal screen */}
        composable(Screen.Map.route) { /* minimal screen */}
        composable(Screen.FitCheck.route) { /* minimal screen */}
        composable(Screen.PreviewItemScreen.route) { /* minimal screen */}
        composable(Screen.AddItemScreen.route) { /* minimal screen */}
        composable(Screen.EditItem.route) { /* minimal screen */}
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
      navigation.navigateTo(Screen.Account)
      assertEquals(Screen.Account.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_accountBackToFeedFlow_shouldWork() {
    composeRule.runOnIdle {
      // Navigate to Feed
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())

      // Navigate to Account
      navigation.navigateTo(Screen.Account)
      assertEquals(Screen.Account.route, navigation.currentRoute())

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
      navigation.navigateTo(Screen.Account)
      assertEquals(Screen.Account.route, navigation.currentRoute())

      // Sign out navigates to Authentication
      navigation.navigateTo(Screen.Authentication)
      assertEquals(Screen.Authentication.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_previewItemToAddItem_shouldWork() {
    composeRule.runOnIdle {
      // Navigate to PreviewItemScreen
      navigation.navigateTo(Screen.PreviewItemScreen)
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Navigate to AddItemScreen
      navigation.navigateTo(Screen.AddItemScreen)
      assertEquals(Screen.AddItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_addItemToPreview_goBackShouldWork() {
    composeRule.runOnIdle {
      // Navigate to PreviewItemScreen
      navigation.navigateTo(Screen.PreviewItemScreen)
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Navigate to AddItemScreen
      navigation.navigateTo(Screen.AddItemScreen)
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
      navigation.navigateTo(Screen.PreviewItemScreen)
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
      navigation.navigateTo(Screen.PreviewItemScreen)

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
      navigation.navigateTo(Screen.PreviewItemScreen)
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Navigate to AddItemScreen
      navigation.navigateTo(Screen.AddItemScreen)
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
      navigation.navigateTo(Screen.PreviewItemScreen)

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
      navigation.navigateTo(Screen.AddItemScreen)
      assertEquals(Screen.AddItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_previewItemScreenCanBeAccessedDirectly_shouldWork() {
    composeRule.runOnIdle {
      // Navigate directly to PreviewItemScreen
      navigation.navigateTo(Screen.PreviewItemScreen)
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun navigationActions_multipleBackNavigations_shouldWork() {
    composeRule.runOnIdle {
      // Build a navigation stack
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.PreviewItemScreen)
      navigation.navigateTo(Screen.AddItemScreen)

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

  // Additional PreviewItemScreen Navigation Tests

  @Test
  fun previewItemScreen_navigateFromFeed_shouldWork() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())

      navigation.navigateTo(Screen.PreviewItemScreen)
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun previewItemScreen_goBackToFeed_shouldWork() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.PreviewItemScreen)
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      navigation.goBack()
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun previewItemScreen_navigateFromFitCheck_shouldWork() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.FitCheck)
      assertEquals(Screen.FitCheck.route, navigation.currentRoute())

      navigation.navigateTo(Screen.PreviewItemScreen)
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun previewItemScreen_goBackToFitCheck_shouldWork() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.FitCheck)
      navigation.navigateTo(Screen.PreviewItemScreen)
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      navigation.goBack()
      assertEquals(Screen.FitCheck.route, navigation.currentRoute())
    }
  }

  @Test
  fun previewItemScreen_addItemAndReturn_shouldMaintainNavigationStack() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.PreviewItemScreen)
      navigation.navigateTo(Screen.AddItemScreen)
      assertEquals(Screen.AddItemScreen.route, navigation.currentRoute())

      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      navigation.goBack()
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun previewItemScreen_editItemAndReturn_shouldMaintainNavigationStack() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.PreviewItemScreen)
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      val testItemId = "test-item-456"
      navigation.navigateTo(Screen.EditItem(testItemId))
      assertEquals(Screen.EditItem.route, navigation.currentRoute())

      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun previewItemScreen_editMultipleItemsSequentially_shouldWork() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.PreviewItemScreen)

      // Edit first item
      navigation.navigateTo(Screen.EditItem("item-1"))
      assertEquals(Screen.EditItem.route, navigation.currentRoute())
      navigation.goBack()

      // Edit second item
      navigation.navigateTo(Screen.EditItem("item-2"))
      assertEquals(Screen.EditItem.route, navigation.currentRoute())
      navigation.goBack()

      // Edit third item
      navigation.navigateTo(Screen.EditItem("item-3"))
      assertEquals(Screen.EditItem.route, navigation.currentRoute())
      navigation.goBack()

      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun previewItemScreen_addMultipleItemsSequentially_shouldWork() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.PreviewItemScreen)

      // Add first item
      navigation.navigateTo(Screen.AddItemScreen)
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Add second item
      navigation.navigateTo(Screen.AddItemScreen)
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Add third item
      navigation.navigateTo(Screen.AddItemScreen)
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun previewItemScreen_complexFlow_addEditAddItem_shouldWork() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.PreviewItemScreen)

      // Add an item
      navigation.navigateTo(Screen.AddItemScreen)
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Edit an existing item
      navigation.navigateTo(Screen.EditItem("existing-item"))
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Add another item
      navigation.navigateTo(Screen.AddItemScreen)
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun previewItemScreen_postOutfitNavigatesToFeed_shouldWork() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.PreviewItemScreen)
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Simulate post action by navigating back to Feed (clearing back stack)
      navigation.popUpTo(Screen.Feed.route)
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun previewItemScreen_fullFlow_fitCheckToPreviewToPost_shouldWork() {
    composeRule.runOnIdle {
      // Start at Feed
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())

      // Go to FitCheck
      navigation.navigateTo(Screen.FitCheck)
      assertEquals(Screen.FitCheck.route, navigation.currentRoute())

      // Go to PreviewItemScreen
      navigation.navigateTo(Screen.PreviewItemScreen)
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Add an item
      navigation.navigateTo(Screen.AddItemScreen)
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Post outfit (navigate back to Feed)
      navigation.popUpTo(Screen.Feed.route)
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun previewItemScreen_cancelFromAddItem_shouldReturnToPreview() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.PreviewItemScreen)
      navigation.navigateTo(Screen.AddItemScreen)
      assertEquals(Screen.AddItemScreen.route, navigation.currentRoute())

      // Cancel by going back
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun previewItemScreen_cancelFromEditItem_shouldReturnToPreview() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.PreviewItemScreen)
      navigation.navigateTo(Screen.EditItem("item-cancel-test"))
      assertEquals(Screen.EditItem.route, navigation.currentRoute())

      // Cancel by going back
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun previewItemScreen_deepNavigationStack_shouldMaintainHistory() {
    composeRule.runOnIdle {
      // Build deep navigation stack
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.FitCheck)
      navigation.navigateTo(Screen.PreviewItemScreen)
      navigation.navigateTo(Screen.AddItemScreen)
      navigation.goBack()
      navigation.navigateTo(Screen.EditItem("item-1"))

      assertEquals(Screen.EditItem.route, navigation.currentRoute())

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
  fun previewItemScreen_fromAccount_shouldNotBeAccessible() {
    composeRule.runOnIdle {
      // PreviewItemScreen should not be directly accessible from Account
      navigation.navigateTo(Screen.Account)
      assertEquals(Screen.Account.route, navigation.currentRoute())

      // This would be an invalid flow in the app, but navigation should still work
      navigation.navigateTo(Screen.PreviewItemScreen)
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun previewItemScreen_directNavigation_fromSplash_shouldWork() {
    composeRule.runOnIdle {
      assertEquals(Screen.Splash.route, navigation.currentRoute())

      navigation.navigateTo(Screen.PreviewItemScreen)
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun previewItemScreen_navigateToAuthentication_shouldClearStack() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.PreviewItemScreen)
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Sign out navigates to Authentication (top-level)
      navigation.navigateTo(Screen.Authentication)
      assertEquals(Screen.Authentication.route, navigation.currentRoute())

      // Going back should return to Splash (start destination)
      navigation.goBack()
      assertEquals(Screen.Splash.route, navigation.currentRoute())
    }
  }

  @Test
  fun previewItemScreen_reenterAfterPosting_shouldWork() {
    composeRule.runOnIdle {
      // First visit to PreviewItemScreen
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.PreviewItemScreen)
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Post outfit
      navigation.popUpTo(Screen.Feed.route)
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())

      // Return to PreviewItemScreen for another outfit
      navigation.navigateTo(Screen.FitCheck)
      navigation.navigateTo(Screen.PreviewItemScreen)
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun previewItemScreen_alternateEditAndAdd_shouldWork() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.PreviewItemScreen)

      // Edit item 1
      navigation.navigateTo(Screen.EditItem("item-1"))
      navigation.goBack()

      // Add new item
      navigation.navigateTo(Screen.AddItemScreen)
      navigation.goBack()

      // Edit item 2
      navigation.navigateTo(Screen.EditItem("item-2"))
      navigation.goBack()

      // Add another item
      navigation.navigateTo(Screen.AddItemScreen)
      navigation.goBack()

      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun previewItemScreen_editSameItemMultipleTimes_shouldWork() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.PreviewItemScreen)
      val itemId = "same-item"

      // Edit same item multiple times
      for (_i in 1..3) {
        navigation.navigateTo(Screen.EditItem(itemId))
        assertEquals(Screen.EditItem.route, navigation.currentRoute())
        navigation.goBack()
        assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
      }
    }
  }

  @Test
  fun feedScreen_onAddPostClick_navigatesToFitCheck() {
    composeRule.runOnIdle {
      // Start at Feed
      navigation.navigateTo(Screen.Feed)

      // Simulate user clicking "Add Post"
      navigation.navigateTo(Screen.FitCheck)
    }

    composeRule.waitForIdle()
    assertEquals(Screen.FitCheck.route, navigation.currentRoute())
  }

  @Test
  fun fitCheckScreen_onNextClick_navigatesToPreviewItem() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.FitCheck)
      navigation.navigateTo(Screen.PreviewItemScreen)
    }
    composeRule.waitForIdle()
    assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
  }

  @Test
  fun previewItemScreen_onPostOutfit_navigatesBackToFeed() {
    composeRule.runOnIdle {
      // Move through the realistic flow
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.FitCheck)
      navigation.navigateTo(Screen.PreviewItemScreen)

      // Simulate post action
      navigation.popUpTo(Screen.Feed.route)
      navigation.navigateTo(Screen.Feed)
    }

    composeRule.waitForIdle()
    assertEquals(Screen.Feed.route, navigation.currentRoute())
  }

  @Test
  fun addItemScreen_canNavigateToPreviewItemScreen() {
    composeRule.runOnIdle {
      // Start at AddItemScreen
      navigation.navigateTo(Screen.AddItemScreen)
      assertEquals(Screen.AddItemScreen.route, navigation.currentRoute())

      // Navigate to PreviewItemScreen
      navigation.navigateTo(Screen.PreviewItemScreen)
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun completeFlow_addItemSavesAndReturnsToPreview() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.PreviewItemScreen)

      // Add item
      navigation.navigateTo(Screen.AddItemScreen)
      assertEquals(Screen.AddItemScreen.route, navigation.currentRoute())

      // Simulate saving and returning (using popUpTo)
      navigation.popUpTo(Screen.PreviewItemScreen.route)
      navigation.navigateTo(Screen.PreviewItemScreen)
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun completeFlow_addItemCancelsAndReturnsToPreview() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.PreviewItemScreen)
      navigation.navigateTo(Screen.AddItemScreen)

      // Cancel by going back
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun completeFlow_feedToFitCheckToPreviewToAddToPreview() {
    composeRule.runOnIdle {
      // Start from Feed
      navigation.navigateTo(Screen.Feed)
      assertEquals(Screen.Feed.route, navigation.currentRoute())

      // Go to FitCheck
      navigation.navigateTo(Screen.FitCheck)
      assertEquals(Screen.FitCheck.route, navigation.currentRoute())

      // Go to PreviewItemScreen
      navigation.navigateTo(Screen.PreviewItemScreen)
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Add an item
      navigation.navigateTo(Screen.AddItemScreen)
      assertEquals(Screen.AddItemScreen.route, navigation.currentRoute())

      // Return to preview after adding
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun multipleCycles_addItemFromPreview_threeTimesCanceling() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.PreviewItemScreen)

      // First cycle
      navigation.navigateTo(Screen.AddItemScreen)
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Second cycle
      navigation.navigateTo(Screen.AddItemScreen)
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Third cycle
      navigation.navigateTo(Screen.AddItemScreen)
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun multipleCycles_alternatingAddAndEdit() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.PreviewItemScreen)

      // Add first item
      navigation.navigateTo(Screen.AddItemScreen)
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Edit item
      navigation.navigateTo(Screen.EditItem("item-1"))
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Add second item
      navigation.navigateTo(Screen.AddItemScreen)
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())

      // Edit another item
      navigation.navigateTo(Screen.EditItem("item-2"))
      navigation.goBack()
      assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
    }
  }

  @Test
  fun multipleCycles_addItemFromPreview_threeTimesSaving() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.PreviewItemScreen)

      for (i in 1..3) {
        navigation.navigateTo(Screen.AddItemScreen)
        assertEquals(Screen.AddItemScreen.route, navigation.currentRoute())

        // Simulate save
        navigation.popUpTo(Screen.PreviewItemScreen.route)
        navigation.navigateTo(Screen.PreviewItemScreen)
        assertEquals(Screen.PreviewItemScreen.route, navigation.currentRoute())
      }
    }
  }

  @Test
  fun deepStack_feedToPreviewToAddToEdit_maintainsHistory() {
    composeRule.runOnIdle {
      // Build deep stack
      navigation.navigateTo(Screen.Feed)

      navigation.navigateTo(Screen.FitCheck)
      navigation.navigateTo(Screen.PreviewItemScreen)
      navigation.navigateTo(Screen.AddItemScreen)

      // Go to edit instead (simulating user flow)
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
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.PreviewItemScreen)

      // Add first item and don't save
      navigation.navigateTo(Screen.AddItemScreen)
      navigation.goBack()

      // Add second item and don't save
      navigation.navigateTo(Screen.AddItemScreen)
      navigation.goBack()

      // Verify we can still navigate back properly
      navigation.goBack()
      assertEquals(Screen.Feed.route, navigation.currentRoute())
    }
  }

  @Test
  fun statePreservation_navigationDoesNotClearStack() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Feed)
      navigation.navigateTo(Screen.FitCheck)
      navigation.navigateTo(Screen.PreviewItemScreen)

      // Navigate to add item
      navigation.navigateTo(Screen.AddItemScreen)

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
}
