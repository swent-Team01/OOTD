package com.android.ootd.ui.searchscreen

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.android.ootd.model.account.Account
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepositoryInMemory
import com.android.ootd.ui.feed.FeedScreen
import com.android.ootd.ui.navigation.NavigationActions
import com.android.ootd.ui.navigation.Screen
import com.android.ootd.ui.search.UserSearchScreen
import com.android.ootd.ui.search.UserSearchScreenPreview
import com.android.ootd.ui.search.UserSearchViewModel
import com.android.ootd.ui.search.UserSelectionFieldTestTags
import com.android.ootd.utils.FirestoreTest
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class UserSearchScreenTest : FirestoreTest() {
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
          FeedScreen(
              onAddPostClick = { /* TODO: handle add post */ }, // this will go to AddItemScreen
              onNotificationIconClick = { /* TODO: show user profile page */ },
          )
        }
        composable(Screen.SearchScreen.route) { UserSearchScreen() }
      }
    }
  }

  @Test
  fun testGeneralSearch() = runTest {
    composeTestRule.setContent {
      UserSearchScreen(UserSearchViewModel(userRepository = userRepository))
    }
    val secondUsername = UserRepositoryInMemory().nameList[1]
    val lastUsername = UserRepositoryInMemory().nameList[4]

    accountRepository.addAccount(
        Account(uid = currentUser.uid, username = lastUsername, ownerId = currentUser.uid))
    accountRepository.addAccount(
        Account(uid = "test_uid", username = lastUsername + "suffix", ownerId = currentUser.uid))

    userRepository.addUser(
        User(uid = currentUser.uid, ownerId = currentUser.uid, username = lastUsername))

    userRepository.addUser(
        User(uid = "test_uid", ownerId = currentUser.uid, username = lastUsername + "suffix"))

    // Assure title is displayed correctly
    composeTestRule.onNodeWithTag(UserSelectionFieldTestTags.FIND_FRIENDS_TITLE).assertIsDisplayed()
    // No query so should be default message
    composeTestRule.onNodeWithTag(UserSelectionFieldTestTags.EMPTY_SEARCH_STATE).assertIsDisplayed()

    composeTestRule.onNodeWithTag(UserSelectionFieldTestTags.USERS_CLOSE_TO_YOU).assertIsDisplayed()

    // Input text to trigger dropdown
    composeTestRule
        .onNodeWithTag(UserSelectionFieldTestTags.INPUT_USERNAME)
        .assertIsDisplayed()
        .performTextInput(lastUsername)

    // Wait for dropdown to appear
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      // Verify dropdown contains exactly one item
      composeTestRule
          .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)
          .fetchSemanticsNodes()
          .size == 2 // Make sure that multiple suggestions are rendered
    }
    // Verify the text of the first suggestion
    composeTestRule
        .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)[0]
        .assertTextContains(lastUsername)

    composeTestRule
        .onNodeWithTag(UserSelectionFieldTestTags.USERS_CLOSE_TO_YOU)
        .assertIsNotDisplayed()

    // Click on the first suggestion
    composeTestRule
        .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)[0]
        .performClick()
  }

  @Test
  fun searchScreenNavigation() {
    composeTestRule.setContent { SetupTestNavigationHost() }

    // Navigate programmatically to the Search screen instead of clicking a UI element
    composeTestRule.runOnIdle { navController.navigate(Screen.SearchScreen.route) }

    composeTestRule.onNodeWithTag(UserSelectionFieldTestTags.FIND_FRIENDS_TITLE).assertIsDisplayed()
  }

  @Test
  fun testSearchWithNoResults() {
    composeTestRule.setContent { UserSearchScreenPreview() }

    // Input text to trigger dropdown
    composeTestRule
        .onNodeWithTag(UserSelectionFieldTestTags.INPUT_USERNAME)
        .assertIsDisplayed()
        .performTextInput("xvhardcoded")

    // Wait for dropdown to appear
    composeTestRule.waitForIdle()

    // Verify dropdown contains exactly one item
    composeTestRule
        .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)
        .assertCountEquals(0)
    composeTestRule
        .onAllNodesWithTag(UserSelectionFieldTestTags.NO_RESULTS_MESSAGE)
        .assertCountEquals(1)

    composeTestRule.onNodeWithTag(UserSelectionFieldTestTags.NO_RESULTS_ICON).assertIsDisplayed()
  }

  @Test
  fun testFindFriendsLinkNavigatesToMap() {
    var findFriendsClicked = false
    composeTestRule.setContent {
      UserSearchScreen(
          viewModel = UserSearchViewModel(userRepository = userRepository),
          onFindFriendsClick = { findFriendsClicked = true })
    }

    // Click on the "Find public friends on the map" link
    composeTestRule
        .onNodeWithTag(UserSelectionFieldTestTags.USERS_CLOSE_TO_YOU)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    // Verify the callback was invoked
    assert(findFriendsClicked) { "onFindFriendsClick callback should have been called" }
  }
}
