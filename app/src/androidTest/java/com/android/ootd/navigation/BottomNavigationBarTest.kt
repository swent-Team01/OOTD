package com.android.ootd.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.android.ootd.ui.account.UiTestTags
import com.android.ootd.ui.navigation.NavigationTestTags
import com.android.ootd.ui.navigation.Screen
import com.android.ootd.ui.search.SearchScreenTestTags
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class BottomNavigationBarTest {

  @get:Rule val composeRule = createComposeRule()

  private lateinit var navController: NavHostController

  @Before
  fun setUp() {
    // Start the app at the top-level Feed graph so the bottom bar is visible and routing is valid.
    composeRule.setContent {
      val nc = rememberNavController()
      navController = nc
      OOTDAppHarness(testNavController = nc, startRoute = Screen.Feed.name)
    }
    composeRule.waitForIdle()
  }

  @Composable
  private fun OOTDAppHarness(testNavController: NavHostController, startRoute: String) {
    com.android.ootd.OOTDApp(
        testNavController = testNavController, testStartDestination = startRoute)
  }

  @Test
  fun bottomBar_isVisibleOnFeed_andTabsExist() {
    composeRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertIsDisplayed()
    composeRule
        .onNodeWithTag(NavigationTestTags.getTabTestTag(com.android.ootd.ui.navigation.Tab.Feed))
        .assertIsDisplayed()
    composeRule
        .onNodeWithTag(NavigationTestTags.getTabTestTag(com.android.ootd.ui.navigation.Tab.Search))
        .assertIsDisplayed()
    composeRule
        .onNodeWithTag(
            NavigationTestTags.getTabTestTag(com.android.ootd.ui.navigation.Tab.Inventory))
        .assertIsDisplayed()
    composeRule
        .onNodeWithTag(NavigationTestTags.getTabTestTag(com.android.ootd.ui.navigation.Tab.Profile))
        .assertIsDisplayed()
  }

  @Test
  fun bottomBar_clickInventory_navigatesToInventoryScreen_andRouteUpdates() {
    composeRule
        .onNodeWithTag(
            NavigationTestTags.getTabTestTag(com.android.ootd.ui.navigation.Tab.Inventory))
        .performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("inventoryScreen").assertIsDisplayed()
    composeRule.runOnIdle {
      assertEquals(Screen.InventoryScreen.route, navController.currentDestination?.route)
    }
  }

  @Test
  fun bottomBar_clickProfile_navigatesToAccount() {
    composeRule
        .onNodeWithTag(NavigationTestTags.getTabTestTag(com.android.ootd.ui.navigation.Tab.Profile))
        .performClick()
    composeRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_TITLE).assertIsDisplayed()
  }

  @Test
  fun bottomBar_clickSearch_navigatesToSearch() {
    composeRule
        .onNodeWithTag(NavigationTestTags.getTabTestTag(com.android.ootd.ui.navigation.Tab.Search))
        .performClick()
    composeRule.waitForIdle()
    // Assert route and UI tag to ensure we're on Search, not Feed
    composeRule.runOnIdle {
      assertEquals(Screen.SearchScreen.route, navController.currentDestination?.route)
    }
    composeRule.onNodeWithTag(SearchScreenTestTags.SEARCH_SCREEN).assertIsDisplayed()
  }

  @Test
  fun bottomBar_hiddenOnFitCheck() {
    composeRule.runOnIdle { navController.navigate(Screen.FitCheck.route) }
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU).assertDoesNotExist()
  }
}
