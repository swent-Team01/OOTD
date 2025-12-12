package com.android.ootd.ui.feed

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.ootd.model.map.Location
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.ui.theme.OOTDTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OutfitPostCardBlurTest {

  @get:Rule val composeRule = createComposeRule()

  @Test
  fun blurredImage_clickInvokesBlurredCallback() {
    val post =
        OutfitPost(
            postUID = "p1",
            ownerId = "owner",
            name = "name",
            userProfilePicURL = "",
            outfitURL = "",
            description = "desc",
            itemsID = emptyList(),
            timestamp = 0L,
            location = Location(latitude = 0.0, longitude = 0.0, name = ""))

    var blurredClicks = 0

    composeRule.setContent {
      OOTDTheme {
        OutfitPostCard(
            post = post,
            isBlurred = true,
            isLiked = false,
            likeCount = 0,
            onLikeClick = {},
            onSeeFitClick = {},
            onCardClick = {},
            onBlurredClick = { blurredClicks++ },
            onLocationClick = {},
            onProfileClick = {})
      }
    }

    composeRule
        .onNodeWithTag(OutfitPostCardTestTags.POST_IMAGE, useUnmergedTree = true)
        .performClick()

    assertEquals(1, blurredClicks)
  }
}
