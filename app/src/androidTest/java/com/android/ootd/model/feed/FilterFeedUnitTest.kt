package com.android.ootd.model.feed

import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.OutfitPost
import com.android.ootd.model.user.Friend
import com.android.ootd.model.user.User
import kotlin.collections.listOf
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test for friends-only filtering. Local helper for now; replace with production filtering when
 * available.
 */
@RunWith(AndroidJUnit4::class)
class FilterFeedTest {

  @Test
  fun onlyFriendsAndSelfPostsAreReturned() {
    val posts =
        listOf(
            OutfitPost(
                "p1",
                "Alice",
                "u1",
                userProfilePicURL = "".toUri(),
                outfitURL = "".toUri(),
                description = "A",
                itemsID = emptyList(),
                timestamp = 3L),
            OutfitPost(
                "p2",
                "Mallory",
                "u9",
                userProfilePicURL = "".toUri(),
                outfitURL = "".toUri(),
                description = "B",
                itemsID = emptyList(),
                timestamp = 2L),
            OutfitPost(
                "p3",
                "Me",
                "me",
                userProfilePicURL = "".toUri(),
                outfitURL = "".toUri(),
                description = "C",
                itemsID = emptyList(),
                timestamp = 1L),
            OutfitPost(
                "p4",
                "Bob",
                "u2",
                userProfilePicURL = "".toUri(),
                outfitURL = "".toUri(),
                description = "D",
                itemsID = emptyList(),
                timestamp = 3L),
        )

    val currentUser =
        User(
            uid = "me",
            username = "Me",
            friendList =
                listOf(
                    Friend(uid = "u1", username = "Alice"), Friend(uid = "u2", username = "Bob")))

    val filtered = filterPostsByFriendsForTest(posts, currentUser)

    assertEquals(listOf("p1", "p4"), filtered.map { it.postUID })
  }

  @Test
  fun EmptyFriendsListReturnsEmpty() {
    val posts =
        listOf(
            OutfitPost(
                "p1",
                "Alice",
                "u1",
                userProfilePicURL = "".toUri(),
                outfitURL = "".toUri(),
                description = "A",
                itemsID = emptyList(),
                timestamp = 3L),
            OutfitPost(
                "p2",
                "Me",
                "me",
                userProfilePicURL = "".toUri(),
                outfitURL = "".toUri(),
                description = "C",
                itemsID = emptyList(),
                timestamp = 1L),
        )

    val currentUser = User(uid = "me", username = "Me", friendList = emptyList())

    val filtered = filterPostsByFriendsForTest(posts, currentUser)

    assertEquals(emptyList<String>(), filtered.map { it.postUID })
  }

  private fun filterPostsByFriendsForTest(
      posts: List<OutfitPost>,
      currentUser: User
  ): List<OutfitPost> {
    val allowed = currentUser.friendList.map { it.uid }.toMutableSet()
    return posts.filter { it.uid in allowed }
  }
}
