package com.android.ootd.ui.post

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.android.ootd.ui.theme.OOTDTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PostViewDescriptionTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun description_rendersUsernameAndText() {
    composeRule.setContent {
      OOTDTheme { PostDescription(username = "user", description = "loves fashion") }
    }

    composeRule.onNodeWithText("user loves fashion").assertIsDisplayed()
  }
}
