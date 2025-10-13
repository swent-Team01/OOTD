package com.android.ootd.ui.searchscreen

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
import com.android.ootd.model.user.UserRepositoryInMemory
import com.android.ootd.ui.search.UserProfileCardTestTags
import com.android.ootd.ui.search.UserSearchScreen
import com.android.ootd.ui.search.UserSearchScreenPreview
import com.android.ootd.ui.search.UserSearchViewModel
import com.android.ootd.ui.search.UserSelectionFieldTestTags
import com.android.ootd.utils.FirestoreTest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Rule
import org.junit.Test

class UserSearchScreenTest : FirestoreTest() {
  @get:Rule val composeTestRule = createComposeRule()

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
  fun testFollowButtonNotLoggedIn() {
    val mockFirebaseAuth = mockk<FirebaseAuth>(relaxed = true)
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockFirebaseAuth
    every { mockFirebaseAuth.currentUser } returns null

    val mockViewModel =
        UserSearchViewModel(userRepository = UserRepositoryInMemory(), overrideUser = false)

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
    // There is no logged in user so this should throw an error
    assert(exception is IllegalStateException)
  }

  @Test
  fun testSearchWithMockedAuth() {
    val mockFirebaseAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockFirebaseUser = mockk<FirebaseUser>(relaxed = true)

    every { mockFirebaseUser.uid } returns "user1"
    mockkStatic(FirebaseAuth::class)

    every { FirebaseAuth.getInstance() } returns mockFirebaseAuth
    every { mockFirebaseAuth.currentUser } returns mockFirebaseUser

    val mockViewModel =
        UserSearchViewModel(userRepository = UserRepositoryInMemory(), overrideUser = false)

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

    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON).performClick()

    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON)
        .assertTextContains("Follow", substring = true)
  }
}
