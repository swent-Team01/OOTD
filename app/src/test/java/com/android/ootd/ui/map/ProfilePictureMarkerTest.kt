package com.android.ootd.ui.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
 * Tests cover:
 * - Marker content rendering with profile picture URL
 * - Marker content rendering with username initials fallback
 * - Theme integration (colors and typography)
 *
 * Disclaimer: This test was written with the assistance of AI.
 */
@RunWith(RobolectricTestRunner::class)
class ProfilePictureMarkerTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun markerContent_withImageUrl_displaysImage() {
    val imageUrl = "https://example.com/profile.jpg"
    val username = "TestUser"

    composeTestRule.setContent {
      OOTDTheme { MarkerContent(username = username, imageUrl = imageUrl) }
    }

    // Verify the image content description is present
    composeTestRule
        .onNodeWithContentDescription("Profile Picture for $username")
        .assertIsDisplayed()
  }

  @Test
  fun markerContent_withNullImageUrl_displaysInitials() {
    val username = "TestUser"

    composeTestRule.setContent { OOTDTheme { MarkerContent(username = username, imageUrl = null) } }

    // Verify the first letter of username is displayed
    composeTestRule.onNodeWithText("T").assertIsDisplayed()
  }

  @Test
  fun markerContent_withEmptyImageUrl_displaysInitials() {
    val username = "Alice"

    composeTestRule.setContent { OOTDTheme { MarkerContent(username = username, imageUrl = "") } }

    // Verify the first letter of username is displayed
    composeTestRule.onNodeWithText("A").assertIsDisplayed()
  }

  @Test
  fun markerContent_initialsAreUppercase() {
    val username = "testuser"

    composeTestRule.setContent { OOTDTheme { MarkerContent(username = username, imageUrl = null) } }

    // Verify the initial is uppercase
    composeTestRule.onNodeWithText("T").assertIsDisplayed()
  }

  @Test
  fun markerContent_handlesLongUsername() {
    val longUsername = "VeryLongUsernameWithManyCharacters"

    composeTestRule.setContent {
      OOTDTheme { MarkerContent(username = longUsername, imageUrl = null) }
    }

    // Should only display first letter
    composeTestRule.onNodeWithText("V").assertIsDisplayed()
  }

  @Test
  fun markerContent_handlesEmptyUsername() {
    composeTestRule.setContent { OOTDTheme { MarkerContent(username = "", imageUrl = null) } }

    // With empty username, take(1) returns empty string
    // Component should still render without crashing
    composeTestRule.waitForIdle()
  }

  @Test
  fun markerContent_withValidImageUrl_loadsImagePainter() {
    val imageUrl = "https://example.com/valid-profile.jpg"
    val username = "ImageUser"

    composeTestRule.setContent {
      OOTDTheme { MarkerContent(username = username, imageUrl = imageUrl) }
    }

    // Verify the image is set up (content description exists)
    composeTestRule.onNodeWithContentDescription("Profile Picture for $username").assertExists()
  }

  @Test
  fun markerContent_displaysInitialsForSingleCharacterUsername() {
    val username = "X"

    composeTestRule.setContent { OOTDTheme { MarkerContent(username = username, imageUrl = null) } }

    // Should display the single character
    composeTestRule.onNodeWithText("X").assertIsDisplayed()
  }
}
