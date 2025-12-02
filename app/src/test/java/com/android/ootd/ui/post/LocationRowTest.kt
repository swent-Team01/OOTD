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
class LocationRowTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun locationRow_showsLocationText() {
    composeRule.setContent {
      OOTDTheme { LocationRow(location = "Paris", isExpanded = true, onToggleExpanded = {}) }
    }

    composeRule.onNodeWithText("Paris").assertIsDisplayed()
  }
}
