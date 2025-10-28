package com.android.ootd.ui.searchscreen

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
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
import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepositoryInMemory
import com.android.ootd.model.user.User
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
              onAddPostClick = { /* TODO: handle add post */}, // this will go to AddItemScreen
              onSearchClick = { navigationActions.navigateTo(Screen.SearchScreen) },
              onAccountIconClick = { /* TODO: show user profile page */})
        }
        composable(Screen.SearchScreen.route) {
          UserSearchScreen(onBack = { navigationActions.goBack() })
        }
      }
    }
  }

  @Test
  fun testGeneralSearch() {
    composeTestRule.setContent { UserSearchScreenPreview() }
    val secondUsername = UserRepositoryInMemory().nameList[1]
    val lastUsername = UserRepositoryInMemory().nameList[4]
    // Input text to trigger dropdown
    composeTestRule
        .onNodeWithTag(UserSelectionFieldTestTags.INPUT_USERNAME)
        .assertIsDisplayed()
        .performTextInput(secondUsername)

    // Wait for dropdown to appear
    composeTestRule.waitForIdle()

    // Verify dropdown contains exactly one item
    composeTestRule
        .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)
        .assertCountEquals(1)

    // Verify the text of the first (and only) suggestion
    composeTestRule
        .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)[0]
        .assertTextEquals(secondUsername)

    // Click on the first suggestion
    composeTestRule
        .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)[0]
        .performClick()

    // Verify the profile card elements are displayed after selection
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.PROFILE_CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USERNAME_TEXT).assertIsDisplayed()

    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON)
        .assertTextContains("Follow", substring = true)

    composeTestRule
        .onNodeWithTag(UserSelectionFieldTestTags.INPUT_USERNAME)
        .assertIsDisplayed()
        .performTextClearance()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(UserSelectionFieldTestTags.INPUT_USERNAME)
        .assertIsDisplayed()
        .performTextInput(lastUsername)
    composeTestRule.waitForIdle()
    composeTestRule
        .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)[0]
        .performClick()
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON)
        .assertTextContains("Unfollow", substring = true)
  }

  @Test
  fun searchScreenNavigation() {
    composeTestRule.setContent { SetupTestNavigationHost() }
    composeTestRule.onNodeWithTag(NAVIGATE_TO_SEARCH_SCREEN).performClick()

    composeTestRule.onNodeWithTag(SEARCH_SCREEN).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(SearchScreenTestTags.GO_BACK_BUTTON)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.SCREEN).assertIsDisplayed()
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

    composeTestRule.setContent { UserSearchScreen(viewModel = mockViewModel, onBack = {}) }
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

    composeTestRule.setContent { UserSearchScreen(viewModel = mockViewModel, onBack = {}) }
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

    composeTestRule
        .onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON)
        .assertIsDisplayed()
        .assertTextContains("Follow", substring = true)
  }
}
