package com.android.ootd.ui.map

import com.android.ootd.model.map.Location
import com.android.ootd.model.posts.OutfitPost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for PostMarker cluster item.
 *
 * Tests verify that OutfitPost data is correctly exposed through the ClusterItem interface for
 * Google Maps marker clustering.
 *
 * Disclaimer: This test was written with the assistance of AI.
 */
class PostMarkerTest {

  private val testLocation = Location(46.5197, 6.6323, "Lausanne")
  private val testPost =
      OutfitPost(
          postUID = "test-post-1",
          name = "TestUser",
          ownerId = "user123",
          userProfilePicURL = "https://example.com/pic.jpg",
          outfitURL = "https://example.com/outfit.jpg",
          description = "Test outfit",
          itemsID = emptyList(),
          timestamp = System.currentTimeMillis(),
          location = testLocation)

  @Test
  fun getPosition_returnsCorrectLatLng() {
    val marker = PostMarker(testPost)
    val position = marker.getPosition()

    assertEquals(testLocation.latitude, position.latitude, 0.0001)
    assertEquals(testLocation.longitude, position.longitude, 0.0001)
  }

  @Test
  fun getTitle_returnsPostName() {
    val marker = PostMarker(testPost)
    assertEquals("TestUser", marker.getTitle())
  }

  @Test
  fun getSnippet_returnsNull() {
    val marker = PostMarker(testPost)
    assertNull(marker.getSnippet())
  }

  @Test
  fun getZIndex_returnsDefaultValue() {
    val marker = PostMarker(testPost)
    assertEquals(0f, marker.getZIndex())
  }

  @Test
  fun postMarker_preservesPostReference() {
    val marker = PostMarker(testPost)
    assertEquals(testPost, marker.post)
    assertEquals("test-post-1", marker.post.postUID)
    assertEquals("user123", marker.post.ownerId)
  }

  @Test
  fun getPosition_handlesMultipleLocations() {
    val locations =
        listOf(
            Location(46.5197, 6.6323, "Lausanne"),
            Location(40.7128, -74.0060, "New York"),
            Location(51.5074, -0.1278, "London"))

    locations.forEach { location ->
      val post = testPost.copy(location = location)
      val marker = PostMarker(post)
      val position = marker.getPosition()

      assertEquals(location.latitude, position.latitude, 0.0001)
      assertEquals(location.longitude, position.longitude, 0.0001)
    }
  }

  @Test
  fun dataClass_equalityWorks() {
    val marker1 = PostMarker(testPost)
    val marker2 = PostMarker(testPost)

    assertEquals(marker1, marker2)
    assertEquals(marker1.hashCode(), marker2.hashCode())
  }

  @Test
  fun dataClass_inequalityWithDifferentPosts() {
    val post2 = testPost.copy(postUID = "different-post")
    val marker1 = PostMarker(testPost)
    val marker2 = PostMarker(post2)

    assert(marker1 != marker2)
  }
}
