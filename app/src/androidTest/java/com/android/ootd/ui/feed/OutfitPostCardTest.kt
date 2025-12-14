package com.android.ootd.ui.feed

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.emptyLocation
import com.android.ootd.model.posts.OutfitPost
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for OutfitPostCard
 *
 * Disclaimer: Some parts of the tests were written with the help of AI
 */
class OutfitPostCardTest {

  @get:Rule val composeTestRule = createComposeRule()

  // Helpers
  private fun setCard(
      post: OutfitPost,
      isBlurred: Boolean = false,
      isLiked: Boolean = false,
      likeCount: Int = 0,
      onLikeClick: (String) -> Unit = {},
      onSeeFitClick: (String) -> Unit = {},
      onCardClick: (String) -> Unit = {},
      onLocationClick: (Location) -> Unit = {},
      onProfileClick: (String) -> Unit = {}
  ) =
      composeTestRule.setContent {
        // Wrap in a scrollable column to handle content taller than the test screen
        androidx.compose.foundation.layout.Column(
            modifier =
                androidx.compose.ui.Modifier.fillMaxSize()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()) // Add this
            ) {
              OutfitPostCard(
                  post = post,
                  isBlurred = isBlurred,
                  isLiked = isLiked,
                  likeCount = likeCount,
                  onLikeClick = onLikeClick,
                  onCardClick = onCardClick,
                  onLocationClick = onLocationClick,
                  onProfileClick = onProfileClick)
            }
      }

  private fun n(tag: String) = composeTestRule.onNodeWithTag(tag, useUnmergedTree = true)

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

  @Test
  fun rendersBasics() {
    setCard(post("John Doe", "Casual Friday outfit"))

    n(OutfitPostCardTestTags.OUTFIT_POST_CARD).assertIsDisplayed()
    n(OutfitPostCardTestTags.POST_USERNAME).assertTextEquals("John Doe")
    n(OutfitPostCardTestTags.POST_DESCRIPTION).assertTextEquals("John Doe: Casual Friday outfit")

    // Like button + count should appear
    n(OutfitPostCardTestTags.LIKE_BUTTON).assertIsDisplayed()
    n(OutfitPostCardTestTags.LIKE_COUNT).assertIsDisplayed()
  }

  @Test
  fun rendersDescriptionLine_whenEmpty() {
    setCard(post(name = "Minimalist", description = ""))

    n(OutfitPostCardTestTags.POST_USERNAME).assertTextEquals("Minimalist")
    n(OutfitPostCardTestTags.POST_DESCRIPTION).assertTextEquals("Minimalist")
  }

  @Test
  fun renders_whenBlurred() {
    setCard(post(name = "Blurred"), isBlurred = true)
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
    val p = post(description = longDesc)

    setCard(p)

    val descNode = n(OutfitPostCardTestTags.POST_DESCRIPTION)
    descNode.performClick()
    descNode.assertIsDisplayed()
  }

  @Test
  fun showsRemainingLifetimeIndicator() {
    val recent = post().copy(timestamp = System.currentTimeMillis() - 2 * 60 * 60 * 1000)
    setCard(recent)

    n(OutfitPostCardTestTags.REMAINING_TIME).assertIsDisplayed()
  }

  @Test
  fun showsExpiredIndicator_forOldPost() {
    val old = post().copy(timestamp = System.currentTimeMillis() - 26 * 60 * 60 * 1000)
    setCard(old)

    n(OutfitPostCardTestTags.EXPIRED_INDICATOR).assertIsDisplayed()
  }

  @Test
  fun postLocation_rendersWhenValid() {
    val postWithLocation = post(location = Location(46.5, 6.6, "Lausanne, Switzerland"))
    setCard(postWithLocation)

    n(OutfitPostCardTestTags.POST_LOCATION).assertIsDisplayed()
    n(OutfitPostCardTestTags.POST_LOCATION).assertTextEquals("Lausanne, Switzerland")
  }

  @Test
  fun postLocation_notRenderedWhenEmpty() {
    val postWithoutLocation = post(location = emptyLocation)
    setCard(postWithoutLocation)

    n(OutfitPostCardTestTags.POST_LOCATION).assertDoesNotExist()
  }

  @Test
  fun postLocation_truncatesLongNames() {
    val longLocationName = "A".repeat(60)
    val postWithLongLocation = post(location = Location(46.5, 6.6, longLocationName))
    setCard(postWithLongLocation)

    n(OutfitPostCardTestTags.POST_LOCATION).performScrollTo().assertIsDisplayed()
    // Should be truncated to 47 chars + "..."
    val displayedText =
        n(OutfitPostCardTestTags.POST_LOCATION)
            .fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.Text]
            .firstOrNull()
            ?.text ?: ""
    assertEquals(50, displayedText.length) // 47 + "..."
  }

  @Test
  fun postLocation_invokesCallbackOnClick() {
    var clickCount = 0
    var clickedLocation: Location? = null
    val testLocation = Location(46.5, 6.6, "Test Location")

    setCard(
        post(location = testLocation),
        onLocationClick = { location ->
          clickCount++
          clickedLocation = location
        })

    n(OutfitPostCardTestTags.POST_LOCATION).performClick()

    assertEquals(1, clickCount)
    assertEquals(testLocation, clickedLocation)
  }

  @Test
  fun likeButton_invokesCallback() {
    var clickCount = 0
    var clickedPostId = ""

    setCard(
        post(),
        onLikeClick = { postId ->
          clickCount++
          clickedPostId = postId
        })

    n(OutfitPostCardTestTags.LIKE_BUTTON).performClick()

    assertEquals(1, clickCount)
    assertEquals("id", clickedPostId)
  }

  @Test
  fun likeButton_showsLikedState() {
    setCard(post(), isLiked = true, likeCount = 5)

    n(OutfitPostCardTestTags.LIKE_BUTTON).assertIsDisplayed()
    n(OutfitPostCardTestTags.LIKE_COUNT).assertTextEquals("5")
  }
}
