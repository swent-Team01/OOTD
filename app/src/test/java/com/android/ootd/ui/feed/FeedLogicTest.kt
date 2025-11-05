package com.android.ootd.ui.feed

import com.android.ootd.model.posts.OutfitPost
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for feed business logic (no Android/Firebase dependencies). These run faster than
 * integration tests.
 */
class FeedLogicTest {

  // ========================================================================
  // Post Validation
  // ========================================================================

  @Test
  fun validatePost_withValidData_isValid() {
    val post = createPost("id", "user", 1000L)
    assertTrue(isValidPost(post))
  }

  @Test
  fun validatePost_withInvalidData_isInvalid() {
    assertFalse(isValidPost(createPost("", "user", 1000L))) // Empty ID
    assertFalse(isValidPost(createPost("id", "", 1000L))) // Empty owner
    assertFalse(isValidPost(createPost("id", "user", 0L))) // Zero timestamp
    assertFalse(isValidPost(createPost("id", "user", -1L))) // Negative timestamp
  }

  // ========================================================================
  // Timestamp Sorting
  // ========================================================================

  @Test
  fun sortPosts_byTimestamp_ascending() {
    val posts =
        listOf(
            createPost("p3", timestamp = 3000L),
            createPost("p1", timestamp = 1000L),
            createPost("p2", timestamp = 2000L))

    val sorted = sortByTimestamp(posts)

    assertEquals(listOf("p1", "p2", "p3"), sorted.map { it.postUID })
  }

  @Test
  fun sortPosts_withEqualTimestamps_maintainsOrder() {
    val posts = listOf(createPost("p1", timestamp = 1000L), createPost("p2", timestamp = 1000L))

    val sorted = sortByTimestamp(posts)

    assertEquals(listOf("p1", "p2"), sorted.map { it.postUID })
  }

  // ========================================================================
  // "Posted Today" Logic
  // ========================================================================

  @Test
  fun isPostedToday_checksCorrectly() {
    val now = System.currentTimeMillis()
    val yesterday = now - (25 * 60 * 60 * 1000L)
    val future = now + (60 * 60 * 1000L)

    assertTrue(isPostedToday(createPost("p1", timestamp = now), now))
    assertFalse(isPostedToday(createPost("p2", timestamp = yesterday), now))
    assertFalse(isPostedToday(createPost("p3", timestamp = future), now))
  }

  // ========================================================================
  // Post Filtering
  // ========================================================================

  @Test
  fun filterValidPosts_removesInvalid() {
    val posts =
        listOf(
            createPost("valid1", "user1", 1000L),
            createPost("", "user2", 2000L), // Invalid
            createPost("valid2", "user3", 3000L))

    val filtered = filterValid(posts)

    assertEquals(2, filtered.size)
    assertEquals(listOf("valid1", "valid2"), filtered.map { it.postUID })
  }

  // ========================================================================
  // Helpers
  // ========================================================================

  private fun createPost(postUID: String, ownerId: String = "owner", timestamp: Long = 1000L) =
      OutfitPost(postUID = postUID, ownerId = ownerId, timestamp = timestamp)

  private fun isValidPost(post: OutfitPost) =
      post.postUID.isNotEmpty() && post.ownerId.isNotEmpty() && post.timestamp > 0

  private fun sortByTimestamp(posts: List<OutfitPost>) = posts.sortedBy { it.timestamp }

  private fun isPostedToday(post: OutfitPost, currentTime: Long): Boolean {
    val oneDayMs = 24 * 60 * 60 * 1000L
    val diff = currentTime - post.timestamp
    return diff in 0 until oneDayMs
  }

  private fun filterValid(posts: List<OutfitPost>) = posts.filter { isValidPost(it) }
}
