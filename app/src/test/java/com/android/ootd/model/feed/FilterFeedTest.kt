package com.android.ootd.model.feed

import com.android.ootd.model.account.Account
import com.android.ootd.model.posts.OutfitPost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for feed filtering and business logic. These tests don't require Firebase/Firestore
 * and run faster than integration tests.
 */
class FilterFeedTest {

  // -------- Friend filtering --------

  @Test
  fun onlyFriendsPostsAreReturned_excludesSelf() {
    val posts =
        listOf(
            createPost("p1", ownerId = "u1", timestamp = 3L),
            createPost("p2", ownerId = "u9", timestamp = 2L), // Non-friend
            createPost("p3", ownerId = "me", timestamp = 1L), // Self - excluded
            createPost("p4", ownerId = "u2", timestamp = 4L),
        )

    val currentAccount = createAccount(uid = "me", friendUids = listOf("u1", "u2"))

    val filtered = filterPostsByFriends(posts, currentAccount)

    assertEquals(listOf("p1", "p4"), filtered.map { it.postUID })
  }

  @Test
  fun emptyFriendsListReturnsEmpty() {
    val posts =
        listOf(
            createPost("p1", ownerId = "u1", timestamp = 3L),
            createPost("p2", ownerId = "me", timestamp = 1L),
        )

    val currentAccount = createAccount(uid = "me", friendUids = emptyList())

    val filtered = filterPostsByFriends(posts, currentAccount)

    assertTrue(filtered.isEmpty())
  }

  @Test
  fun emptyPostsListReturnsEmpty() {
    val currentAccount = createAccount(uid = "me", friendUids = listOf("u1", "u2"))

    val filtered = filterPostsByFriends(emptyList(), currentAccount)

    assertTrue(filtered.isEmpty())
  }

  // -------- Timestamp sorting --------

  @Test
  fun postsAreSortedByTimestampAscending() {
    val unsortedPosts =
        listOf(
            createPost("p1", timestamp = 5L),
            createPost("p2", timestamp = 1L),
            createPost("p3", timestamp = 3L),
            createPost("p4", timestamp = 2L),
        )

    val sorted = sortPostsByTimestamp(unsortedPosts)

    assertEquals(listOf("p2", "p4", "p3", "p1"), sorted.map { it.postUID })
    assertEquals(listOf(1L, 2L, 3L, 5L), sorted.map { it.timestamp })
  }

  @Test
  fun postsWithSameTimestamp_maintainStableOrder() {
    val posts =
        listOf(
            createPost("p1", timestamp = 2L),
            createPost("p2", timestamp = 2L),
            createPost("p3", timestamp = 2L),
        )

    val sorted = sortPostsByTimestamp(posts)

    // Should maintain original order for equal timestamps (stable sort)
    assertEquals(listOf("p1", "p2", "p3"), sorted.map { it.postUID })
  }

  // -------- Post validation --------

  @Test
  fun isValidPost_returnsTrueForCompletePost() {
    val validPost = createPost("p1", ownerId = "u1", timestamp = 123L)

    assertTrue(isValidPost(validPost))
  }

  @Test
  fun isValidPost_returnsFalseForEmptyPostId() {
    val invalidPost = createPost("", ownerId = "u1", timestamp = 123L)

    assertFalse(isValidPost(invalidPost))
  }

  @Test
  fun isValidPost_returnsFalseForEmptyOwnerId() {
    val invalidPost = createPost("p1", ownerId = "", timestamp = 123L)

    assertFalse(isValidPost(invalidPost))
  }

  @Test
  fun isValidPost_returnsFalseForNegativeTimestamp() {
    val invalidPost = createPost("p1", ownerId = "u1", timestamp = -1L)

    assertFalse(isValidPost(invalidPost))
  }

  // -------- "Posted today" logic --------

  @Test
  fun isPostedToday_returnsTrueForTodaysPosts() {
    val now = System.currentTimeMillis()
    val todayPost = createPost("p1", timestamp = now)

    assertTrue(isPostedToday(todayPost, now))
  }

  @Test
  fun isPostedToday_returnsFalseForYesterdaysPosts() {
    val now = System.currentTimeMillis()
    val oneDayAgo = now - (24 * 60 * 60 * 1000)
    val yesterdayPost = createPost("p1", timestamp = oneDayAgo)

    assertFalse(isPostedToday(yesterdayPost, now))
  }

  @Test
  fun isPostedToday_returnsFalseForFuturePosts() {
    val now = System.currentTimeMillis()
    val futurePost = createPost("p1", timestamp = now + 10000L)

    assertFalse(isPostedToday(futurePost, now))
  }

  // -------- Combined filtering and sorting --------

  @Test
  fun filterAndSort_appliesBothOperations() {
    val posts =
        listOf(
            createPost("p1", ownerId = "u1", timestamp = 5L), // Friend
            createPost("p2", ownerId = "u9", timestamp = 1L), // Not friend
            createPost("p3", ownerId = "u2", timestamp = 3L), // Friend
            createPost("p4", ownerId = "me", timestamp = 2L), // Self
        )

    val currentAccount = createAccount(uid = "me", friendUids = listOf("u1", "u2"))

    val result = filterAndSortPosts(posts, currentAccount)

    // Should only have friends' posts, sorted by timestamp
    assertEquals(listOf("p3", "p1"), result.map { it.postUID })
    assertEquals(listOf(3L, 5L), result.map { it.timestamp })
  }

  // -------- Helper functions (production logic that would be in repository/utils) --------

  private fun filterPostsByFriends(
      posts: List<OutfitPost>,
      currentAccount: Account
  ): List<OutfitPost> {
    val allowedUids = currentAccount.friendUids.toSet()
    return posts.filter { it.ownerId in allowedUids }
  }

  private fun sortPostsByTimestamp(posts: List<OutfitPost>): List<OutfitPost> {
    return posts.sortedBy { it.timestamp }
  }

  private fun isValidPost(post: OutfitPost): Boolean {
    return post.postUID.isNotEmpty() && post.ownerId.isNotEmpty() && post.timestamp >= 0
  }

  private fun isPostedToday(
      post: OutfitPost,
      currentTime: Long = System.currentTimeMillis()
  ): Boolean {
    val dayInMillis = 24 * 60 * 60 * 1000L
    val startOfToday = (currentTime / dayInMillis) * dayInMillis
    val endOfToday = startOfToday + dayInMillis
    return post.timestamp >= startOfToday &&
        post.timestamp < endOfToday &&
        post.timestamp <= currentTime
  }

  private fun filterAndSortPosts(
      posts: List<OutfitPost>,
      currentAccount: Account
  ): List<OutfitPost> {
    return filterPostsByFriends(posts, currentAccount).let { sortPostsByTimestamp(it) }
  }

  // -------- Test data builders --------

  private fun createPost(
      postUID: String,
      name: String = "Test Post",
      ownerId: String = "owner1",
      timestamp: Long = System.currentTimeMillis()
  ): OutfitPost {
    return OutfitPost(
        postUID = postUID,
        name = name,
        ownerId = ownerId,
        userProfilePicURL = "https://example.com/pic.jpg",
        outfitURL = "https://example.com/outfit.jpg",
        description = "Test description",
        itemsID = emptyList(),
        timestamp = timestamp)
  }

  private fun createAccount(
      uid: String,
      username: String = "TestUser",
      friendUids: List<String> = emptyList()
  ): Account {
    return Account(uid = uid, ownerId = uid, username = username, friendUids = friendUids)
  }
}
