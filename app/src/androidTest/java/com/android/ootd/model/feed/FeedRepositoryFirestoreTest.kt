package com.android.ootd.model.feed

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.OutfitPost
import com.android.ootd.utils.FirebaseEmulator
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestoreSettings
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedRepositoryFirestoreTest {

  private val db = FirebaseEmulator.firestore
  private lateinit var repo: FeedRepositoryFirestore

  @Before
  fun setUp() = runBlocking {
    assert(FirebaseEmulator.isRunning) { "FirebaseEmulator must be running" }
    repo = FeedRepositoryFirestore(db)
    clearPosts()
  }

  @After fun tearDown() = runBlocking { clearPosts() }

  @Test
  fun getFeed_emptyCollection_returnsEmptyList() = runBlocking {
    val result = repo.getFeed()
    assertTrue(result.isEmpty())
  }

  @Test
  fun addPost_thenGetFeed_returnsAscendingTimestampOrder() = runBlocking {
    val p1 = post("p1", ts = 2L)
    val p2 = post("p2", ts = 1L)
    val p3 = post("p3", ts = 3L)

    repo.addPost(p1)
    repo.addPost(p2)
    repo.addPost(p3)

    val result = repo.getFeed()
    assertEquals(listOf("p2", "p1", "p3"), result.map { it.postUID })
    assertEquals(listOf(1L, 2L, 3L), result.map { it.timestamp })
  }

  @Test
  fun addPost_writesDocumentWithGivenId() = runBlocking {
    val p = post("explicit-id-123", ts = 42L)
    repo.addPost(p)

    val doc = db.collection(POSTS_COLLECTION_PATH).document("explicit-id-123").get().await()
    assertTrue(doc.exists())
    assertEquals(42L, doc.getLong("timestamp"))
  }

  @Test
  fun getFeed_withCorruptedDocument_returnsEmptyList_dueToCatchAll() = runBlocking {
    // Seed one valid post
    val good = post("good", ts = 1L)
    repo.addPost(good)

    // Inject one invalid/corrupted document (wrong type for timestamp)
    db.collection(POSTS_COLLECTION_PATH)
        .document("bad")
        .set(mapOf("postUID" to "bad", "timestamp" to "oops"))
        .await()

    // Current implementation tries to toObject() each doc without per-doc try/catch;
    // if one fails, the outer try/catch returns emptyList.
    val result = repo.getFeed()
    assertTrue(result.isEmpty())
  }

  @Test
  fun getFeed_onNetworkFailure_returnsEmptyList() = runBlocking {
    val badDb = firestoreForApp(appName = "feed-repo-bad", host = "10.0.2.2", port = 6553)
    val badRepo = FeedRepositoryFirestore(badDb)

    val result = badRepo.getFeed()
    assertTrue(result.isEmpty())
  }

  @Test
  fun addPost_onNetworkFailure_isLocallyAcknowledged_andRepoReadIsEmpty() = runBlocking {
    val badDb = firestoreForApp(appName = "feed-repo-bad2", host = "10.0.2.2", port = 6553)
    val badRepo = FeedRepositoryFirestore(badDb)

    val p = post("will-fail", ts = 7L)
    // Local ack: should not throw
    badRepo.addPost(p)

    // Same instance: may or may not show local write depending on timing/timeout
    val sameInstance = badRepo.getFeed()
    assertTrue(sameInstance.isEmpty() || sameInstance.any { it.postUID == "will-fail" })

    // Fresh misconfigured instance has no local cache, so read is empty
    val badDbReader =
        firestoreForApp(appName = "feed-repo-bad2-reader", host = "10.0.2.2", port = 6553)
    val badRepoReader = FeedRepositoryFirestore(badDbReader)
    val freshRead = badRepoReader.getFeed()
    assertTrue(freshRead.isEmpty())
  }

  @Test
  fun hasPostedToday_defaultFalse() = runBlocking { assertEquals(false, repo.hasPostedToday()) }

  @Test
  fun getNewPostId_isUniqueAndNonEmpty() {
    val ids = (1..200).map { repo.getNewPostId() }
    assertTrue(ids.all { it.isNotBlank() })
    assertEquals(ids.size, ids.toSet().size)
  }

  // -------- Private helpers via reflection to improve coverage --------

  @Test
  fun checkPostData_returnsNullForInvalid_andSameForValid() = runBlocking {
    val invalid = post("", ts = 1L) // blank postUID -> invalid
    val valid = post("ok", ts = 1L)

    val method =
        FeedRepositoryFirestore::class
            .java
            .getDeclaredMethod("checkPostData", OutfitPost::class.java)
    method.isAccessible = true

    val invalidRes = method.invoke(repo, invalid) as OutfitPost?
    val validRes = method.invoke(repo, valid) as OutfitPost?

    assertNull(invalidRes)
    assertEquals(valid, validRes)
  }

  @Test
  fun transformPostDocument_handlesValidAndInvalid() = runBlocking {
    // Valid doc stored through repo
    val good = post("tpd-ok", ts = 5L)
    repo.addPost(good)
    val goodSnap: DocumentSnapshot =
        db.collection(POSTS_COLLECTION_PATH).document("tpd-ok").get().await()

    // Invalid doc (missing required fields)
    db.collection(POSTS_COLLECTION_PATH).document("tpd-bad").set(mapOf("random" to 1)).await()
    val badSnap: DocumentSnapshot =
        db.collection(POSTS_COLLECTION_PATH).document("tpd-bad").get().await()

    val method =
        FeedRepositoryFirestore::class
            .java
            .getDeclaredMethod("transformPostDocument", DocumentSnapshot::class.java)
    method.isAccessible = true

    val ok = method.invoke(repo, goodSnap) as OutfitPost?
    val bad = method.invoke(repo, badSnap) as OutfitPost?

    assertNotNull(ok)
    assertNull(bad)
  }

  // -------- Helpers --------

  private suspend fun clearPosts() {
    val docs = db.collection(POSTS_COLLECTION_PATH).get().await().documents
    docs.forEach { it.reference.delete().await() }
  }

  private fun post(id: String, ts: Long) =
      OutfitPost(
          postUID = id,
          name = "name-$id",
          uid = "user-$id",
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
