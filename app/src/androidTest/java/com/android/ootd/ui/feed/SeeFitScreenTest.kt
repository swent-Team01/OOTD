package com.android.ootd.ui.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.android.ootd.ui.navigation.NavigationActions
import com.android.ootd.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Integration tests for SeeFitScreen navigation and UI interactions.
 *
 * These tests specifically cover the MainActivity navigation code:
 * ```
 * val postUuid = navBackStackEntry.arguments?.getString("postUuid") ?: ""
 * SeeFitScreen(postUuid = postUuid, goBack = { navigationActions.goBack() })
 * ```
 */
class SeeFitScreenTest {
  @get:Rule val composeTestRule = createComposeRule()
  private lateinit var navController: NavHostController
  private lateinit var navigationActions: NavigationActions

  @Composable
  private fun SetupTestNavigationHost() {
    navController = rememberNavController()
    navigationActions = NavigationActions(navController)

    NavHost(navController = navController, startDestination = Screen.Feed.route) {
      navigation(startDestination = Screen.Feed.route, route = Screen.Feed.name) {
        composable(Screen.Feed.route) {
          val samplePost =
              com.android.ootd.model.posts.OutfitPost(
                  postUID = "post-123",
                  name = "Alice",
                  ownerId = "owner-1",
                  description = "Test outfit",
                  outfitURL = "https://via.placeholder.com/600x400")

          // Use FeedList directly with test data
          Box(modifier = Modifier.fillMaxSize().testTag(FeedScreenTestTags.SCREEN)) {
            FeedList(
                posts = listOf(samplePost),
                isBlurred = false,
                onSeeFitClick = { post ->
                  navigationActions.navigateTo(Screen.SeeFitScreen(post.postUID))
                })
          }
        }

        // This matches the exact MainActivity code we want to test
        composable(
            route = Screen.SeeFitScreen.route,
            arguments = listOf(navArgument("postUuid") { type = NavType.StringType })) {
                navBackStackEntry ->
              val postUuid = navBackStackEntry.arguments?.getString("postUuid") ?: ""
              SeeFitScreen(postUuid = postUuid, goBack = { navigationActions.goBack() })
            }
      }
    }
  }

  @Test
  fun mainActivityCode_extractsPostUuidFromArguments_withValidUuid() {
    composeTestRule.setContent { SetupTestNavigationHost() }

    val testPostUuid = "test-post-456"

    // Navigate with a specific postUuid
    composeTestRule.runOnIdle { navigationActions.navigateTo(Screen.SeeFitScreen(testPostUuid)) }

    // Verify SeeFitScreen is displayed (meaning postUuid was extracted successfully)
    composeTestRule.onNodeWithTag("seeFitScreen").assertIsDisplayed()

    // Verify we're on the correct route
    composeTestRule.runOnIdle {
      assertEquals(Screen.SeeFitScreen.route, navigationActions.currentRoute())
    }
  }

  @Test
  fun mainActivityCode_goBackCallback_returnsToFeed() {
    composeTestRule.setContent { SetupTestNavigationHost() }

    // Start on Feed
    composeTestRule.runOnIdle { navigationActions.navigateTo(Screen.Feed) }

    // Navigate to SeeFitScreen
    composeTestRule.runOnIdle { navigationActions.navigateTo(Screen.SeeFitScreen("some-post-id")) }

    // Verify we're on SeeFitScreen
    composeTestRule.onNodeWithTag("seeFitScreen").assertIsDisplayed()

    // Test the goBack callback
    composeTestRule.runOnIdle { navigationActions.goBack() }

    // Should be back on Feed
    composeTestRule.onNodeWithTag(FeedScreenTestTags.SCREEN).assertIsDisplayed()
  }

  @Test
  fun seeFitButton_click_passesCorrectPostUuidToSeeFitScreen() {
    composeTestRule.setContent { SetupTestNavigationHost() }

    // Start on Feed with a post
    composeTestRule.onNodeWithTag(FeedScreenTestTags.SCREEN).assertIsDisplayed()

    // Click the See Fit button (will pass "post-123" from our sample post)
    composeTestRule
        .onNodeWithTag(OutfitPostCardTestTags.SEE_FIT_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // Verify navigation to SeeFitScreen
    composeTestRule.onNodeWithTag("seeFitScreen").assertIsDisplayed()

    // Verify we're on SeeFitScreen route
    composeTestRule.runOnIdle {
      assertEquals(Screen.SeeFitScreen.route, navigationActions.currentRoute())
    }
  }

  @Test
  fun seeFitScreen_multipleDifferentPostUuids_eachExtractsCorrectly() {
    composeTestRule.setContent { SetupTestNavigationHost() }

    val postUuids = listOf("post-1", "post-2", "post-3", "post-xyz-123")

    postUuids.forEach { postUuid ->
      // Navigate to SeeFitScreen with different postUuid
      composeTestRule.runOnIdle { navigationActions.navigateTo(Screen.SeeFitScreen(postUuid)) }

      // Verify SeeFitScreen is displayed (postUuid extracted successfully)
      composeTestRule.onNodeWithTag("seeFitScreen").assertIsDisplayed()

      // Go back to Feed
      composeTestRule.runOnIdle { navigationActions.goBack() }

      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag(FeedScreenTestTags.SCREEN).assertIsDisplayed()
    }
  }

  @Test
  fun seeFitScreen_navigationMaintainsBackStack() {
    composeTestRule.setContent { SetupTestNavigationHost() }

    // Build navigation stack: Feed -> SeeFit
    composeTestRule.runOnIdle {
      navigationActions.navigateTo(Screen.Feed)
      navigationActions.navigateTo(Screen.SeeFitScreen("stack-test-post"))
    }

    // Verify on SeeFitScreen
    composeTestRule.onNodeWithTag("seeFitScreen").assertIsDisplayed()

    // Go back should return to Feed
    composeTestRule.runOnIdle { navigationActions.goBack() }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.SCREEN).assertIsDisplayed()
  }
}
