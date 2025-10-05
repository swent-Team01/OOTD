package com.android.ootd

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class GreetingPreviewTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun showsPreviewText() {
    composeRule.setContent { GreetingPreview() }
    composeRule.onNodeWithText("Preview").assertExists()
  }
}
