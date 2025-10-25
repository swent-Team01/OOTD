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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class OutfitPostRepositoryFirestoreTest : FirestoreTest() {

  @Before
  override fun setUp() {
    super.setUp()
    Assume.assumeTrue("Firebase Emulator must be running before tests.", FirebaseEmulator.isRunning)
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
        .set(
            mapOf("uid" to currentUid, "username" to "tester", "friendUids" to emptyList<String>()))
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
    Assert.assertNotNull(outfitPostRepository.getPostById(postId))

    outfitPostRepository.deletePost(postId)

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

  @Test
  fun deletePost_ignoresMissingImage() = runTest {
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
    outfitPostRepository.deletePost(postId)

    val deleted = outfitPostRepository.getPostById(postId)
    Assert.assertNull(deleted)
  }

  @Test
  fun getPostById_handlesMalformedData() = runTest {
    val postId = outfitPostRepository.getNewPostId()

    FirebaseEmulator.firestore
        .collection(POSTS_COLLECTION)
        .document(postId)
        .set(mapOf("unexpectedField" to 123))
        .await()

    val result = outfitPostRepository.getPostById(postId)
    Assert.assertNotNull(result)
    Assert.assertEquals("", result?.name)
  }

  @Test
  fun fullPostLifecycle_worksCorrectly() = runTest {
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
            name = "Integration Test User",
            ownerId = currentUid,
            userProfilePicURL = "https://fake.com/user.jpg",
            outfitURL = "https://fake.com/outfit.jpg",
            description = "Lifecycle test",
            timestamp = System.currentTimeMillis())

    outfitPostRepository.savePostToFirestore(post)
    val added = outfitPostRepository.getPostById(postId)
    Assert.assertNotNull(added)

    outfitPostRepository.deletePost(postId)
    val deleted = outfitPostRepository.getPostById(postId)
    Assert.assertNull(deleted)
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
        outfitPostRepository.savePartialPost(
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

  fun uploadOutfitPhoto_returnsValidDowwnloadUrl() = runTest {
    val postId = outfitPostRepository.getNewPostId()
    val fakeUri = "file:///tmp/fake_upload_image.jpg"

    // Just call the method — emulator will handle upload
    val url = outfitPostRepository.uploadOutfitPhoto(fakeUri, postId)

    Assert.assertTrue("Returned URL should not be blank", url.isNotBlank())
    Assert.assertTrue("URL should include the post ID", url.contains(postId))
  }

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
}
