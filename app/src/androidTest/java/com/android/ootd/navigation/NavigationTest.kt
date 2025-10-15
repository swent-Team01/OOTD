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

      // Should be back at Authentication (the start destination)
      assertEquals(Screen.Authentication.route, navigation.currentRoute())
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
}
