package com.android.ootd.ui.searchscreen

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
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
import com.android.ootd.model.account.AccountRepositoryInMemory
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepositoryInMemory
import com.android.ootd.ui.feed.FeedScreen
import com.android.ootd.ui.navigation.NavigationActions
import com.android.ootd.ui.navigation.Screen
import com.android.ootd.ui.search.SearchScreenTestTags.SEARCH_SCREEN
import com.android.ootd.ui.search.UserProfileCardTestTags
import com.android.ootd.ui.search.UserSearchScreen
import com.android.ootd.ui.search.UserSearchScreenPreview
import com.android.ootd.ui.search.UserSearchViewModel
import com.android.ootd.ui.search.UserSelectionFieldTestTags
import com.android.ootd.utils.FirebaseEmulator
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
              onNotificationIconClick = { /* TODO: show user profile page */ })
        }
        composable(Screen.SearchScreen.route) { UserSearchScreen() }
      }
    }
  }

  @Test
  fun testGeneralSearch() = runTest {
    composeTestRule.setContent {
      UserSearchScreen(
          UserSearchViewModel(
              userRepository = userRepository,
              accountRepository = accountRepository,
              notificationRepository = notificationsRepository,
              overrideUser = false))
    }
    val secondUsername = UserRepositoryInMemory().nameList[1]
    val lastUsername = UserRepositoryInMemory().nameList[4]

    accountRepository.addAccount(
        Account(uid = currentUser.uid, username = lastUsername, ownerId = currentUser.uid))
    accountRepository.addAccount(
        Account(uid = currentUser.uid, username = secondUsername, ownerId = currentUser.uid))

    userRepository.addUser(
        User(uid = currentUser.uid, ownerId = currentUser.uid, username = lastUsername))

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
          .size == 1
    }
    // Verify the text of the first (and only) suggestion
    composeTestRule
        .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)[0]
        .assertTextEquals(lastUsername)

    // Click on the first suggestion
    composeTestRule
        .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)[0]
        .performClick()

    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      // Verify dropdown contains exactly one item
      composeTestRule
          .onAllNodesWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON)
          .fetchSemanticsNodes()
          .size == 1
    }

    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON).performClick()

    composeTestRule
        .onNodeWithTag(UserProfileCardTestTags.USERNAME_TEXT, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(UserProfileCardTestTags.AVATAR_LETTER, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun searchScreenNavigation() {
    composeTestRule.setContent { SetupTestNavigationHost() }

    // Navigate programmatically to the Search screen instead of clicking a UI element
    composeTestRule.runOnIdle { navController.navigate(Screen.SearchScreen.route) }

    composeTestRule.onNodeWithTag(SEARCH_SCREEN).assertIsDisplayed()
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
  }

  @Test
  fun testFollowButtonNotLoggedIn() = runTest {
    FirebaseEmulator.auth.signOut()
    val mockViewModel =
        UserSearchViewModel(
            userRepository = UserRepositoryInMemory(),
            accountRepository = AccountRepositoryInMemory(),
            overrideUser = false)

    composeTestRule.setContent { UserSearchScreen(viewModel = mockViewModel) }
    val secondUsername = UserRepositoryInMemory().nameList[1]
    composeTestRule
        .onNodeWithTag(UserSelectionFieldTestTags.INPUT_USERNAME)
        .assertIsDisplayed()
        .performTextInput(secondUsername)

    composeTestRule.waitForIdle()

    composeTestRule
        .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)[0]
        .performClick()

    val exception =
        runCatching {
              composeTestRule
                  .onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON)
                  .performClick()
            }
            .exceptionOrNull()
    FirebaseEmulator.auth.signInAnonymously()
    // There is no logged in user so this should throw an error
    assert(exception is IllegalStateException)
  }

  @Test
  fun testSearchWithMockedAuth() = runTest {
    val userRepositoryInMemory = UserRepositoryInMemory()
    val accountRepositoryInMemory = AccountRepositoryInMemory()

    // Remove and re-add user with Firebase auth UID
    userRepositoryInMemory.deleteUser("user1")
    userRepositoryInMemory.addUser(
        User(uid = FirebaseEmulator.auth.uid ?: "", username = userRepositoryInMemory.nameList[0]))

    // Remove and re-add account with Firebase auth UID
    accountRepositoryInMemory.deleteAccount("user1")
    accountRepositoryInMemory.addAccount(
        Account(
            uid = FirebaseEmulator.auth.uid ?: "",
            ownerId = FirebaseEmulator.auth.uid ?: "",
            username = userRepositoryInMemory.nameList[0],
            friendUids = listOf("user2", "user3")))

    accountRepositoryInMemory.currentUser = FirebaseEmulator.auth.uid ?: ""

    val mockViewModel =
        UserSearchViewModel(
            userRepository = userRepositoryInMemory,
            accountRepository = accountRepositoryInMemory,
            overrideUser = false)

    composeTestRule.setContent { UserSearchScreen(viewModel = mockViewModel) }
    val secondUsername = userRepositoryInMemory.nameList[1]
    composeTestRule
        .onNodeWithTag(UserSelectionFieldTestTags.INPUT_USERNAME)
        .assertIsDisplayed()
        .performTextInput(secondUsername)

    composeTestRule.waitForIdle()

    composeTestRule
        .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)[0]
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    // Wait for follow button to appear
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    // After clicking follow, the button should still exist (may show "Unfollow" or similar)
    // Just verify the button is still present, don't check specific text since it may have changed
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON).assertExists()
  }
}
