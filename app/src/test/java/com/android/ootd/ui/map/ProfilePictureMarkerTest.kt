package com.android.ootd.ui.map

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
  fun markerContent_withNullBitmap_displaysInitials() {
    composeTestRule.setContent {
      OOTDTheme { MarkerContent(profileBitmap = null, username = "TestUser") }
    }
    composeTestRule.onNodeWithText("T").assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfilePictureMarkerTestTags.PROFILE_LETTER).assertIsDisplayed()
  }

  @Test
  fun markerContent_withNullBitmap_initialsAreUppercase() {
    composeTestRule.setContent {
      OOTDTheme { MarkerContent(profileBitmap = null, username = "testuser") }
    }
    composeTestRule.onNodeWithText("T").assertIsDisplayed()
  }

  @Test
  fun markerContent_withNullBitmap_handlesEmptyUsername() {
    composeTestRule.setContent { OOTDTheme { MarkerContent(profileBitmap = null, username = "") } }
    composeTestRule.onNodeWithTag(ProfilePictureMarkerTestTags.PROFILE_LETTER).assertIsDisplayed()
  }

  @Test
  fun markerContent_withBitmap_displaysImage() {
    // Create a simple 10x10 bitmap for testing
    val testBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)

    composeTestRule.setContent {
      OOTDTheme { MarkerContent(profileBitmap = testBitmap, username = "TestUser") }
    }

    composeTestRule.onNodeWithTag(ProfilePictureMarkerTestTags.PROFILE_IMAGE).assertIsDisplayed()
  }
}
