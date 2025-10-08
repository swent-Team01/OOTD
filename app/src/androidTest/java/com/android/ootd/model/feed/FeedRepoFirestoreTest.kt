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
class FeedRepoFirestoreTest {

  private val db = FirebaseEmulator.firestore
  private lateinit var repo: FeedRepositoryFirestore

  @Before
  fun setUp() = runBlocking {
    assert(FirebaseEmulator.isRunning) { "FirebaseEmulator must be running" }
    repo = FeedRepositoryFirestore(db)
    clearPosts()
  }

  @After fun tearDown() = runBlocking { clearPosts() }

  // --------------------------------------------------------------
  // Core Functional Tests
  // --------------------------------------------------------------

  @Test
  fun getFeed_emptyCollection_returnsEmptyList() = runBlocking {
    val result = repo.getFeed()
    assertTrue(result.isEmpty())
  }

  @Test
  fun addPost_thenGetFeed_returnsAscendingTimestampOrder() = runBlocking {
    val morningPost = samplePost("morning-fit", ts = 2L)
    val earlyPost = samplePost("early-fit", ts = 1L)
    val eveningPost = samplePost("evening-fit", ts = 3L)

    repo.addPost(morningPost)
    repo.addPost(earlyPost)
    repo.addPost(eveningPost)

    val result = repo.getFeed()
    assertEquals(listOf("early-fit", "morning-fit", "evening-fit"), result.map { it.postUID })
    assertEquals(listOf(1L, 2L, 3L), result.map { it.timestamp })
  }

  @Test
  fun addPost_writesDocumentWithGivenId() = runBlocking {
    val post = samplePost("explicit-fit-01", ts = 42L)
    repo.addPost(post)

    val doc = db.collection(POSTS_COLLECTION_PATH).document("explicit-fit-01").get().await()
    assertTrue(doc.exists())
    assertEquals(42L, doc.getLong("timestamp"))
  }

  @Test
  fun getFeed_withCorruptedDocument_returnsEmptyList_dueToCatchAll() = runBlocking {
    val validPost = samplePost("valid-fit", ts = 1L)
    repo.addPost(validPost)

    db.collection(POSTS_COLLECTION_PATH)
        .document("corrupted-fit")
        .set(mapOf("postUID" to "corrupted-fit", "timestamp" to "not-a-number"))
        .await()

    val result = repo.getFeed()
    assertTrue(result.isEmpty())
  }

  @Test
  fun getFeed_onNetworkFailure_returnsEmptyList() = runBlocking {
    val invalidDb =
        firestoreForApp(appName = "feed-repo-unreachable", host = "10.0.2.2", port = 6553)
    val unreachableRepo = FeedRepositoryFirestore(invalidDb)

    val result = unreachableRepo.getFeed()
    assertTrue(result.isEmpty())
  }

  @Test
  fun hasPostedToday_defaultFalse() = runBlocking { assertEquals(false, repo.hasPostedToday()) }

  @Test
  fun getNewPostId_isUniqueAndNonEmpty() {
    val ids = (1..200).map { repo.getNewPostId() }
    assertTrue(ids.all { it.isNotBlank() })
    assertEquals(ids.size, ids.toSet().size)
  }

  @Test
  fun checkPostData_returnsNullForInvalid_andSameForValid() = runBlocking {
    val invalidPost = samplePost("", ts = 1L)
    val validPost = samplePost("valid-check", ts = 1L)

    val method =
        FeedRepositoryFirestore::class
            .java
            .getDeclaredMethod("checkPostData", OutfitPost::class.java)
    method.isAccessible = true

    val invalidResult = method.invoke(repo, invalidPost) as OutfitPost?
    val validResult = method.invoke(repo, validPost) as OutfitPost?

    assertNull(invalidResult)
    assertEquals(validPost, validResult)
  }

  @Test
  fun transformPostDocument_handlesValidAndInvalid() = runBlocking {
    val goodPost = samplePost("transform-ok", ts = 5L)
    repo.addPost(goodPost)
    val validSnapshot = db.collection(POSTS_COLLECTION_PATH).document("transform-ok").get().await()

    db.collection(POSTS_COLLECTION_PATH).document("transform-bad").set(mapOf("random" to 1)).await()
    val invalidSnapshot =
        db.collection(POSTS_COLLECTION_PATH).document("transform-bad").get().await()

    val method =
        FeedRepositoryFirestore::class
            .java
            .getDeclaredMethod("transformPostDocument", DocumentSnapshot::class.java)
    method.isAccessible = true

    val okResult = method.invoke(repo, validSnapshot) as OutfitPost?
    val badResult = method.invoke(repo, invalidSnapshot) as OutfitPost?

    assertNotNull(okResult)
    assertNull(badResult)
  }

  @Test
  fun postsCollectionConstant_isCorrect() {
    assertEquals("posts", POSTS_COLLECTION_PATH)
  }

  // Helper Methods

  private suspend fun clearPosts() {
    val docs = db.collection(POSTS_COLLECTION_PATH).get().await().documents
    docs.forEach { it.reference.delete().await() }
  }

  private fun samplePost(id: String, ts: Long) =
      OutfitPost(
          postUID = id,
          name = "User Outfit $id",
          uid = "user-$id",
          userProfilePicURL = "https://example.com/users/$id.png",
          outfitURL = "https://example.com/outfits/$id.jpg",
          description = "Sample outfit description for $id",
          itemsID = listOf("top-$id", "bottom-$id"),
          timestamp = ts)

  private fun firestoreForApp(appName: String, host: String, port: Int): FirebaseFirestore {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val defaultApp =
        FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(context)!!
    val app =
        try {
          FirebaseApp.getInstance(appName)
        } catch (_: IllegalStateException) {
          FirebaseApp.initializeApp(context, defaultApp.options, appName)!!
        }
    return FirebaseFirestore.getInstance(app).apply {
      useEmulator(host, port)
      firestoreSettings = firestoreSettings { isPersistenceEnabled = false }
    }
  }
}
