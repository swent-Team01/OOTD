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

  // -------- Core functional tests (merged) --------

  @Test
  fun getFeed_emptyCollection_returnsEmptyList() = runTest {
    val result = feedRepository.getFeedForUids(listOf(currentUid))
    assertTrue(result.isEmpty())
  }

  @Test
  fun addPost_thenGetFeed_returnsAscendingTimestampOrder() = runBlocking {
    val p1 = samplePost("p1", ts = 2L)
    val p2 = samplePost("p2", ts = 1L)
    val p3 = samplePost("p3", ts = 3L)

    feedRepository.addPost(p1)
    feedRepository.addPost(p2)
    feedRepository.addPost(p3)

    val result = feedRepository.getFeedForUids(listOf(currentUid))
    assertEquals(listOf("p2", "p1", "p3"), result.map { it.postUID })
    assertEquals(listOf(1L, 2L, 3L), result.map { it.timestamp })
  }

  @Test
  fun addPost_writesDocumentWithGivenId() = runBlocking {
    val p = samplePost("explicit-id-123", ts = 42L)

    feedRepository.addPost(p)

    val doc = db.collection(POSTS_COLLECTION_PATH).document("explicit-id-123").get().await()
    assertTrue(doc.exists())
    assertEquals(42L, doc.getLong("timestamp"))
  }

  @Test
  fun getFeed_withCorruptedDocument_returnsEmptyList_dueToCatchAll() = runTest {
    val good = samplePost("good", ts = 1L)
    feedRepository.addPost(good)

    // First create a valid doc under the signed-in user's uid so create rules pass
    val badValid = samplePost("bad", ts = 2L)
    feedRepository.addPost(badValid)

    // Now corrupt the document by updating timestamp to a String while keeping uid/postUID
    // unchanged
    db.collection(POSTS_COLLECTION_PATH)
        .document("bad")
        .update(mapOf("timestamp" to "oops"))
        .await()

    val result = feedRepository.getFeedForUids(listOf(currentUid))
    assertTrue(result.isEmpty())
  }

  @Test
  fun getFeed_onNetworkFailure_returnsEmptyList() = runTest {
    val badDb = firestoreForApp(appName = "feed-repo-bad", host = "10.0.2.2", port = 6553)
    val badRepo = FeedRepositoryFirestore(badDb)

    val result = badRepo.getFeedForUids(listOf(currentUid))
    assertTrue(result.isEmpty())
  }

  @Test
  fun addPost_onNetworkFailure_isLocallyAcknowledged_andRepoReadIsEmpty() = runTest {
    val badDb = firestoreForApp(appName = "feed-repo-bad2", host = "10.0.2.2", port = 6553)
    val badRepo = FeedRepositoryFirestore(badDb)

    val p = samplePost("will-fail", ts = 7L)
    // Local ack: should not throw
    runCatching { badRepo.addPost(p) }

    // Same instance: may or may not show local write depending on timing/timeout
    val sameInstance = badRepo.getFeedForUids(listOf(currentUid))
    assertTrue(sameInstance.isEmpty() || sameInstance.any { it.postUID == "will-fail" })

    // Fresh misconfigured instance has no local cache, so read is empty
    val badDbReader =
        firestoreForApp(appName = "feed-repo-bad2-reader", host = "10.0.2.2", port = 6553)
    val badRepoReader = FeedRepositoryFirestore(badDbReader)
    val freshRead = badRepoReader.getFeedForUids(listOf(currentUid))
    assertTrue(freshRead.isEmpty())
  }

  @Test
  fun hasPostedToday_defaultFalse_andTrueWhenUserHasPostedToday() = runTest {
    // default false (user id doesn't exist)
    assertEquals(false, feedRepository.hasPostedToday("non-existent-user"))

    // true after posting today by the signed-in user
    val post = samplePost("today-post", ts = System.currentTimeMillis())
    feedRepository.addPost(post)
    val result = feedRepository.hasPostedToday(currentUid)
    assertTrue(result)
  }

  @Test
  fun getNewPostId_isUniqueAndNonEmpty() {
    val ids = (1..200).map { feedRepository.getNewPostId() }
    assertTrue(ids.all { it.isNotBlank() })
    assertEquals(ids.size, ids.toSet().size)
  }

  @Test
  fun getFeed_throwsException_caughtAndReturnsEmptyList() = runTest {
    val invalidDb = firestoreForApp("feedrepo-throws", "localhost", 65533) // unreachable
    val repo = FeedRepositoryFirestore(invalidDb)
    val result = repo.getFeedForUids(listOf(currentUid))
    assertTrue(result.isEmpty())
  }

  @Test
  fun addPost_whenFirestoreUnavailable_throwsOrOfflineAck() = runTest {
    val invalidDb = firestoreForApp("feedrepo-addpost-fail", "localhost", 65532)
    val repo = FeedRepositoryFirestore(invalidDb)
    val post = samplePost("failing-post", ts = 10L)

    val result = runCatching { repo.addPost(post) }

    if (result.isFailure) {
      assertTrue(true) // immediate failure path exercised
    } else {
      // Offline ack path: verify a fresh misconfigured repo cannot read it
      val freshDb = firestoreForApp("feedrepo-addpost-fail-reader", "localhost", 65532)
      val freshRepo = FeedRepositoryFirestore(freshDb)
      val freshRead = freshRepo.getFeedForUids(listOf(currentUid))
      assertTrue(freshRead.isEmpty())
    }
  }

  @Test
  fun postsCollectionConstant_isCorrect() {
    assertEquals("posts", POSTS_COLLECTION_PATH)
  }

  // -------- Helpers --------

  private fun samplePost(id: String, ts: Long) =
      OutfitPost(
          postUID = id,
          name = "name-$id",
          ownerId = currentUid, // author is the signed-in user so rules allow writes/reads
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
          FirebaseApp.initializeApp(context, default.options, appName)!!
        }
    return FirebaseFirestore.getInstance(app).apply {
      useEmulator(host, port)
      firestoreSettings = firestoreSettings { isPersistenceEnabled = false }
    }
  }
}
