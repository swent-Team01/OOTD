package com.android.ootd.ui.searchscreen

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.ootd.model.user.UserRepositoryInMemory
import com.android.ootd.ui.search.UserProfileCardTestTags
import com.android.ootd.ui.search.UserSearchScreenPreview
import com.android.ootd.ui.search.UserSelectionFieldTestTags
import org.junit.Rule
import org.junit.Test

class UserSearchScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testGeneralSearch() {
    val firstUsername = UserRepositoryInMemory().nameList[0]

    composeTestRule.setContent { UserSearchScreenPreview() }

    // Input text to trigger dropdown
    composeTestRule
        .onNodeWithTag(UserSelectionFieldTestTags.INPUT_USERNAME)
        .assertIsDisplayed()
        .performTextInput(firstUsername)

    // Wait for dropdown to appear
    composeTestRule.waitForIdle()

    // Verify dropdown contains exactly one item
    composeTestRule
        .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)
        .assertCountEquals(1)

    // Verify the text of the first (and only) suggestion
    composeTestRule
        .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)[0]
        .assertTextEquals(firstUsername)

    // Click on the first suggestion
    composeTestRule
        .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)[0]
        .performClick()

    // Verify the profile card elements are displayed after selection
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.PROFILE_CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USERNAME_TEXT).assertIsDisplayed()
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
}
