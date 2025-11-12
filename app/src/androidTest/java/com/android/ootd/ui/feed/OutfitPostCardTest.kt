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
    setCard(post(), onSeeFitClick = { clicks++ })

    n(OutfitPostCardTestTags.SEE_FIT_BUTTON).performClick()

    assertEquals(1, clicks)
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
}
