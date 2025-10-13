package com.android.ootd.ui.account

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.ui.theme.OOTDTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun accountScreen_showsAllMainElements_and_avatarConditionally() {
    val avatarState = mutableStateOf<String?>(null)

    // Single setContent for the whole test; content reads from avatarState.value
    composeTestRule.setContent {
      OOTDTheme {
        AccountScreen(username = "user1", email = "user1@google.com", avatarUri = avatarState.value)
      }
    }

    // Wait for compose to settle
    composeTestRule.waitForIdle()

    // Assert main elements are displayed and avatar image does not exist initially
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_BACK).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_CONTAINER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_IMAGE).assertDoesNotExist()
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_EDIT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_USERNAME_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_USERNAME_CLEAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_GOOGLE_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_SIGNOUT_BUTTON).assertIsDisplayed()

    // Update the avatar state on the UI thread and wait for compose to update
    composeTestRule.runOnUiThread { avatarState.value = "https://example.com/avatar.jpg" }
    composeTestRule.waitForIdle()

    // Now the avatar image node should be present
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_IMAGE).assertIsDisplayed()
  }
}
