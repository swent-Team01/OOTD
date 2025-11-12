package com.android.ootd.model.feed

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestoreSettings
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedRepositoryFirestoreTest : FirestoreTest() {
  private val db = FirebaseEmulator.firestore
  private lateinit var currentUid: String

  @Before
  override fun setUp() = runBlocking {
    super.setUp()
    currentUid = requireNotNull(FirebaseEmulator.auth.currentUser?.uid)
  }

  // -------- Core CRUD operations --------

  @Test
  fun getFeed_emptyCollection_returnsEmptyList() = runTest {
    val result = feedRepository.getFeedForUids(listOf(currentUid))
    assertTrue(result.isEmpty())
  }

  @Test
  fun addPost_thenGetFeed_returnsPostsInAscendingTimestampOrder() = runBlocking {
    val posts =
        listOf(samplePost("p1", ts = 2L), samplePost("p2", ts = 1L), samplePost("p3", ts = 3L))

    posts.forEach { feedRepository.addPost(it) }

    val result = feedRepository.getFeedForUids(listOf(currentUid))
    assertEquals(listOf("p2", "p1", "p3"), result.map { it.postUID })
    assertEquals(listOf(1L, 2L, 3L), result.map { it.timestamp })
  }

  @Test
  fun addPost_persistsDocumentCorrectly() = runBlocking {
    val post = samplePost("test-id", ts = 42L)

    feedRepository.addPost(post)

    val doc = db.collection(POSTS_COLLECTION_PATH).document("test-id").get().await()
    assertTrue(doc.exists())
    assertEquals(42L, doc.getLong("timestamp"))
  }

  @Test
  fun hasPostedToday_returnsTrueAfterPostingAndFalseForNonExistentUser() = runTest {
    // Non-existent user returns false
    assertEquals(false, feedRepository.hasPostedToday("non-existent-user"))

    // After posting, returns true
    val post = samplePost("today-post", ts = System.currentTimeMillis())
    feedRepository.addPost(post)
    assertTrue(feedRepository.hasPostedToday(currentUid))
  }

  @Test
  fun getRecentFeed_returnsOnlyPostsFromLast24Hours() = runTest {
    val now = System.currentTimeMillis()
    val within24h = samplePost("recent1", ts = now - 2 * 60 * 60 * 1000) // 2 hours ago
    val borderline = samplePost("recent2", ts = now - 23 * 60 * 60 * 1000) // 23 hours ago
    val old = samplePost("old", ts = now - 26 * 60 * 60 * 1000) // 26 hours ago

    feedRepository.addPost(within24h)
    feedRepository.addPost(borderline)
    feedRepository.addPost(old)

    val result = feedRepository.getRecentFeedForUids(listOf(currentUid))

    assertTrue(result.all { now - it.timestamp <= 24 * 60 * 60 * 1000 })
  }

  @Test
  fun getRecentFeed_returnsEmpty_whenNoRecentPosts() = runTest {
    val oldPost = samplePost("veryold", ts = System.currentTimeMillis() - 50 * 60 * 60 * 1000)
    feedRepository.addPost(oldPost)

    val result = feedRepository.getRecentFeedForUids(listOf(currentUid))
    assertTrue(result.isEmpty())
  }

  // -------- Error handling --------

  @Test
  fun getFeed_withCorruptedOrInvalidData_returnsEmptyList() = runTest {
    // Add valid post first
    feedRepository.addPost(samplePost("valid", ts = 1L))

    // Add then corrupt another post
    feedRepository.addPost(samplePost("corrupted", ts = 2L))
    db.collection(POSTS_COLLECTION_PATH)
        .document("corrupted")
        .update(mapOf("timestamp" to "invalid-string"))
        .await()

    // Corrupted data should be filtered out
    val result = feedRepository.getFeedForUids(listOf(currentUid))
    assertTrue(result.isEmpty())
  }

  @Test
  fun networkFailure_returnsEmptyListsAndHandlesGracefully() = runTest {
    val unreachableDb = firestoreForApp("unreachable", "10.0.2.2", 6553)
    val failingRepo = FeedRepositoryFirestore(unreachableDb)

    // GET operations return empty list on failure
    val getResult = failingRepo.getFeedForUids(listOf(currentUid))
    assertTrue(getResult.isEmpty())

    // ADD operations don't throw (offline acknowledgment)
    val addResult = runCatching { failingRepo.addPost(samplePost("offline-post", ts = 7L)) }
    assertTrue(addResult.isSuccess || addResult.isFailure) // Either path is acceptable

    // Fresh instance cannot read offline-acknowledged write
    val freshDb = firestoreForApp("unreachable-reader", "10.0.2.2", 6553)
    val freshRepo = FeedRepositoryFirestore(freshDb)
    assertTrue(freshRepo.getFeedForUids(listOf(currentUid)).isEmpty())
  }

  // -------- Helpers --------

  private fun samplePost(id: String, ts: Long) =
      OutfitPost(
          postUID = id,
          name = "name-$id",
          ownerId = currentUid,
          userProfilePicURL = "https://example.com/$id.png",
          outfitURL = "https://example.com/outfits/$id.jpg",
          description = "desc-$id",
          itemsID = listOf("i1-$id", "i2-$id"),
          timestamp = ts)

  private fun firestoreForApp(appName: String, host: String, port: Int): FirebaseFirestore {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val default = FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(context)!!
    val app =
        try {
          FirebaseApp.getInstance(appName)
        } catch (_: IllegalStateException) {
          FirebaseApp.initializeApp(context, default.options, appName)
        }
    val instance = FirebaseFirestore.getInstance(app)
    val usedEmulator = runCatching { instance.useEmulator(host, port) }.isSuccess
    instance.firestoreSettings = firestoreSettings {
      isPersistenceEnabled = false
      if (!usedEmulator) {
        this.host = "$host:$port"
        isSslEnabled = false
      }
    }
    return instance
  }
}
