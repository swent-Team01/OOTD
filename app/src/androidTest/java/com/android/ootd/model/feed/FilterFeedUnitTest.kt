package com.android.ootd.model.feed

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.OutfitPost
import com.android.ootd.model.user.Friend
import com.android.ootd.model.user.User
import com.android.ootd.ui.feed.FeedViewModel
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests friends-only filtering through the production ViewModel + repository API. We inject a fake
 * FeedRepository that implements getFeedForUids and verify VM wiring.
 */
@RunWith(AndroidJUnit4::class)
class FilterFeedTest {

  @Test
  fun onlyFriendsPostsAreReturned_viaGetFeedForUids() = runBlocking {
    val posts =
        listOf(
            OutfitPost(
                postUID = "p1",
                name = "Alice",
                uid = "u1",
                userProfilePicURL = "",
                outfitURL = "",
                description = "A",
                itemsID = emptyList(),
                timestamp = 1L),
            OutfitPost(
                postUID = "p2",
                name = "Mallory",
                uid = "u9",
                userProfilePicURL = "",
                outfitURL = "",
                description = "B",
                itemsID = emptyList(),
                timestamp = 2L),
            OutfitPost(
                postUID = "p3",
                name = "Me",
                uid = "me",
                userProfilePicURL = "",
                outfitURL = "",
                description = "C",
                itemsID = emptyList(),
                timestamp = 3L),
            OutfitPost(
                postUID = "p4",
                name = "Bob",
                uid = "u2",
                userProfilePicURL = "",
                outfitURL = "",
                description = "D",
                itemsID = emptyList(),
                timestamp = 4L),
        )

    val fakeRepo =
        object : FeedRepository {
          override suspend fun getFeed(): List<OutfitPost> = posts

          override suspend fun getFeedForUids(uids: List<String>): List<OutfitPost> =
              posts.filter { it.uid in uids.toSet() }.sortedBy { it.timestamp }

          override suspend fun hasPostedToday(userId: String): Boolean = true

          override suspend fun addPost(post: OutfitPost) {}

          override fun getNewPostId(): String = "fake"
        }

    val saved = FeedRepositoryProvider.repository
    FeedRepositoryProvider.repository = fakeRepo
    try {
      val vm = FeedViewModel()
      val currentUser =
          User(
              uid = "me",
              name = "Me",
              friendList =
                  listOf(Friend(uid = "u1", name = "Alice"), Friend(uid = "u2", name = "Bob")))

      vm.setCurrentUser(currentUser) // hasPostedToday = true via fake, will load immediately

      val state = withTimeout(5_000) { vm.uiState.filter { it.feedPosts.size == 2 }.first() }
      val filtered = state.feedPosts
      assertEquals(listOf("p1", "p4"), filtered.map { it.postUID })
      assertTrue(state.hasPostedToday)
    } finally {
      FeedRepositoryProvider.repository = saved
    }
  }

  @Test
  fun emptyFriendsList_returnsEmpty_viaProductionPath() = runBlocking {
    val posts =
        listOf(
            OutfitPost(
                postUID = "p1",
                name = "Alice",
                uid = "u1",
                userProfilePicURL = "",
                outfitURL = "",
                description = "A",
                itemsID = emptyList(),
                timestamp = 3L),
            OutfitPost(
                postUID = "p2",
                name = "Me",
                uid = "me",
                userProfilePicURL = "",
                outfitURL = "",
                description = "C",
                itemsID = emptyList(),
                timestamp = 1L),
        )

    val fakeRepo =
        object : FeedRepository {
          override suspend fun getFeed(): List<OutfitPost> = posts

          override suspend fun getFeedForUids(uids: List<String>): List<OutfitPost> =
              posts.filter { it.uid in uids.toSet() }.sortedBy { it.timestamp }

          override suspend fun hasPostedToday(userId: String): Boolean = true

          override suspend fun addPost(post: OutfitPost) {}

          override fun getNewPostId(): String = "fake"
        }

    val saved = FeedRepositoryProvider.repository
    FeedRepositoryProvider.repository = fakeRepo
    try {
      val vm = FeedViewModel()
      val currentUser = User(uid = "me", name = "Me", friendList = emptyList())

      vm.setCurrentUser(currentUser)

      // Wait until VM acknowledges hasPostedToday, then assert feed is empty
      val state = withTimeout(5_000) { vm.uiState.filter { it.hasPostedToday }.first() }
      val filtered = state.feedPosts
      assertEquals(emptyList<String>(), filtered.map { it.postUID })
    } finally {
      FeedRepositoryProvider.repository = saved
    }
  }
}
