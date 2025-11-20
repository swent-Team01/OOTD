package com.android.ootd.ui.feed

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.emptyLocation
import com.android.ootd.model.posts.OutfitPost
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for OutfitPostCard kept minimal but meaningful.
 *
 * Disclaimer: Some parts of the tests were written with the help of AI
 */
class OutfitPostCardTest {

  @get:Rule val composeTestRule = createComposeRule()

  // Helpers
  private fun setCard(
      post: OutfitPost,
      blurred: Boolean = false,
      onSeeFitClick: (String) -> Unit = { _ -> }
  ) =
      composeTestRule.setContent {
        OutfitPostCard(post = post, isBlurred = blurred, onSeeFitClick = onSeeFitClick)
      }

  private fun n(tag: String) = composeTestRule.onNodeWithTag(tag)

  private fun post(
      name: String = "Test User",
      description: String = "Test description",
      profilePic: String = "",
      location: Location = emptyLocation
  ) =
      OutfitPost(
          postUID = "id",
          ownerId = "owner",
          name = name,
          description = description,
          timestamp = 0L,
          outfitURL = "",
          userProfilePicURL = profilePic,
          itemsID = emptyList(),
          location = location)

  // Tests

  @Test
  fun rendersBasics() {
    setCard(post(name = "John Doe", description = "Casual Friday outfit"))

    n(OutfitPostCardTestTags.OUTFIT_POST_CARD).assertIsDisplayed()
    n(OutfitPostCardTestTags.POST_USERNAME).assertTextEquals("John Doe")
    n(OutfitPostCardTestTags.POST_DESCRIPTION).assertTextEquals("John Doe: Casual Friday outfit")
    n(OutfitPostCardTestTags.SEE_FIT_BUTTON).assertIsDisplayed().assertHasClickAction()
  }

  @Test
  fun rendersDescriptionLine_whenEmpty() {
    setCard(post(name = "Minimalist", description = ""))

    // The username still shows as before
    n(OutfitPostCardTestTags.POST_USERNAME).assertTextEquals("Minimalist")

    // The description node should still exist
    n(OutfitPostCardTestTags.POST_DESCRIPTION).assertIsDisplayed()

    // And it should show only the name (no colon or extra space)
    n(OutfitPostCardTestTags.POST_DESCRIPTION).assertTextEquals("Minimalist")
  }

  @Test
  fun seeFitButton_invokesCallback() {
    var clicks = 0
    var capturedPostUid = ""
    setCard(
        post(),
        onSeeFitClick = { postUid ->
          clicks++
          capturedPostUid = postUid
        })

    n(OutfitPostCardTestTags.SEE_FIT_BUTTON).performClick()

    assertEquals(1, clicks)
    assertEquals("id", capturedPostUid)
  }

  @Test
  fun renders_whenBlurred() {
    setCard(post(name = "Blurred", description = "x"), blurred = true)

    n(OutfitPostCardTestTags.OUTFIT_POST_CARD).assertIsDisplayed()
  }

  @Test
  fun showsInitial_whenNoProfilePicture() {
    setCard(post(name = "Alex", description = "No pic", profilePic = ""))

    n(OutfitPostCardTestTags.PROFILE_INITIAL).assertIsDisplayed()
    n(OutfitPostCardTestTags.PROFILE_PIC).assertDoesNotExist()
  }

  @Test
  fun showsProfileImage_whenUrlProvided() {
    setCard(
        post(name = "Sophie", description = "With pic", profilePic = "https://example.com/pic.jpg"))

    n(OutfitPostCardTestTags.PROFILE_PIC).assertIsDisplayed()
    n(OutfitPostCardTestTags.PROFILE_INITIAL).assertDoesNotExist()
  }

  @Test
  fun description_expandsOnClick() {
    val longDesc = "Very long description ".repeat(20)
    val post = post(name = "User", description = longDesc)

    setCard(post)

    val descNode = n(OutfitPostCardTestTags.POST_DESCRIPTION)
    descNode.assertIsDisplayed()
    descNode.performClick() // toggle expansion
    // No direct assert for expanded lines in Compose testing yet,
    // but you can check that click didn’t crash and node still exists.
    descNode.assertIsDisplayed()
  }

  @Test
  fun showsRemainingLifetimeIndicator() {
    val recentPost =
        post().copy(timestamp = System.currentTimeMillis() - 2 * 60 * 60 * 1000) // 2h ago
    setCard(recentPost)

    n(OutfitPostCardTestTags.REMAINING_TIME).assertIsDisplayed()
  }

  @Test
  fun showsExpiredIndicator_forOldPost() {
    val oldPost =
        post()
            .copy(
                timestamp = System.currentTimeMillis() - 26 * 60 * 60 * 1000 // 26 hours ago
                )
    setCard(oldPost)

    n(OutfitPostCardTestTags.EXPIRED_INDICATOR).assertIsDisplayed()
    n(OutfitPostCardTestTags.REMAINING_TIME).assertDoesNotExist()
  }

  @Test
  fun showsLocation_whenValidLocationProvided() {
    val location = Location(46.5191, 6.5668, "EPFL, Lausanne")
    setCard(post(name = "User", description = "Test", location = location))

    n(OutfitPostCardTestTags.POST_LOCATION).assertIsDisplayed()
    n(OutfitPostCardTestTags.POST_LOCATION).assertTextEquals("EPFL, Lausanne")
  }

  @Test
  fun hidesLocation_whenLocationIsEmpty_or_whenLocationNameIsBlank() {
    setCard(post(name = "User", description = "Test", location = emptyLocation))

    n(OutfitPostCardTestTags.POST_LOCATION).assertDoesNotExist()
  }

  @Test
  fun hidesLocation_whenLocationNameIsBlank() {
    val blankLocation = Location(46.5191, 6.5668, "")
    setCard(post(name = "User", description = "Test", location = blankLocation))

    n(OutfitPostCardTestTags.POST_LOCATION).assertDoesNotExist()
  }

  @Test
  fun hidesLocation_whenLocationHasInvalidCoordinates() {
    val invalidLocation = Location(Double.NaN, Double.NaN, "Invalid Place")
    setCard(post(name = "User", description = "Test", location = invalidLocation))

    n(OutfitPostCardTestTags.POST_LOCATION).assertDoesNotExist()
  }

  @Test
  fun truncatesLongLocationNames() {
    val longName = "VeryLongLocationName_".repeat(4) // > 50 chars
    val location = Location(46.0, 6.0, longName)
    val expected = if (longName.length > 50) longName.take(47) + "..." else longName

    setCard(post(name = "User", description = "Test", location = location))

    n(OutfitPostCardTestTags.POST_LOCATION).assertIsDisplayed()
    n(OutfitPostCardTestTags.POST_LOCATION).assertTextEquals(expected)
  }
}
