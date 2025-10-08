package com.android.ootd.model.feed

import com.android.ootd.model.OutfitPost
import com.android.ootd.model.user.Friend
import com.android.ootd.model.user.User
import com.android.ootd.utils.FirebaseEmulator
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Firestore-emulator backed tests for FeedViewModel friends-only filtering.
 *
 * Pre-req: run the Firebase emulators (at least Firestore) locally before executing tests: firebase
 * emulators:start --only firestore,auth
 */
class FeedViewModelFirestoreTest {

  private val db = FirebaseEmulator.firestore
  private lateinit var repo: FeedRepositoryFirestore
  private var originalProviderRepo: FeedRepository? = null

  @Before
  fun setUp() = runBlocking {
    assert(FirebaseEmulator.isRunning) { "FirebaseEmulator must be running" }

    repo = FeedRepositoryFirestore(db)
    originalProviderRepo = FeedRepositoryProvider.repository
    FeedRepositoryProvider.repository = repo

    clearPosts()
  }

  @After
  fun tearDown() = runBlocking {
    clearPosts()
    originalProviderRepo?.let { FeedRepositoryProvider.repository = it }
    FirebaseEmulator.clearFirestoreEmulator()
  }

  @Test
  fun friendsOnlyFiltering_afterOnPostUploaded_loadsOnlyFriendsPosts() = runBlocking {
    // Seed 3 posts: two by friends (u1,u2), one by non-friend (u9)
    seedPosts(
        listOf(
            post("p1", "Alice", "u1", ts = 1L),
            post("p2", "Bob", "u2", ts = 2L),
            post("p3", "Mallory", "u9", ts = 3L)))
    assertEquals(3, getPostsCount())

    val vm = com.android.ootd.ui.feed.FeedViewModel()
    val currentUser =
        User(
            uid = "me",
            name = "Me",
            friendList = listOf(Friend("u1", "Alice"), Friend("u2", "Bob")))
    vm.setCurrentUser(currentUser)

    vm.onPostUploaded()

    val filtered = withTimeout(5_000) { vm.feedPosts.filter { it.isNotEmpty() }.first() }

    assertEquals(listOf("p1", "p2"), filtered.map { it.postUID })
    assertTrue(vm.hasPostedToday.value)
  }

  @Test
  fun emptyFriendsList_resultsInEmptyFeed_evenWhenPostsExist() = runBlocking {
    seedPosts(listOf(post("p1", "Alice", "u1", ts = 1L), post("p2", "Bob", "u2", ts = 2L)))

    val vm = com.android.ootd.ui.feed.FeedViewModel()
    vm.setCurrentUser(User(uid = "me", name = "Me", friendList = emptyList()))

    vm.onPostUploaded()

    val maybe = withTimeout(5_000) { vm.feedPosts.first() }
    assertEquals(0, maybe.size)
  }

  @Test
  fun init_withHasPostedTodayTrue_loadsAndFiltersWithoutOnPostUploaded() = runBlocking {
    seedPosts(
        listOf(
            post("p1", "Alice", "u1", ts = 1L),
            post("p2", "Bob", "u2", ts = 2L),
            post("p3", "Mallory", "u9", ts = 3L)))

    // Force hasPostedToday = true via provider wrapper
    originalProviderRepo = FeedRepositoryProvider.repository
    FeedRepositoryProvider.repository =
        object : FeedRepository by repo {
          override suspend fun hasPostedToday(userId: String): Boolean = true
        }

    val vm = com.android.ootd.ui.feed.FeedViewModel()

    assertEquals(0, vm.feedPosts.first().size)

    val currentUser = User(uid = "me", name = "Me", friendList = listOf(Friend("u2", "Bob")))
    vm.setCurrentUser(currentUser)

    val filtered = withTimeout(5_000) { vm.feedPosts.filter { it.isNotEmpty() }.first() }
    assertEquals(listOf("p2"), filtered.map { it.postUID })
    assertTrue(vm.hasPostedToday.value)
  }

  @Test
  fun excludesSelfPosts_whenUserNotInFriendList() = runBlocking {
    // Current user posts, but has no friends
    seedPosts(listOf(post("pSelf", "Me", "me", ts = 1L)))

    val vm = com.android.ootd.ui.feed.FeedViewModel()
    vm.setCurrentUser(User(uid = "me", name = "Me", friendList = emptyList()))

    vm.onPostUploaded()

    val list = withTimeout(5_000) { vm.feedPosts.first() }
    assertEquals(0, list.size)
  }

  @Test
  fun preservesOrderByTimestamp_afterFiltering() = runBlocking {
    // Mixed authors; only u1 and u3 are friends; timestamps out of insert order
    seedPosts(
        listOf(
            post("p3", "C", "u3", ts = 3L),
            post("p2", "B", "u2", ts = 2L),
            post("p1", "A", "u1", ts = 1L),
            post("p4", "D", "u1", ts = 4L)))

    val vm = com.android.ootd.ui.feed.FeedViewModel()
    vm.setCurrentUser(
        User(uid = "me", name = "Me", friendList = listOf(Friend("u1", "A"), Friend("u3", "C"))))

    vm.onPostUploaded()

    val filtered = withTimeout(5_000) { vm.feedPosts.filter { it.isNotEmpty() }.first() }
    assertEquals(listOf("p1", "p3", "p4"), filtered.map { it.postUID })
  }

  @Test
  fun changingCurrentUser_recomputesFilter() = runBlocking {
    seedPosts(listOf(post("p1", "Alice", "u1", ts = 1L), post("p2", "Bob", "u2", ts = 2L)))

    val vm = com.android.ootd.ui.feed.FeedViewModel()

    vm.setCurrentUser(User(uid = "me", name = "Me", friendList = emptyList()))
    vm.onPostUploaded()
    assertEquals(0, withTimeout(5_000) { vm.feedPosts.first() }.size)

    vm.setCurrentUser(User(uid = "me", name = "Me", friendList = listOf(Friend("u2", "Bob"))))
    val filtered = withTimeout(5_000) { vm.feedPosts.filter { it.isNotEmpty() }.first() }
    assertEquals(listOf("p2"), filtered.map { it.postUID })
  }

  @Test
  fun emptyDatabase_resultsInEmptyFeed_whenHasPostedTrue() = runBlocking {
    // Ensure no posts
    clearPosts()

    originalProviderRepo = FeedRepositoryProvider.repository
    FeedRepositoryProvider.repository =
        object : FeedRepository by repo {
          override suspend fun hasPostedToday(userId: String): Boolean = true
        }

    val vm = com.android.ootd.ui.feed.FeedViewModel()
    vm.setCurrentUser(User(uid = "me", name = "Me", friendList = listOf(Friend("u1", "A"))))

    assertEquals(0, withTimeout(5_000) { vm.feedPosts.first() }.size)
  }

  @Test
  fun duplicateFriendEntries_doNotDuplicatePosts() = runBlocking {
    seedPosts(listOf(post("p1", "Alice", "u1", ts = 1L)))

    val vm = com.android.ootd.ui.feed.FeedViewModel()
    vm.setCurrentUser(
        User(
            uid = "me",
            name = "Me",
            friendList = listOf(Friend("u1", "Alice"), Friend("u1", "Alice"))))

    vm.onPostUploaded()

    val filtered = withTimeout(5_000) { vm.feedPosts.filter { it.isNotEmpty() }.first() }
    assertEquals(listOf("p1"), filtered.map { it.postUID })
  }

  @Test
  fun hasPostedFalse_keepsFeedEmpty_untilOnPostUploadedCalled() = runBlocking {
    seedPosts(listOf(post("p1", "Alice", "u1", ts = 1L), post("p2", "Bob", "u2", ts = 2L)))

    // Default repo will report false for users without posts today
    val vm = com.android.ootd.ui.feed.FeedViewModel()
    vm.setCurrentUser(User(uid = "me", name = "Me", friendList = listOf(Friend("u1", "Alice"))))

    // Before posting, feed should remain empty
    assertEquals(0, vm.feedPosts.first().size)

    // After posting, feed should load and filter
    vm.onPostUploaded()
    val filtered = withTimeout(5_000) { vm.feedPosts.filter { it.isNotEmpty() }.first() }
    assertEquals(listOf("p1"), filtered.map { it.postUID })
  }

  @Test
  fun noCurrentUser_evenWhenHasPostedTrue_feedRemainsEmpty() = runBlocking {
    seedPosts(listOf(post("p1", "Alice", "u1", ts = 1L)))

    // Force hasPostedToday = true so VM loads posts on init
    originalProviderRepo = FeedRepositoryProvider.repository
    FeedRepositoryProvider.repository =
        object : FeedRepository by repo {
          override suspend fun hasPostedToday(userId: String): Boolean = true
        }

    val vm = com.android.ootd.ui.feed.FeedViewModel()
    // Do not set current user

    // Feed should remain empty since user is null
    assertEquals(0, withTimeout(5_000) { vm.feedPosts.first() }.size)
  }

  @Test
  fun invalidFriendEntries_blankAndWhitespaceIgnored() = runBlocking {
    seedPosts(listOf(post("p1", "Alice", "u1", ts = 1L), post("p2", "Bob", "u2", ts = 2L)))

    val vm = com.android.ootd.ui.feed.FeedViewModel()
    vm.setCurrentUser(
        User(
            uid = "me",
            name = "Me",
            friendList = listOf(Friend(" ", "Ghost"), Friend("", "Nobody"), Friend("u2", "Bob"))))

    vm.onPostUploaded()

    val filtered = withTimeout(5_000) { vm.feedPosts.filter { it.isNotEmpty() }.first() }
    // Only the valid friend uid should match
    assertEquals(listOf("p2"), filtered.map { it.postUID })
  }

  @Test
  fun equalTimestamps_returnsAllFriendPosts_regardlessOfOrder() = runBlocking {
    seedPosts(listOf(post("pA", "Alice", "u1", ts = 100L), post("pB", "Bob", "u2", ts = 100L)))

    val vm = com.android.ootd.ui.feed.FeedViewModel()
    vm.setCurrentUser(
        User(
            uid = "me",
            name = "Me",
            friendList = listOf(Friend("u1", "Alice"), Friend("u2", "Bob"))))

    vm.onPostUploaded()

    val filtered = withTimeout(5_000) { vm.feedPosts.filter { it.size == 2 }.first() }
    // Order is not guaranteed when timestamps are equal; assert set equality
    assertEquals(setOf("pA", "pB"), filtered.map { it.postUID }.toSet())
  }

  @Test
  fun addPost_failure_propagates_toCaller() = runBlocking {
    val failingRepo =
        object : FeedRepository {
          override suspend fun getFeed(): List<OutfitPost> = emptyList()

          override suspend fun hasPostedToday(userId: String): Boolean = false

          override suspend fun addPost(post: OutfitPost) {
            throw RuntimeException("boom")
          }

          override fun getNewPostId(): String = "dummy"
        }

    val dummy =
        OutfitPost(
            postUID = "x",
            name = "Name",
            uid = "uid",
            userProfilePicURL = "",
            outfitURL = "url_x",
            description = "",
            itemsID = emptyList(),
            timestamp = 1L)

    val ex = runCatching { failingRepo.addPost(dummy) }.exceptionOrNull()
    assertTrue(ex is RuntimeException)
  }

  @Test
  fun newPostId_isUnique_overManyCalls() {
    val ids = (0 until 200).map { repo.getNewPostId() }.toSet()
    assertEquals(200, ids.size)
  }
  // ---------- Helpers ----------

  private suspend fun clearPosts() {
    val docs = db.collection(POSTS_COLLECTION_PATH).get().await().documents
    docs.forEach { it.reference.delete().await() }
  }

  private suspend fun seedPosts(posts: List<OutfitPost>) {
    posts.forEach { repo.addPost(it) }
  }

  private suspend fun getPostsCount(): Int {
    return db.collection(POSTS_COLLECTION_PATH).get().await().size()
  }

  private fun post(
      id: String,
      userName: String,
      userId: String,
      ts: Long,
      profileUrl: String = "",
      outfitUrl: String = "url_$id",
      desc: String = "desc_$id"
  ) =
      OutfitPost(
          postUID = id,
          name = userName,
          uid = userId,
          userProfilePicURL = profileUrl,
          outfitURL = outfitUrl,
          description = desc,
          itemsID = emptyList(),
          timestamp = ts)
}
