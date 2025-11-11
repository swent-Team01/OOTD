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
      profilePic: String = ""
  ) =
      OutfitPost(
          postUID = "id",
          ownerId = "owner",
          name = name,
          description = description,
          timestamp = 0L,
          outfitURL = "",
          userProfilePicURL = profilePic,
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
}
