package com.android.ootd.ui.post

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.ootd.model.user.User
import com.android.ootd.ui.theme.OOTDTheme
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LikedUsersRowTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun likedUsersRow_rendersUser_and_callsOnProfileClick() {
    var clickedUserId: String? = null

    val likedUser = User(uid = "liked-user-1", username = "Liked User", profilePicture = "")

    composeRule.setContent {
      OOTDTheme {
        LikedUsersRow(likedUsers = listOf(likedUser), onProfileClick = { clickedUserId = it })
      }
    }

    composeRule.onNodeWithText("Liked User").assertIsDisplayed()

    composeRule.onNodeWithText("Liked User").performClick()

    assertEquals("liked-user-1", clickedUserId)
  }
}
