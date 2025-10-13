package com.android.ootd.ui.feed

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.ootd.model.post.OutfitPost
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for OutfitPostCard component using test tags.
 *
 * Coverage:
 * - Card displays username correctly
 * - Card displays description correctly
 * - Image placeholder is visible
 * - "See fit" button triggers callback
 * - Card handles empty descriptions
 * - Card renders with blur state (currently no visual difference)
 */
class OutfitPostCardTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun outfitPostCard_isDisplayed() {
    // Given a post
    val testPost =
        OutfitPost(
            postUID = "test-post-id",
            uid = "user-123",
            name = "John Doe",
            description = "Casual Friday outfit",
            timestamp = System.currentTimeMillis(),
            outfitURL = "",
            userProfilePicURL = "",
            itemsID = emptyList())

    // When the card is displayed
    composeTestRule.setContent { OutfitPostCard(post = testPost, isBlurred = false) }

    // Then the card should be visible
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.OUTFIT_POST_CARD).assertIsDisplayed()
  }

  @Test
  fun outfitPostCard_displaysUsernameCorrectly() {
    // Given a post with a specific username
    val testPost =
        OutfitPost(
            postUID = "test-id",
            uid = "user-123",
            name = "John Doe",
            description = "Casual Friday outfit",
            timestamp = System.currentTimeMillis(),
            outfitURL = "",
            userProfilePicURL = "",
            itemsID = emptyList())

    // When the card is displayed
    composeTestRule.setContent { OutfitPostCard(post = testPost, isBlurred = false) }

    // Then the username should be visible with correct text
    composeTestRule
        .onNodeWithTag(OutfitPostCardTestTags.POST_USERNAME)
        .assertIsDisplayed()
        .assertTextEquals("John Doe")
  }

  @Test
  fun outfitPostCard_displaysDescriptionCorrectly() {
    // Given a post with a description
    val testPost =
        OutfitPost(
            postUID = "test-id",
            uid = "user-123",
            name = "Jane Smith",
            description = "Summer vibes with denim jacket",
            timestamp = System.currentTimeMillis(),
            outfitURL = "",
            userProfilePicURL = "",
            itemsID = emptyList())

    // When the card is displayed
    composeTestRule.setContent { OutfitPostCard(post = testPost, isBlurred = false) }

    // Then the description should be visible with correct text
    composeTestRule
        .onNodeWithTag(OutfitPostCardTestTags.POST_DESCRIPTION)
        .assertIsDisplayed()
        .assertTextEquals("Summer vibes with denim jacket")
  }

  @Test
  fun outfitPostCard_displaysImagePlaceholder() {
    // Given any post
    val testPost =
        OutfitPost(
            postUID = "test-id",
            uid = "user-123",
            name = "Test User",
            description = "Test description",
            timestamp = System.currentTimeMillis(),
            outfitURL = "",
            userProfilePicURL = "",
            itemsID = emptyList())

    // When the card is displayed
    composeTestRule.setContent { OutfitPostCard(post = testPost, isBlurred = false) }

    // Then the image placeholder should be visible
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.POST_IMAGE_PLACEHOLDER).assertIsDisplayed()
  }

  @Test
  fun outfitPostCard_seeFitButtonIsDisplayed() {
    // Given a post
    val testPost =
        OutfitPost(
            postUID = "test-id",
            uid = "user-123",
            name = "Test User",
            description = "Test description",
            timestamp = System.currentTimeMillis(),
            outfitURL = "",
            userProfilePicURL = "",
            itemsID = emptyList())

    // When the card is displayed
    composeTestRule.setContent { OutfitPostCard(post = testPost, isBlurred = false) }

    // Then the "See fit" button should be visible
    composeTestRule
        .onNodeWithTag(OutfitPostCardTestTags.SEE_FIT_BUTTON)
        .assertIsDisplayed()
        .assertHasClickAction()
  }

  @Test
  fun outfitPostCard_seeFitButtonTriggersCallback() {
    // Given a post and a click tracker
    var clickCount = 0
    val testPost =
        OutfitPost(
            postUID = "test-id",
            uid = "user-123",
            name = "Test User",
            description = "Test description",
            timestamp = System.currentTimeMillis(),
            outfitURL = "",
            userProfilePicURL = "",
            itemsID = emptyList())

    // When the card is displayed with a callback
    composeTestRule.setContent {
      OutfitPostCard(post = testPost, isBlurred = false, onSeeFitClick = { clickCount++ })
    }

    // And the button is clicked
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.SEE_FIT_BUTTON).performClick()

    // Then the callback should be triggered
    assert(clickCount == 1) { "Expected click count to be 1, but was $clickCount" }
  }

  @Test
  fun outfitPostCard_seeFitButtonTriggersCallbackMultipleTimes() {
    // Given a post and a click tracker
    var clickCount = 0
    val testPost =
        OutfitPost(
            postUID = "test-id",
            uid = "user-123",
            name = "Test User",
            description = "Test description",
            timestamp = System.currentTimeMillis(),
            outfitURL = "",
            userProfilePicURL = "",
            itemsID = emptyList())

    // When the card is displayed
    composeTestRule.setContent {
      OutfitPostCard(post = testPost, isBlurred = false, onSeeFitClick = { clickCount++ })
    }

    // And the button is clicked multiple times
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.SEE_FIT_BUTTON).performClick()
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.SEE_FIT_BUTTON).performClick()
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.SEE_FIT_BUTTON).performClick()

    // Then all clicks should be registered
    assert(clickCount == 3) { "Expected click count to be 3, but was $clickCount" }
  }

  @Test
  fun outfitPostCard_handlesEmptyDescription() {
    // Given a post with an empty description
    val testPost =
        OutfitPost(
            postUID = "test-id",
            uid = "user-123",
            name = "Minimalist User",
            description = "",
            timestamp = System.currentTimeMillis(),
            outfitURL = "",
            userProfilePicURL = "",
            itemsID = emptyList())

    // When the card is displayed
    composeTestRule.setContent { OutfitPostCard(post = testPost, isBlurred = false) }

    // Then the card should still render without crashing
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.OUTFIT_POST_CARD).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(OutfitPostCardTestTags.POST_USERNAME)
        .assertIsDisplayed()
        .assertTextEquals("Minimalist User")
    // Description should not exist when empty
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.POST_DESCRIPTION).assertDoesNotExist()
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.SEE_FIT_BUTTON).assertIsDisplayed()
  }

  @Test
  fun outfitPostCard_rendersWithBlurredState() {
    // Given a post with blur enabled
    val testPost =
        OutfitPost(
            postUID = "test-id",
            uid = "user-123",
            name = "Blurred User",
            description = "This should be blurred",
            timestamp = System.currentTimeMillis(),
            outfitURL = "",
            userProfilePicURL = "",
            itemsID = emptyList())

    // When the card is displayed with blur
    composeTestRule.setContent { OutfitPostCard(post = testPost, isBlurred = true) }

    // Then the card should still render normally (blur not yet implemented)
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.OUTFIT_POST_CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.POST_USERNAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.POST_DESCRIPTION).assertIsDisplayed()
  }

  @Test
  fun outfitPostCard_rendersWithLongDescription() {
    // Given a post with a very long description
    val longDescription =
        "This is a really long description that contains a lot of text " +
            "about the outfit, including details about the colors, patterns, accessories, " +
            "and the inspiration behind the look. It goes on and on to test how the card " +
            "handles long text content."

    val testPost =
        OutfitPost(
            postUID = "test-id",
            uid = "user-123",
            name = "Verbose User",
            description = longDescription,
            timestamp = System.currentTimeMillis(),
            outfitURL = "",
            userProfilePicURL = "",
            itemsID = emptyList())

    // When the card is displayed
    composeTestRule.setContent { OutfitPostCard(post = testPost, isBlurred = false) }

    // Then all content should be displayed
    composeTestRule
        .onNodeWithTag(OutfitPostCardTestTags.POST_USERNAME)
        .assertIsDisplayed()
        .assertTextEquals("Verbose User")
    composeTestRule
        .onNodeWithTag(OutfitPostCardTestTags.POST_DESCRIPTION)
        .assertIsDisplayed()
        .assertTextEquals(longDescription)
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.SEE_FIT_BUTTON).assertIsDisplayed()
  }

  @Test
  fun outfitPostCard_allElementsRenderedTogether() {
    // Given a complete post
    val testPost =
        OutfitPost(
            postUID = "complete-post",
            uid = "user-456",
            name = "Complete User",
            description = "Complete outfit description",
            timestamp = System.currentTimeMillis(),
            outfitURL = "",
            userProfilePicURL = "",
            itemsID = emptyList())

    // When the card is displayed
    composeTestRule.setContent { OutfitPostCard(post = testPost, isBlurred = false) }

    // Then all elements should be present
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.OUTFIT_POST_CARD).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(OutfitPostCardTestTags.POST_USERNAME)
        .assertIsDisplayed()
        .assertTextEquals("Complete User")
    composeTestRule
        .onNodeWithTag(OutfitPostCardTestTags.POST_DESCRIPTION)
        .assertIsDisplayed()
        .assertTextEquals("Complete outfit description")
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.POST_IMAGE_PLACEHOLDER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.SEE_FIT_BUTTON).assertIsDisplayed()
  }

  @Test
  fun outfitPostCard_displaysWithItemsList() {
    // Given a post with items
    val testPost =
        OutfitPost(
            postUID = "test-id",
            uid = "user-123",
            name = "Fashionista",
            description = "Outfit with tracked items",
            timestamp = System.currentTimeMillis(),
            outfitURL = "https://example.com/outfit.jpg",
            userProfilePicURL = "https://example.com/profile.jpg",
            itemsID = listOf("item1", "item2", "item3"))

    // When the card is displayed
    composeTestRule.setContent { OutfitPostCard(post = testPost, isBlurred = false) }

    // Then all elements should render correctly
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.OUTFIT_POST_CARD).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(OutfitPostCardTestTags.POST_USERNAME)
        .assertTextEquals("Fashionista")
    composeTestRule
        .onNodeWithTag(OutfitPostCardTestTags.POST_DESCRIPTION)
        .assertTextEquals("Outfit with tracked items")
  }
}
