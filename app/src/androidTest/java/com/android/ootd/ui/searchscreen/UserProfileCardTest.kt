package com.android.ootd.ui.searchscreen

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.android.ootd.model.user.User
import com.android.ootd.ui.search.UserProfileCard
import com.android.ootd.ui.search.UserProfileCardPreview
import com.android.ootd.ui.search.UserProfileCardTestTags
import com.android.ootd.ui.search.UserProfileCardWithErrorPreview
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class UserProfileCardTest(private val uid: String, private val name: String) {
  @get:Rule val composeTestRule = createComposeRule()

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "uid={0}, name={1}")
    fun data(): Collection<Array<Any>> {
      return listOf(
          arrayOf("Bob", "Michael"),
          arrayOf("RandUID", "TheMostSuperNameofTheWorldTheThirdKingOfPeople"))
    }
  }

  fun assertCardIsDisplayed(composeTestRule: ComposeContentTestRule) {
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.PROFILE_CARD).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(UserProfileCardTestTags.USERNAME_TEXT, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  fun setCustomProfileCard(
      composeTestRule: ComposeContentTestRule,
      onFollowClick: () -> Unit,
      onUserClick: (String) -> Unit,
      selectedUser: User? = null
  ) {
    composeTestRule.setContent {
      UserProfileCard(
          selectedUser = selectedUser,
          modifier = Modifier.padding(16.dp),
          isSelectedUserFollowed = false,
          hasRequestPending = false,
          onErrorDismiss = {},
          errorMessage = null,
          onUserClick = onUserClick,
          onFollowClick = onFollowClick)
    }
  }

  @Test
  fun followButtonAlwaysAppears() {
    setCustomProfileCard(composeTestRule, {}, {}, User(uid = uid, username = name))
    assertCardIsDisplayed(composeTestRule)
  }

  @Test
  fun seeCardPreview() {
    composeTestRule.setContent { UserProfileCardPreview() }
    assertCardIsDisplayed(composeTestRule)
  }

  @Test
  fun seeCardErrorPreview() {
    composeTestRule.setContent { UserProfileCardWithErrorPreview() }
    assertCardIsDisplayed(composeTestRule)
  }

  @Test
  fun onClickIsCalled() {
    var clickCount = 0
    var clickUser = 0
    setCustomProfileCard(
        composeTestRule, { clickCount += 1 }, { clickUser += 1 }, User(uid = uid, username = name))
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(UserProfileCardTestTags.USERNAME_TEXT, useUnmergedTree = true)
        .performClick()
    composeTestRule
        .onNodeWithTag(UserProfileCardTestTags.AVATAR_LETTER, useUnmergedTree = true)
        .performClick()
    assert(clickUser == 2)
    assert(clickCount == 1)
  }

  @Test
  fun onClickIsNotCalledWhenUserIsNull() {
    var clickCount = 0
    setCustomProfileCard(composeTestRule, { clickCount++ }, { clickCount++ }, null)
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON).performClick()
    composeTestRule
        .onNodeWithTag(UserProfileCardTestTags.USERNAME_TEXT, useUnmergedTree = true)
        .performClick()
    composeTestRule
        .onNodeWithTag(UserProfileCardTestTags.AVATAR_LETTER, useUnmergedTree = true)
        .assertDoesNotExist()

    assert(clickCount == 0) // Should not be called when user is null
  }
}
