package com.android.ootd.ui.feed

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.ootd.model.posts.OutfitPost
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

  // --- Helper to create a base OutfitPost for tests ---
  private fun createTestPost(
      postUID: String = "test-id",
      ownerId: String = "user-123",
      name: String = "Test User",
      description: String = "Test description",
      timestamp: Long = System.currentTimeMillis(),
      outfitURL: String = "",
      userProfilePicURL: String = "",
      itemsID: List<String> = emptyList()
  ): OutfitPost {
    return OutfitPost(
        postUID = postUID,
        ownerId = ownerId,
        name = name,
        description = description,
        timestamp = timestamp,
        outfitURL = outfitURL,
        userProfilePicURL = userProfilePicURL,
        itemsID = itemsID)
  }

  // --- Tests ---

  @Test
  fun outfitPostCard_isDisplayed() {
    // Given a post
    val testPost = createTestPost(name = "John Doe", description = "Casual Friday outfit")

    composeTestRule.setContent { OutfitPostCard(post = testPost, isBlurred = false) }

    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.OUTFIT_POST_CARD).assertIsDisplayed()
  }

  @Test
  fun outfitPostCard_displaysUsernameCorrectly() {
    val testPost = createTestPost(name = "John Doe")

    composeTestRule.setContent { OutfitPostCard(post = testPost, isBlurred = false) }

    composeTestRule
        .onNodeWithTag(OutfitPostCardTestTags.POST_USERNAME)
        .assertIsDisplayed()
        .assertTextEquals("John Doe")
  }

  @Test
  fun outfitPostCard_displaysDescriptionCorrectly() {
    val testPost =
        createTestPost(name = "Jane Smith", description = "Summer vibes with denim jacket")

    composeTestRule.setContent { OutfitPostCard(post = testPost, isBlurred = false) }

    composeTestRule
        .onNodeWithTag(OutfitPostCardTestTags.POST_DESCRIPTION)
        .assertIsDisplayed()
        .assertTextEquals("Jane Smith: Summer vibes with denim jacket")
  }

  @Test
  fun outfitPostCard_displaysImagePlaceholder() {
    val testPost = createTestPost()

    composeTestRule.setContent { OutfitPostCard(post = testPost, isBlurred = false) }

    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.POST_IMAGE_PLACEHOLDER).assertIsDisplayed()
  }

  @Test
  fun outfitPostCard_seeFitButtonIsDisplayed() {
    val testPost = createTestPost()

    composeTestRule.setContent { OutfitPostCard(post = testPost, isBlurred = false) }

    composeTestRule
        .onNodeWithTag(OutfitPostCardTestTags.SEE_FIT_BUTTON)
        .assertIsDisplayed()
        .assertHasClickAction()
  }

  @Test
  fun outfitPostCard_seeFitButtonTriggersCallback() {
    var clickCount = 0
    val testPost = createTestPost()

    composeTestRule.setContent {
      OutfitPostCard(post = testPost, isBlurred = false, onSeeFitClick = { clickCount++ })
    }

    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.SEE_FIT_BUTTON).performClick()

    assert(clickCount == 1) { "Expected click count to be 1, but was $clickCount" }
  }

  @Test
  fun outfitPostCard_seeFitButtonTriggersCallbackMultipleTimes() {
    var clickCount = 0
    val testPost = createTestPost()

    composeTestRule.setContent {
      OutfitPostCard(post = testPost, isBlurred = false, onSeeFitClick = { clickCount++ })
    }

    val button = composeTestRule.onNodeWithTag(OutfitPostCardTestTags.SEE_FIT_BUTTON)
    repeat(3) { button.performClick() }

    assert(clickCount == 3) { "Expected click count to be 3, but was $clickCount" }
  }

  @Test
  fun outfitPostCard_handlesEmptyDescription() {
    val testPost = createTestPost(name = "Minimalist User", description = "")

    composeTestRule.setContent { OutfitPostCard(post = testPost, isBlurred = false) }

    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.OUTFIT_POST_CARD).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(OutfitPostCardTestTags.POST_USERNAME)
        .assertIsDisplayed()
        .assertTextEquals("Minimalist User")
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.POST_DESCRIPTION).assertDoesNotExist()
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.SEE_FIT_BUTTON).assertIsDisplayed()
  }

  @Test
  fun outfitPostCard_rendersWithBlurredState() {
    val testPost = createTestPost(name = "Blurred User", description = "This should be blurred")

    composeTestRule.setContent { OutfitPostCard(post = testPost, isBlurred = true) }

    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.OUTFIT_POST_CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.POST_USERNAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.POST_DESCRIPTION).assertIsDisplayed()
  }

  @Test
  fun outfitPostCard_rendersWithLongDescription() {
    val longDescription =
        "This is a really long description that contains a lot of text " +
            "about the outfit, including details about the colors, patterns, accessories, " +
            "and the inspiration behind the look. It goes on and on to test how the card " +
            "handles long text content."

    val testPost = createTestPost(name = "Verbose User", description = longDescription)

    composeTestRule.setContent { OutfitPostCard(post = testPost, isBlurred = false) }

    composeTestRule
        .onNodeWithTag(OutfitPostCardTestTags.POST_USERNAME)
        .assertIsDisplayed()
        .assertTextEquals("Verbose User")
    composeTestRule
        .onNodeWithTag(OutfitPostCardTestTags.POST_DESCRIPTION)
        .assertIsDisplayed()
        .assertTextEquals("Verbose User: $longDescription")
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.SEE_FIT_BUTTON).assertIsDisplayed()
  }

  @Test
  fun outfitPostCard_allElementsRenderedTogether() {
    val testPost =
        createTestPost(
            postUID = "complete-post",
            ownerId = "user-456",
            name = "Complete User",
            description = "Complete outfit description")

    composeTestRule.setContent { OutfitPostCard(post = testPost, isBlurred = false) }

    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.OUTFIT_POST_CARD).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(OutfitPostCardTestTags.POST_USERNAME)
        .assertIsDisplayed()
        .assertTextEquals("Complete User")
    composeTestRule
        .onNodeWithTag(OutfitPostCardTestTags.POST_DESCRIPTION)
        .assertIsDisplayed()
        .assertTextEquals("Complete User: Complete outfit description")
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.POST_IMAGE_PLACEHOLDER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.SEE_FIT_BUTTON).assertIsDisplayed()
  }

  @Test
  fun outfitPostCard_displaysWithItemsList() {
    val testPost =
        createTestPost(
            name = "Fashionista",
            description = "Outfit with tracked items",
            outfitURL = "https://example.com/outfit.jpg",
            userProfilePicURL = "https://example.com/profile.jpg",
            itemsID = listOf("item1", "item2", "item3"))

    composeTestRule.setContent { OutfitPostCard(post = testPost, isBlurred = false) }

    composeTestRule.onNodeWithTag(OutfitPostCardTestTags.OUTFIT_POST_CARD).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(OutfitPostCardTestTags.POST_USERNAME)
        .assertTextEquals("Fashionista")
    composeTestRule
        .onNodeWithTag(OutfitPostCardTestTags.POST_DESCRIPTION)
        .assertTextEquals("Fashionista: Outfit with tracked items")
  }
}
