package com.android.ootd.ui.searchscreen

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.android.ootd.model.account.AccountRepositoryInMemory
import com.android.ootd.model.user.UserRepositoryInMemory
import com.android.ootd.ui.feed.FeedScreen
import com.android.ootd.ui.feed.FeedScreenTestTags
import com.android.ootd.ui.feed.FeedScreenTestTags.NAVIGATE_TO_SEARCH_SCREEN
import com.android.ootd.ui.navigation.NavigationActions
import com.android.ootd.ui.navigation.Screen
import com.android.ootd.ui.search.SearchScreenTestTags
import com.android.ootd.ui.search.SearchScreenTestTags.SEARCH_SCREEN
import com.android.ootd.ui.search.UserProfileCardTestTags
import com.android.ootd.ui.search.UserSearchScreen
import com.android.ootd.ui.search.UserSearchScreenPreview
import com.android.ootd.ui.search.UserSearchViewModel
import com.android.ootd.ui.search.UserSelectionFieldTestTags
import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class UserSearchScreenTest : FirestoreTest() {
  @get:Rule val composeTestRule = createComposeRule()
  private lateinit var navController: NavHostController
  private lateinit var navigationActions: NavigationActions
  private val nameList = UserRepositoryInMemory().nameList

  @Composable
  private fun SetupTestNavigationHost() {
    navController = rememberNavController()
    navigationActions = NavigationActions(navController)

    NavHost(navController = navController, startDestination = Screen.Feed.route) {
      navigation(startDestination = Screen.Feed.route, route = Screen.Feed.name) {
        composable(Screen.Feed.route) {
          FeedScreen(
              onAddPostClick = {},
              onSearchClick = { navigationActions.navigateTo(Screen.SearchScreen) },
              onNotificationIconClick = {})
        }
        composable(Screen.SearchScreen.route) {
          UserSearchScreen(onBack = { navigationActions.goBack() })
        }
      }
    }
  }

  // --- Tiny helpers ---
  private fun searchAndSelect(username: String) {
    composeTestRule
        .onNodeWithTag(UserSelectionFieldTestTags.INPUT_USERNAME)
        .assertIsDisplayed()
        .performTextInput(username)
    composeTestRule.waitForIdle()
    composeTestRule
        .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)[0]
        .performClick()
    composeTestRule.waitForIdle()
  }

  private fun assertFollowButtonText(text: String) {
    composeTestRule
        .onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON)
        .assertIsDisplayed()
        .assertTextContains(text, substring = true)
  }

  @Test
  fun searchFlow_endToEnd_multipleUsers_followUnfollow_noResults_navigation() {
    composeTestRule.setContent { UserSearchScreenPreview() }

    // Search and select second user
    val secondUsername = nameList[1]
    searchAndSelect(secondUsername)

    // Verify suggestion and profile card displayed
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.PROFILE_CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USERNAME_TEXT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON).assertIsDisplayed()

    // Follow user (toggle from initial state to "Follow")
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON).performClick()
    composeTestRule.waitForIdle()
    assertFollowButtonText("Follow")

    // Clear and search for another user
    composeTestRule.onNodeWithTag(UserSelectionFieldTestTags.INPUT_USERNAME).performTextClearance()
    composeTestRule.waitForIdle()

    val lastUsername = nameList[4]
    searchAndSelect(lastUsername)

    // Toggle follow for this user (should show "Unfollow")
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON).performClick()
    composeTestRule.waitForIdle()
    assertFollowButtonText("Unfollow")

    // Test no results scenario
    composeTestRule.onNodeWithTag(UserSelectionFieldTestTags.INPUT_USERNAME).performTextClearance()
    composeTestRule
        .onNodeWithTag(UserSelectionFieldTestTags.INPUT_USERNAME)
        .performTextInput("xvhardcoded")
    composeTestRule.waitForIdle()
    composeTestRule
        .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)
        .assertCountEquals(0)
    composeTestRule
        .onAllNodesWithTag(UserSelectionFieldTestTags.NO_RESULTS_MESSAGE)
        .assertCountEquals(1)
  }

  @Test
  fun navigation_fromFeedToSearchAndBack() {
    composeTestRule.setContent { SetupTestNavigationHost() }

    // Navigate to search screen
    composeTestRule.onNodeWithTag(NAVIGATE_TO_SEARCH_SCREEN).performClick()
    composeTestRule.onNodeWithTag(SEARCH_SCREEN).assertIsDisplayed()

    // Navigate back to feed
    composeTestRule
        .onNodeWithTag(SearchScreenTestTags.GO_BACK_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.SCREEN).assertIsDisplayed()
  }

  @Test
  fun authentication_notLoggedIn_throwsError() = runTest {
    FirebaseEmulator.auth.signOut()
    val mockViewModel =
        UserSearchViewModel(
            userRepository = UserRepositoryInMemory(),
            accountRepository = AccountRepositoryInMemory(),
            overrideUser = false)

    composeTestRule.setContent { UserSearchScreen(viewModel = mockViewModel, onBack = {}) }

    // Input text to trigger dropdown
    composeTestRule
        .onNodeWithTag(UserSelectionFieldTestTags.INPUT_USERNAME)
        .assertIsDisplayed()
        .performTextInput(nameList[1])
    composeTestRule.waitForIdle()

    // Clicking on suggestion should throw error when not logged in (selectUsername checks auth)
    val exception =
        runCatching {
              composeTestRule
                  .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)[0]
                  .performClick()
              composeTestRule.waitForIdle()
            }
            .exceptionOrNull()

    // Should throw IllegalStateException when not logged in
    assert(exception is IllegalStateException)

    // Re-authenticate for subsequent tests
    FirebaseEmulator.auth.signInAnonymously().await()
    kotlinx.coroutines.delay(500)
  }

  @Test
  fun authentication_withMockedAuth_followButtonWorks() = runTest {
    // Ensure we're signed in
    if (FirebaseEmulator.auth.currentUser == null) {
      FirebaseEmulator.auth.signInAnonymously().await()
      kotlinx.coroutines.delay(500)
    }

    // Test with mocked auth using overrideUser = true
    val userRepo = UserRepositoryInMemory()
    val accountRepo = AccountRepositoryInMemory()

    val authViewModel =
        UserSearchViewModel(
            userRepository = userRepo,
            accountRepository = accountRepo,
            overrideUser = true) // Use true to avoid Firebase auth checks

    composeTestRule.setContent { UserSearchScreen(viewModel = authViewModel, onBack = {}) }
    searchAndSelect(userRepo.nameList[1])

    composeTestRule
        .onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.waitForIdle()
    assertFollowButtonText("Follow")
  }
}
