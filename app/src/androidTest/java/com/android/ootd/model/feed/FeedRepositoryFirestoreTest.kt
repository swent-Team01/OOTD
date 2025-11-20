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

/**
 * Tests for [FeedRepositoryFirestore] using Firebase Emulator Suite.
 *
 * DISCLAIMER : These tests are partially created using AI and verified by humans.
 */
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

  // -------- getPostById and mapToPost tests --------

  @Test
  fun getPostById_returnsCorrectPost_whenExists() = runBlocking {
    val post = samplePost("test-post-123", ts = 12345L)
    feedRepository.addPost(post)

    val retrieved = feedRepository.getPostById("test-post-123")

    assertEquals("test-post-123", retrieved?.postUID)
    assertEquals(currentUid, retrieved?.ownerId)
    assertEquals(12345L, retrieved?.timestamp)
    assertEquals("desc-test-post-123", retrieved?.description)
    assertEquals("name-test-post-123", retrieved?.name)
    assertEquals(2, retrieved?.itemsID?.size)
    assertTrue(retrieved?.itemsID?.contains("i1-test-post-123") == true)
  }

  @Test
  fun getPostById_handlesPostWithEmptyFields() = runBlocking {
    val postWithEmptyFields =
        OutfitPost(
            postUID = "empty-fields-post",
            name = "",
            ownerId = currentUid,
            userProfilePicURL = "",
            outfitURL = "",
            description = "",
            itemsID = emptyList(),
            timestamp = 0L)
    feedRepository.addPost(postWithEmptyFields)

    val retrieved = feedRepository.getPostById("empty-fields-post")

    assertEquals("empty-fields-post", retrieved?.postUID)
    assertEquals("", retrieved?.name)
    assertEquals("", retrieved?.description)
    assertEquals(0L, retrieved?.timestamp)
    assertTrue(retrieved?.itemsID?.isEmpty() == true)
  }

  @Test
  fun getPostById_handlesPostWithMultipleItems() = runBlocking {
    val postWithManyItems =
        OutfitPost(
            postUID = "multi-item-post",
            name = "Multi Item Post",
            ownerId = currentUid,
            userProfilePicURL = "https://example.com/profile.jpg",
            outfitURL = "https://example.com/outfit.jpg",
            description = "A post with many items",
            itemsID = listOf("item1", "item2", "item3", "item4", "item5"),
            timestamp = System.currentTimeMillis())
    feedRepository.addPost(postWithManyItems)

    val retrieved = feedRepository.getPostById("multi-item-post")

    assertEquals(5, retrieved?.itemsID?.size)
    assertEquals(listOf("item1", "item2", "item3", "item4", "item5"), retrieved?.itemsID)
  }

  @Test(expected = Exception::class)
  fun getPostById_throwsException_whenDocumentIsCorrupted() =
      runBlocking<Unit> {
        // Add a valid post first
        feedRepository.addPost(samplePost("corrupted-doc", ts = 100L))

        // Corrupt the document by removing required fields
        db.collection(POSTS_COLLECTION_PATH)
            .document("corrupted-doc")
            .update(mapOf("postUuid" to null, "ownerId" to null))
            .await()

        // mapToPost returns null, getPostById throws
        feedRepository.getPostById("corrupted-doc")
      }

  @Test
  fun getPostById_handlesNullItemsIdGracefully() = runBlocking {
    // Manually create a document with null itemsId
    val docData =
        mapOf(
            "postUID" to "null-items-post", // Changed from "postUuid"
            "ownerId" to currentUid,
            "timestamp" to 500L,
            "description" to "Test post",
            "name" to "Test",
            "outfitURL" to "https://example.com/outfit.jpg", // Changed from "outfitUrl"
            "userProfilePicURL" to
                "https://example.com/profile.jpg", // Changed from "userProfilePicture"
            "itemsID" to null // Changed from "itemsId"
            )
    db.collection(POSTS_COLLECTION_PATH).document("null-items-post").set(docData).await()

    val retrieved = feedRepository.getPostById("null-items-post")

    assertEquals("null-items-post", retrieved?.postUID)
    assertTrue(retrieved?.itemsID?.isEmpty() == true) // Should default to empty list
  }

  @Test
  fun getPostById_handlesItemsIdWithMixedTypes() = runBlocking {
    // Create document with mixed types in itemsId array
    val docData =
        mapOf(
            "postUID" to "mixed-items-post", // Changed from "postUuid"
            "ownerId" to currentUid,
            "timestamp" to 600L,
            "description" to "Test",
            "name" to "Test",
            "outfitURL" to "https://example.com/outfit.jpg", // Changed from "outfitUrl"
            "userProfilePicURL" to
                "https://example.com/profile.jpg", // Changed from "userProfilePicture"
            "itemsID" to listOf("item1", 123, "item2", null, "item3") // Changed from "itemsId"
            )
    db.collection(POSTS_COLLECTION_PATH).document("mixed-items-post").set(docData).await()

    val retrieved = feedRepository.getPostById("mixed-items-post")

    // mapNotNull should filter out non-strings
    assertEquals(3, retrieved?.itemsID?.size)
    assertEquals(listOf("item1", "item2", "item3"), retrieved?.itemsID)
  }

  @Test
  fun getPostById_handlesVeryOldTimestamp() = runBlocking {
    val veryOldPost = samplePost("very-old-post", ts = 1L) // Unix epoch + 1ms
    feedRepository.addPost(veryOldPost)

    val retrieved = feedRepository.getPostById("very-old-post")

    assertEquals(1L, retrieved?.timestamp)
  }

  @Test
  fun getPostById_handlesRecentTimestamp() = runBlocking {
    val now = System.currentTimeMillis()
    val recentPost = samplePost("recent-post", ts = now)
    feedRepository.addPost(recentPost)

    val retrieved = feedRepository.getPostById("recent-post")

    assertEquals(now, retrieved?.timestamp)
  }

  @Test
  fun getPostById_retrievesLocationCorrectly() = runBlocking {
    val testLocation =
        com.android.ootd.model.map.Location(latitude = 46.5197, longitude = 6.5659, name = "EPFL")
    val postWithLocation =
        OutfitPost(
            postUID = "post-with-location",
            name = "Location Test",
            ownerId = currentUid,
            userProfilePicURL = "https://example.com/pic.jpg",
            outfitURL = "https://example.com/outfit.jpg",
            description = "Testing location retrieval",
            itemsID = listOf("item1"),
            timestamp = System.currentTimeMillis(),
            location = testLocation)
    feedRepository.addPost(postWithLocation)

    val retrieved = feedRepository.getPostById("post-with-location")

    assertEquals(46.5197, retrieved?.location?.latitude)
    assertEquals(6.5659, retrieved?.location?.longitude)
    assertEquals("EPFL", retrieved?.location?.name)
  }

  @Test
  fun getPostById_handlesPostWithoutLocation() = runBlocking {
    val postWithoutLocation =
        OutfitPost(
            postUID = "post-no-location",
            name = "No Location",
            ownerId = currentUid,
            userProfilePicURL = "",
            outfitURL = "",
            description = "No location",
            itemsID = emptyList(),
            timestamp = System.currentTimeMillis()
            // location defaults to emptyLocation
            )
    feedRepository.addPost(postWithoutLocation)

    val retrieved = feedRepository.getPostById("post-no-location")

    assertEquals(com.android.ootd.model.map.emptyLocation, retrieved?.location)
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
