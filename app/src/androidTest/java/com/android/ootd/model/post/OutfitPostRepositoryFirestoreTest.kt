package com.android.ootd.model.post

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.OutfitPost
import com.android.ootd.utils.FirebaseEmulator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class OutfitPostRepositoryFirestoreTest {

  private lateinit var repo: OutfitPostRepositoryFirestore
  private lateinit var firestore: FirebaseFirestore
  private lateinit var storage: FirebaseStorage

  @Before
  fun setup() {
    Assume.assumeTrue("Firebase Emulator must be running before tests.", FirebaseEmulator.isRunning)

    firestore = FirebaseEmulator.firestore
    storage = FirebaseEmulator.storage

    // Clean up previous data (optional)
    FirebaseEmulator.clearFirestoreEmulator()

    repo = OutfitPostRepositoryFirestore(firestore, storage)
  }

  @Test
  fun addAndRetrievePost_worksCorrectly() = runTest {
    val postId = repo.getNewPostId()

    val post =
        OutfitPost(
            postUID = postId,
            name = "Test User",
            uid = "uid123",
            userProfilePicURL = Uri.parse("https://fake.com/profile.jpg"),
            outfitURL = Uri.parse("https://fake.com/outfit.jpg"),
            description = "Cool outfit",
            itemsID = listOf("item1", "item2"),
            timestamp = System.currentTimeMillis())

    repo.addPost(post)

    val fetched = repo.getPostById(postId)

    Assert.assertNotNull(fetched)
    Assert.assertEquals(post.name, fetched?.name)
    Assert.assertEquals(post.description, fetched?.description)
    Assert.assertEquals(post.itemsID, fetched?.itemsID)
  }

  @Test
  fun deletePost_removesPostFromFirestore() = runTest {
    val postId = repo.getNewPostId()

    val post =
        OutfitPost(
            postUID = postId,
            name = "Delete Test",
            uid = "uid456",
            userProfilePicURL = Uri.parse("https://fake.com/pic.jpg"),
            outfitURL = Uri.parse("https://fake.com/outfit.jpg"),
            description = "To be deleted",
            timestamp = System.currentTimeMillis())

    repo.addPost(post)
    Assert.assertNotNull(repo.getPostById(postId))

    repo.deletePost(postId)

    val deleted = repo.getPostById(postId)
    Assert.assertNull(deleted)
  }

  @Test
  fun getNewPostId_returnsUniqueIds() {
    val id1 = repo.getNewPostId()
    val id2 = repo.getNewPostId()
    Assert.assertNotEquals(id1, id2)
    Assert.assertTrue(id1.isNotBlank())
    Assert.assertTrue(id2.isNotBlank())
  }

  @Test
  fun addPost_overwritesExistingDocument() = runTest {
    val postId = repo.getNewPostId()

    val post1 =
        OutfitPost(
            postUID = postId,
            name = "User A",
            uid = "123",
            userProfilePicURL = Uri.parse("https://example.com/a.jpg"),
            outfitURL = Uri.parse("https://example.com/outfit1.jpg"),
            description = "First",
            timestamp = 1L)

    val post2 = post1.copy(name = "User B", description = "Updated")

    repo.addPost(post1)
    repo.addPost(post2)

    val fetched = repo.getPostById(postId)
    Assert.assertEquals("User B", fetched?.name)
    Assert.assertEquals("Updated", fetched?.description)
  }

  @Test
  fun getPostById_returnsNullForNonExistent() = runTest {
    val result = repo.getPostById("does_not_exist")
    Assert.assertNull(result)
  }

  @Test
  fun deletePost_ignoresMissingImage() = runTest {
    val postId = repo.getNewPostId()
    val post =
        OutfitPost(
            postUID = postId,
            name = "No Image User",
            uid = "uidNoImage",
            userProfilePicURL = Uri.EMPTY,
            outfitURL = Uri.EMPTY,
            description = "No image to delete",
            timestamp = System.currentTimeMillis())

    repo.addPost(post)
    repo.deletePost(postId) // Should log a warning but not throw

    val deleted = repo.getPostById(postId)
    Assert.assertNull(deleted)
  }

  @Test
  fun getPostById_handlesMalformedData() = runTest {
    val postId = repo.getNewPostId()

    firestore
        .collection(POSTS_COLLECTION)
        .document(postId)
        .set(mapOf("unexpectedField" to 123))
        .await()

    val result = repo.getPostById(postId)
    Assert.assertNotNull(result)
    Assert.assertEquals("", result?.name)
  }

  @Test
  fun fullPostLifecycle_worksCorrectly() = runTest {
    val postId = repo.getNewPostId()

    val post =
        OutfitPost(
            postUID = postId,
            name = "Integration Test User",
            uid = "uid_integration",
            userProfilePicURL = Uri.parse("https://fake.com/user.jpg"),
            outfitURL = Uri.parse("https://fake.com/outfit.jpg"),
            description = "Lifecycle test",
            timestamp = System.currentTimeMillis())

    repo.addPost(post)
    val added = repo.getPostById(postId)
    Assert.assertNotNull(added)

    repo.deletePost(postId)
    val deleted = repo.getPostById(postId)
    Assert.assertNull(deleted)
  }

  @After
  fun tearDown() {
    // Optional cleanup between tests
    FirebaseEmulator.clearFirestoreEmulator()
  }
}
