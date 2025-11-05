package com.android.ootd.ui.feed

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.ootd.model.posts.OutfitPost
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** UI tests for OutfitPostCard kept minimal but meaningful. */
class OutfitPostCardTest {

  @get:Rule val composeTestRule = createComposeRule()

  // Helpers
  private fun setCard(post: OutfitPost, blurred: Boolean = false, onSeeFitClick: () -> Unit = {}) =
      composeTestRule.setContent {
        OutfitPostCard(post = post, isBlurred = blurred, onSeeFitClick = onSeeFitClick)
      }

  private fun n(tag: String) = composeTestRule.onNodeWithTag(tag)

  private fun post(name: String = "Test User", description: String = "Test description") =
      OutfitPost(
          postUID = "id",
          ownerId = "owner",
          name = name,
          description = description,
          timestamp = 0L,
          outfitURL = "",
          userProfilePicURL = "",
          itemsID = emptyList())

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
  fun hidesDescription_whenEmpty() {
    setCard(post(name = "Minimalist", description = ""))

    n(OutfitPostCardTestTags.POST_USERNAME).assertTextEquals("Minimalist")
    n(OutfitPostCardTestTags.POST_DESCRIPTION).assertDoesNotExist()
  }

  @Test
  fun seeFitButton_invokesCallback() {
    var clicks = 0
    setCard(post(), onSeeFitClick = { clicks++ })

    n(OutfitPostCardTestTags.SEE_FIT_BUTTON).performClick()

    assertEquals(1, clicks)
  }

  @Test
  fun renders_whenBlurred() {
    setCard(post(name = "Blurred", description = "x"), blurred = true)

    n(OutfitPostCardTestTags.OUTFIT_POST_CARD).assertIsDisplayed()
  }
}
