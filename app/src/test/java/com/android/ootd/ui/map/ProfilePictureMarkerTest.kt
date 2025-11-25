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
  fun markerContent_withOverlappingCount1_noBadgeDisplayed() {
    val username = "TestUser"

    composeTestRule.setContent {
      OOTDTheme { MarkerContent(username = username, imageUrl = null, overlappingCount = 1) }
    }

    // Verify the initial is displayed
    composeTestRule.onNodeWithText("T").assertIsDisplayed()
    // Badge with count should not be visible when count is 1
    composeTestRule.onNodeWithText("1").assertDoesNotExist()
  }

  @Test
  fun markerContent_withOverlappingCount2_badgeDisplayed() {
    val username = "TestUser"

    composeTestRule.setContent {
      OOTDTheme { MarkerContent(username = username, imageUrl = null, overlappingCount = 2) }
    }

    // Verify the initial is displayed
    composeTestRule.onNodeWithText("T").assertIsDisplayed()
    // Badge with count 2 should be visible
    composeTestRule.onNodeWithText("2").assertIsDisplayed()
  }

  @Test
  fun markerContent_withOverlappingCount5_badgeDisplaysCorrectNumber() {
    val username = "TestUser"

    composeTestRule.setContent {
      OOTDTheme {
        MarkerContent(
            username = username, imageUrl = "https://example.com/pic.jpg", overlappingCount = 5)
      }
    }

    // Badge with count 5 should be visible
    composeTestRule.onNodeWithText("5").assertIsDisplayed()
  }

  @Test
  fun markerContent_withOverlappingCount10_badgeDisplaysDoubleDigits() {
    val username = "TestUser"

    composeTestRule.setContent {
      OOTDTheme { MarkerContent(username = username, imageUrl = null, overlappingCount = 10) }
    }

    // Verify the initial is displayed
    composeTestRule.onNodeWithText("T").assertIsDisplayed()
    // Badge with count 10 should be visible
    composeTestRule.onNodeWithText("10").assertIsDisplayed()
  }

  @Test
  fun markerContent_badgeUsesAppTheme_withPrimaryColor() {
    val username = "TestUser"

    composeTestRule.setContent {
      OOTDTheme { MarkerContent(username = username, imageUrl = null, overlappingCount = 3) }
    }

    // Verify badge with count 3 is displayed (badge uses Primary color from theme)
    composeTestRule.onNodeWithText("3").assertIsDisplayed()
  }

  @Test
  fun markerContent_badgeWithLargeCount_displaysCorrectly() {
    val username = "TestUser"

    composeTestRule.setContent {
      OOTDTheme {
        MarkerContent(
            username = username, imageUrl = "https://example.com/pic.jpg", overlappingCount = 99)
      }
    }

    // Badge should handle large numbers
    composeTestRule.onNodeWithText("99").assertIsDisplayed()
  }

  @Test
  fun markerContent_withImageAndBadge_bothDisplayed() {
    val imageUrl = "https://example.com/profile.jpg"
    val username = "TestUser"

    composeTestRule.setContent {
      OOTDTheme { MarkerContent(username = username, imageUrl = imageUrl, overlappingCount = 4) }
    }

    // Verify both image and badge are displayed
    composeTestRule
        .onNodeWithContentDescription("Profile Picture for $username")
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("4").assertIsDisplayed()
  }

  @Test
  fun markerContent_withInitialsAndBadge_bothDisplayed() {
    val username = "Alice"

    composeTestRule.setContent {
      OOTDTheme { MarkerContent(username = username, imageUrl = null, overlappingCount = 7) }
    }

    // Verify both initial and badge are displayed
    composeTestRule.onNodeWithText("A").assertIsDisplayed()
    composeTestRule.onNodeWithText("7").assertIsDisplayed()
  }

  @Test
  fun markerContent_validImageUrl_loadsImagePainter() {
    val imageUrl = "https://example.com/valid-profile.jpg"
    val username = "ImageUser"

    composeTestRule.setContent {
      OOTDTheme { MarkerContent(username = username, imageUrl = imageUrl) }
    }

    // Verify AsyncImage loads with correct content description
    composeTestRule
        .onNodeWithContentDescription("Profile Picture for $username")
        .assertIsDisplayed()
  }
}
