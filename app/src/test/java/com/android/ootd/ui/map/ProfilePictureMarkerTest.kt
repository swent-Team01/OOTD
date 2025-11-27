package com.android.ootd.ui.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.android.ootd.ui.theme.OOTDTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for MarkerContent composable (the visual content of ProfilePictureMarker). We test
 * MarkerContent separately because ProfilePictureMarker uses MarkerComposable which requires full
 * Google Maps rendering and cannot be tested in Robolectric.
 *
 * Disclaimer: This test was written with the assistance of AI.
 */
@RunWith(RobolectricTestRunner::class)
class ProfilePictureMarkerTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun markerContent_displaysInitials() {
    composeTestRule.setContent { OOTDTheme { MarkerContent(username = "TestUser") } }
    composeTestRule.onNodeWithText("T").assertIsDisplayed()
  }

  @Test
  fun markerContent_initialsAreUppercase() {
    composeTestRule.setContent { OOTDTheme { MarkerContent(username = "testuser") } }
    composeTestRule.onNodeWithText("T").assertIsDisplayed()
  }

  @Test
  fun markerContent_handlesEmptyUsername() {
    composeTestRule.setContent { OOTDTheme { MarkerContent(username = "") } }
    composeTestRule.waitForIdle()
  }
}
