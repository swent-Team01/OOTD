package com.android.ootd.model.post

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith

/** ACKNOWLEDGEMENT: These tests were generated with the help of AI and verified by human */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class OutfitPostRepositoryFirestoreTest : FirestoreTest() {
  var ownerId = ""

  @Before
  override fun setUp() {
    super.setUp()
    Assume.assumeTrue("Firebase Emulator must be running before tests.", FirebaseEmulator.isRunning)
    ownerId = FirebaseEmulator.auth.uid ?: ""
    if (ownerId == "") {
      throw IllegalStateException("There needs to be an authenticated user")
    }
  }

  @Test
  fun saveAndRetrievePost_worksCorrectly() = runTest {
    val postId = outfitPostRepository.getNewPostId()
    val currentUid = FirebaseEmulator.auth.currentUser!!.uid

    // Create user document since the rules require it
    FirebaseEmulator.firestore
        .collection("users")
        .document(currentUid)
        .set(
            mapOf("uid" to currentUid, "username" to "tester", "friendUids" to emptyList<String>()))
        .await()

    val post =
        OutfitPost(
            postUID = postId,
            name = "Test User",
            ownerId = currentUid,
            userProfilePicURL = "https://fake.com/profile.jpg",
            outfitURL = "https://fake.com/outfit.jpg",
            description = "Cool outfit",
            itemsID = listOf("item1", "item2"),
            timestamp = System.currentTimeMillis())

    outfitPostRepository.savePostToFirestore(post)

    val fetched = outfitPostRepository.getPostById(postId)
    Assert.assertNotNull(fetched)
    Assert.assertEquals(post.name, fetched?.name)
    Assert.assertEquals(post.description, fetched?.description)
    Assert.assertEquals(post.itemsID, fetched?.itemsID)
  }

  @Test
  fun deletePost_removesPostFromFirestore() = runTest {
    val postId = outfitPostRepository.getNewPostId()
    val currentUid = FirebaseEmulator.auth.currentUser!!.uid

    FirebaseEmulator.firestore
        .collection("users")
        .document(currentUid)
        .set(mapOf("uid" to currentUid, "username" to "tester"))
        .await()

    val post =
        OutfitPost(
            postUID = postId,
            name = "Delete Test",
            ownerId = currentUid,
            userProfilePicURL = "https://fake.com/pic.jpg",
            outfitURL = "https://fake.com/outfit.jpg",
            description = "To be deleted",
            timestamp = System.currentTimeMillis())

    outfitPostRepository.savePostToFirestore(post)

    try {
      outfitPostRepository.deletePost(postId)
    } catch (e: Exception) {
      // Ignore if it's just "object not found"
      if (!e.message.orEmpty().contains("Object does not exist")) throw e
    }

    val deleted = outfitPostRepository.getPostById(postId)
    Assert.assertNull(deleted)
  }

  @Test
  fun getNewPostId_returnsUniqueIds() {
    val id1 = outfitPostRepository.getNewPostId()
    val id2 = outfitPostRepository.getNewPostId()
    Assert.assertNotEquals(id1, id2)
    Assert.assertTrue(id1.isNotBlank())
    Assert.assertTrue(id2.isNotBlank())
  }

  @Test
  fun savePostToFirestore_overwritesExistingDocument() = runTest {
    val postId = outfitPostRepository.getNewPostId()

    val currentUid = FirebaseEmulator.auth.currentUser!!.uid

    FirebaseEmulator.firestore
        .collection("users")
        .document(currentUid)
        .set(
            mapOf("uid" to currentUid, "username" to "tester", "friendUids" to emptyList<String>()))
        .await()

    val post1 =
        OutfitPost(
            postUID = postId,
            name = "User A",
            ownerId = currentUid,
            userProfilePicURL = "https://example.com/a.jpg",
            outfitURL = "https://example.com/outfit1.jpg",
            description = "First",
            timestamp = 1L)

    val post2 = post1.copy(name = "User B", description = "Updated")

    outfitPostRepository.savePostToFirestore(post1)
    outfitPostRepository.savePostToFirestore(post2)

    val fetched = outfitPostRepository.getPostById(postId)
    Assert.assertEquals("User B", fetched?.name)
    Assert.assertEquals("Updated", fetched?.description)
  }

  @Test
  fun getPostById_returnsNullForNonExistent() = runTest {
    val result = outfitPostRepository.getPostById("does_not_exist")
    Assert.assertNull(result)
  }

  @Test(expected = Exception::class)
  fun deletePost_throwsWhenImageMissing() = runTest {
    val postId = outfitPostRepository.getNewPostId()
    val currentUid = FirebaseEmulator.auth.currentUser!!.uid

    FirebaseEmulator.firestore
        .collection("users")
        .document(currentUid)
        .set(
            mapOf("uid" to currentUid, "username" to "tester", "friendUids" to emptyList<String>()))
        .await()

    val post =
        OutfitPost(
            postUID = postId,
            name = "No Image User",
            ownerId = currentUid,
            userProfilePicURL = "",
            outfitURL = "",
            description = "No image to delete",
            timestamp = System.currentTimeMillis())

    outfitPostRepository.savePostToFirestore(post)

    // This should throw, since the image doesn't exist
    outfitPostRepository.deletePost(postId)
  }

  @Test
  fun getPostById_handlesMalformedData() = runTest {
    val postId = outfitPostRepository.getNewPostId()

    FirebaseEmulator.firestore
        .collection(POSTS_COLLECTION)
        .document(postId)
        .set(mapOf("unexpectedField" to 123, "ownerId" to ownerId))
        .await()

    val result = outfitPostRepository.getPostById(postId)
    Assert.assertNotNull(result)
    Assert.assertEquals("", result?.name)
  }

  @Test
  fun fullPostLifecycle_worksCorrectly() = runTest {
    val postId = outfitPostRepository.getNewPostId()
    val currentUid = FirebaseEmulator.auth.currentUser!!.uid

    // Ensure user document exists for Firestore security rules
    FirebaseEmulator.firestore
        .collection("users")
        .document(currentUid)
        .set(
            mapOf("uid" to currentUid, "username" to "tester", "friendUids" to emptyList<String>()))
        .await()

    // Create and save a post
    val post =
        OutfitPost(
            postUID = postId,
            name = "Integration Test User",
            ownerId = currentUid,
            userProfilePicURL = "https://fake.com/user.jpg",
            outfitURL = "https://fake.com/outfit.jpg",
            description = "Lifecycle test",
            timestamp = System.currentTimeMillis())

    outfitPostRepository.savePostToFirestore(post)

    // Verify it was saved correctly
    val added = outfitPostRepository.getPostById(postId)
    Assert.assertNotNull("Post should be saved and retrievable", added)
    Assert.assertEquals(post.name, added?.name)
    Assert.assertEquals(post.description, added?.description)

    // Try to delete the post; tolerate missing image errors
    try {
      outfitPostRepository.deletePost(postId)
    } catch (e: Exception) {
      // Ignore common emulator "object not found" errors
      val message = e.message.orEmpty().lowercase()
      if (!message.contains("object") || !message.contains("exist")) {
        throw e // rethrow anything unexpected
      }
    }

    // Verify that Firestore document is gone
    val deleted = outfitPostRepository.getPostById(postId)
    Assert.assertNull("Post should be deleted from Firestore", deleted)
  }

  @Test
  fun savePartialPost_createsPostWithUploadedPhoto() = runTest {
    val currentUid = FirebaseEmulator.auth.currentUser!!.uid

    // Ensure user doc exists
    FirebaseEmulator.firestore
        .collection("users")
        .document(currentUid)
        .set(
            mapOf("uid" to currentUid, "username" to "tester", "friendUids" to emptyList<String>()))
        .await()

    // Fake URI for upload; since emulator doesn’t check for file existence,
    // we can use any valid URI format.
    val fakeUri = "file:///tmp/fake_photo.jpg"

    val post =
        outfitPostRepository.savePostWithMainPhoto(
            uid = currentUid,
            name = "Partial Save Tester",
            userProfilePicURL = "https://example.com/profile.jpg",
            localPath = fakeUri,
            description = "Test post with photo")

    // Assert Firestore document exists
    val fetched = outfitPostRepository.getPostById(post.postUID)
    Assert.assertNotNull(fetched)
    Assert.assertEquals("Partial Save Tester", fetched!!.name)
    Assert.assertEquals("Test post with photo", fetched.description)
    Assert.assertTrue(fetched.outfitURL.isNotBlank())
  }

  @Test
  fun uploadOutfitPhoto_returnsValidDownloadUrl() = runTest {
    val postId = outfitPostRepository.getNewPostId()
    val fakeUri = "file:///tmp/fake_upload_image.jpg"
    val url = outfitPostRepository.uploadOutfitPhoto(fakeUri, postId)

    Assert.assertTrue("Returned URL should not be blank", url.isNotBlank())
    Assert.assertTrue("URL should include the post ID", url.contains(postId))
  }

  @Test
  fun updatePostFields_updatesDescriptionSuccessfully() = runTest {
    val currentUid = FirebaseEmulator.auth.currentUser!!.uid
    FirebaseEmulator.firestore
        .collection("users")
        .document(currentUid)
        .set(
            mapOf("uid" to currentUid, "username" to "tester", "friendUids" to emptyList<String>()))
        .await()

    // Create a base post
    val postId = outfitPostRepository.getNewPostId()
    val post =
        OutfitPost(
            postUID = postId,
            name = "Updater",
            ownerId = currentUid,
            userProfilePicURL = "https://example.com/pic.jpg",
            outfitURL = "https://example.com/outfit.jpg",
            description = "Old description",
            timestamp = System.currentTimeMillis())
    outfitPostRepository.savePostToFirestore(post)

    // Update description only
    outfitPostRepository.updatePostFields(postId, mapOf("description" to "New description"))

    val updated = outfitPostRepository.getPostById(postId)
    Assert.assertNotNull(updated)
    Assert.assertEquals("New description", updated!!.description)
  }

  @Test(expected = Exception::class)
  fun savePostToFirestore_logsAndThrowsOnFailure() = runTest {
    val post =
        OutfitPost(
            postUID = "bad/post/path", // Firestore rejects '/' in document IDs
            name = "Invalid",
            ownerId = ownerId,
            userProfilePicURL = "",
            outfitURL = "",
            description = "This will fail",
            itemsID = emptyList(),
            timestamp = System.currentTimeMillis())

    try {
      outfitPostRepository.savePostToFirestore(post)
    } catch (e: Exception) {
      // We expect it to throw after logging
      Assert.assertTrue(e is Exception)
      throw e
    }
  }

  @Test
  fun uploadOutfitPhoto_returnsFakeUrlOnFailure() = runTest {
    val postId = outfitPostRepository.getNewPostId()

    // This path cannot be uploaded (invalid URI format)
    val fakeBadPath = "not_a_valid_uri"

    val url = outfitPostRepository.uploadOutfitPhoto(fakeBadPath, postId)

    // It should not throw, but return the fallback fake URL
    Assert.assertTrue(url.contains("https://fake.storage/"))
    Assert.assertTrue(url.contains(postId))
  }

  @Test
  fun savePostWithMainPhoto_usesFallbackUrlOnUploadFailure() = runTest {
    val currentUid = FirebaseEmulator.auth.currentUser!!.uid

    // Invalid path will make upload fail
    val invalidPath = "not_a_valid_uri"

    val post =
        outfitPostRepository.savePostWithMainPhoto(
            uid = currentUid,
            name = "Fallback Upload",
            userProfilePicURL = "https://example.com/profile.jpg",
            localPath = invalidPath,
            description = "Fallback triggered")

    Assert.assertTrue(post.outfitURL.startsWith("https://fake.storage/"))
    Assert.assertTrue(post.outfitURL.contains(post.postUID))

    val fetched = outfitPostRepository.getPostById(post.postUID)
    Assert.assertNotNull(fetched)
    Assert.assertEquals("Fallback Upload", fetched?.name)
  }
}
