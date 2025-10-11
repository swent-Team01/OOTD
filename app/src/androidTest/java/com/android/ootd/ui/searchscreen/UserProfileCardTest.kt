package com.android.ootd.ui.searchscreen

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.android.ootd.model.user.User
import com.android.ootd.ui.search.UserProfileCard
import com.android.ootd.ui.search.UserProfileCardPreview
import com.android.ootd.ui.search.UserProfileCardTestTags
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

  @Test
  fun followButtonAlwaysAppears() {
    composeTestRule.setContent {
      UserProfileCard(
          selectedUser = User(uid = uid, name = name, friendList = emptyList()),
          modifier = Modifier.padding(16.dp),
          isSelectedUserFollowed = false,
          onFollowClick = {})
    }
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.PROFILE_CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USERNAME_TEXT).assertIsDisplayed()
  }

  @Test
  fun seeCardPreview() {
    composeTestRule.setContent { UserProfileCardPreview() }
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.PROFILE_CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USERNAME_TEXT).assertIsDisplayed()
  }

  @Test
  fun onClickIsCalled() {
    var clickCount = 0
    composeTestRule.setContent {
      UserProfileCard(
          selectedUser = User(uid = uid, name = name, friendList = emptyList()),
          modifier = Modifier.padding(16.dp),
          isSelectedUserFollowed = false,
          onFollowClick = { clickCount = clickCount + 1 })
    }
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON).performClick()
    assert(clickCount == 1)
  }

  @Test
  fun onClickIsNotCalledWhenUserIsNull() {
    var clickCount = 0
    composeTestRule.setContent {
      UserProfileCard(
          selectedUser = null, // Pass null here
          modifier = Modifier.padding(16.dp),
          isSelectedUserFollowed = false,
          onFollowClick = { clickCount++ })
    }
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON).performClick()
    assert(clickCount == 0) // Should not be called when user is null
  }
}
