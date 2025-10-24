package com.android.ootd.model.post

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.OutfitPost
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

    val post =
        OutfitPost(
            postUID = postId,
            name = "Test User",
            uid = "uid123",
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

    val post =
        OutfitPost(
            postUID = postId,
            name = "Delete Test",
            uid = "uid456",
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

    val post1 =
        OutfitPost(
            postUID = postId,
            name = "User A",
            uid = "123",
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
    val post =
        OutfitPost(
            postUID = postId,
            name = "No Image User",
            uid = "uidNoImage",
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

    val post =
        OutfitPost(
            postUID = postId,
            name = "Integration Test User",
            uid = "uid_integration",
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
}
