package com.android.ootd.screen

import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.android.ootd.ui.register.RegisterScreenTestTags

fun ComposeTestRule.enterUsername(text: String) {
  onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).performClick()
  onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).performTextClearance()
  onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).performTextInput(text)
}

fun ComposeTestRule.enterDate(text: String) {
  onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_DATE).performClick()
  onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_DATE).performTextClearance()
  onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_DATE).performTextInput(text)
}
