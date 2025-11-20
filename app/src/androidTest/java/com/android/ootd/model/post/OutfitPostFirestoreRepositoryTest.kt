package com.android.ootd.model.post

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.emptyLocation
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

  // --- helpers to keep tests short ---
  private fun uid() = FirebaseEmulator.auth.currentUser!!.uid

  private fun newId() = outfitPostRepository.getNewPostId()

  private suspend fun ensureUser(
      id: String = uid(),
      username: String = "tester",
      friends: List<String> = emptyList()
  ) {
    FirebaseEmulator.firestore
        .collection("users")
        .document(id)
        .set(mapOf("uid" to id, "ownerId" to id, "username" to username, "friendUids" to friends))
        .await()
  }

  private fun post(
      id: String = newId(),
      name: String = "Test User",
      owner: String = uid(),
      profile: String = "https://fake.com/profile.jpg",
      outfit: String = "https://fake.com/outfit.jpg",
      description: String = "Cool outfit",
      items: List<String> = emptyList(),
      ts: Long = System.currentTimeMillis(),
      location: Location = emptyLocation
  ) =
      OutfitPost(
          postUID = id,
          name = name,
          ownerId = owner,
          userProfilePicURL = profile,
          outfitURL = outfit,
          description = description,
          itemsID = items,
          timestamp = ts,
          location = location)

  private suspend inline fun <reified T : Throwable> expectThrows(
      messageContains: String? = null,
      crossinline block: suspend () -> Unit
  ): T {
    val e = runCatching { block() }.exceptionOrNull()
    Assert.assertTrue("Expected ${T::class.java.simpleName}, was: $e", e is T)
    if (messageContains != null)
        Assert.assertTrue(e?.message.orEmpty().contains(messageContains, ignoreCase = true))
    return e as T
  }

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
    val newId = newId()
    val uid = uid()
    ensureUser(uid)

    val post = post(id = newId, owner = uid, items = listOf("item1", "item2"))
    outfitPostRepository.savePostToFirestore(post)

    val fetched = outfitPostRepository.getPostById(newId)
    Assert.assertNotNull(fetched)
    Assert.assertEquals(post.name, fetched?.name)
    Assert.assertEquals(post.description, fetched?.description)
    Assert.assertEquals(post.itemsID, fetched?.itemsID)
    Assert.assertEquals(post.location, fetched?.location)
  }

  @Test
  fun saveAndRetrievePost_withLocation_worksCorrectly() = runTest {
    val newId = newId()
    val uid = uid()
    ensureUser(uid)

    val location = Location(46.5197, 6.6323, "EPFL")
    val post = post(id = newId, owner = uid, location = location)
    outfitPostRepository.savePostToFirestore(post)

    val fetched = outfitPostRepository.getPostById(newId)
    Assert.assertNotNull(fetched)
    Assert.assertEquals(location.latitude, fetched?.location?.latitude)
    Assert.assertEquals(location.longitude, fetched?.location?.longitude)
    Assert.assertEquals(location.name, fetched?.location?.name)
  }

  @Test
  fun getPostById_withoutLocation_defaultsToEmptyLocation() = runTest {
    val newId = newId()

    // Manually create post without location field
    FirebaseEmulator.firestore
        .collection(POSTS_COLLECTION)
        .document(newId)
        .set(
            mapOf(
                "postUID" to newId,
                "name" to "Old Post",
                "ownerId" to ownerId,
                "userProfilePicURL" to "",
                "outfitURL" to "",
                "description" to "",
                "itemsID" to emptyList<String>(),
                "timestamp" to 0L
                // No location field
                ))
        .await()

    val fetched = outfitPostRepository.getPostById(newId)
    Assert.assertNotNull(fetched)
    Assert.assertEquals(emptyLocation, fetched?.location)
  }

  @Test
  fun deletePost_removesPostFromFirestore() = runTest {
    val newId = newId()
    ensureUser()

    val post = post(id = newId, name = "Delete Test", description = "To be deleted")
    outfitPostRepository.savePostToFirestore(post)

    try {
      outfitPostRepository.deletePost(newId)
    } catch (e: Exception) {
      if (!e.message.orEmpty().contains("Object does not exist")) throw e
    }

    val deleted = outfitPostRepository.getPostById(newId)
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
    val newId = newId()
    ensureUser()

    val location1 = Location(46.5197, 6.6323, "EPFL")
    val location2 = Location(46.2044, 6.1432, "Geneva")

    val post1 =
        post(
            id = newId,
            name = "User A",
            profile = "https://example.com/a.jpg",
            outfit = "https://example.com/outfit1.jpg",
            description = "First",
            ts = 1L,
            location = location1)
    val post2 = post1.copy(name = "User B", description = "Updated", location = location2)

    outfitPostRepository.savePostToFirestore(post1)
    outfitPostRepository.savePostToFirestore(post2)

    val fetched = outfitPostRepository.getPostById(newId)
    Assert.assertEquals("User B", fetched?.name)
    Assert.assertEquals("Updated", fetched?.description)
    Assert.assertEquals(location2, fetched?.location)
  }

  @Test
  fun getPostById_returnsNullForNonExistent() = runTest {
    val result = outfitPostRepository.getPostById("does_not_exist")
    Assert.assertNull(result)
  }

  @Test
  fun deletePost_throwsWhenImageMissing() = runTest {
    val newId = newId()
    ensureUser()

    val post = post(id = newId, name = "No Image User", profile = "", outfit = "")
    outfitPostRepository.savePostToFirestore(post)

    expectThrows<Exception> { outfitPostRepository.deletePost(newId) }
  }

  @Test
  fun getPostById_handlesMalformedData() = runTest {
    val newId = newId()

    FirebaseEmulator.firestore
        .collection(POSTS_COLLECTION)
        .document(newId)
        .set(mapOf("unexpectedField" to 123, "ownerId" to ownerId))
        .await()

    val result = outfitPostRepository.getPostById(newId)
    Assert.assertNotNull(result)
    Assert.assertEquals("", result?.name)
    Assert.assertEquals(emptyLocation, result?.location)
  }

  @Test
  fun fullPostLifecycle_worksCorrectly() = runTest {
    val newId = newId()
    ensureUser()

    val location = Location(46.5197, 6.6323, "EPFL")
    val post =
        post(
            id = newId,
            name = "Integration Test User",
            profile = "https://fake.com/user.jpg",
            outfit = "https://fake.com/outfit.jpg",
            description = "Lifecycle test",
            location = location)

    outfitPostRepository.savePostToFirestore(post)

    val added = outfitPostRepository.getPostById(newId)
    Assert.assertNotNull("Post should be saved and retrievable", added)
    Assert.assertEquals(post.name, added?.name)
    Assert.assertEquals(post.description, added?.description)
    Assert.assertEquals(location, added?.location)

    try {
      outfitPostRepository.deletePost(newId)
    } catch (e: Exception) {
      val message = e.message.orEmpty().lowercase()
      if (!message.contains("object") || !message.contains("exist")) throw e
    }

    val deleted = outfitPostRepository.getPostById(newId)
    Assert.assertNull("Post should be deleted from Firestore", deleted)
  }

  @Test
  fun savePartialPost_createsPostWithUploadedPhoto() = runTest {
    val uid = uid()
    ensureUser(uid)

    val fakeUri = "file:///tmp/fake_photo.jpg"
    val location = Location(46.5197, 6.6323, "EPFL")

    val saved =
        outfitPostRepository.savePostWithMainPhoto(
            uid = uid,
            name = "Partial Save Tester",
            userProfilePicURL = "https://example.com/profile.jpg",
            localPath = fakeUri,
            description = "Test post with photo",
            location = location)

    val fetched = outfitPostRepository.getPostById(saved.postUID)
    Assert.assertNotNull(fetched)
    Assert.assertEquals("Partial Save Tester", fetched!!.name)
    Assert.assertEquals("Test post with photo", fetched.description)
    Assert.assertTrue(fetched.outfitURL.isNotBlank())
    Assert.assertEquals(location, fetched.location)
  }

  @Test
  fun uploadOutfitPhoto_returnsValidDownloadUrl() = runTest {
    val id = newId()
    val fakeUri = "file:///tmp/fake_upload_image.jpg"
    val url = outfitPostRepository.uploadOutfitPhoto(fakeUri, id)

    Assert.assertTrue("Returned URL should not be blank", url.isNotBlank())
    Assert.assertTrue("URL should include the post ID", url.contains(id))
  }

  @Test
  fun updatePostFields_updatesDescriptionSuccessfully() = runTest {
    val uid = uid()
    ensureUser(uid)

    val id = newId()
    val post =
        post(
            id = id,
            name = "Updater",
            owner = uid,
            profile = "https://example.com/pic.jpg",
            outfit = "https://example.com/outfit.jpg",
            description = "Old description")
    outfitPostRepository.savePostToFirestore(post)

    outfitPostRepository.updatePostFields(id, mapOf("description" to "New description"))

    val updated = outfitPostRepository.getPostById(id)
    Assert.assertNotNull(updated)
    Assert.assertEquals("New description", updated!!.description)
  }

  @Test
  fun savePostToFirestore_logsAndThrowsOnFailure() = runTest {
    val badPost =
        OutfitPost(
            postUID = "bad/post/path", // Firestore rejects '/' in document IDs
            name = "Invalid",
            ownerId = ownerId,
            userProfilePicURL = "",
            outfitURL = "",
            description = "This will fail",
            itemsID = emptyList(),
            timestamp = System.currentTimeMillis(),
            location = emptyLocation)

    expectThrows<Exception> { outfitPostRepository.savePostToFirestore(badPost) }
  }

  @Test
  fun uploadOutfitPhoto_returnsFakeUrlOnFailure() = runTest {
    val id = newId()
    val fakeBadPath = "not_a_valid_uri"

    val url = outfitPostRepository.uploadOutfitPhoto(fakeBadPath, id)

    Assert.assertTrue(url.contains("https://fake.storage/"))
    Assert.assertTrue(url.contains(id))
  }

  @Test
  fun savePostWithMainPhoto_usesFallbackUrlOnUploadFailure() = runTest {
    val uid = uid()

    val invalidPath = "not_a_valid_uri"
    val location = Location(46.5197, 6.6323, "EPFL")

    val saved =
        outfitPostRepository.savePostWithMainPhoto(
            uid = uid,
            name = "Fallback Upload",
            userProfilePicURL = "https://example.com/profile.jpg",
            localPath = invalidPath,
            description = "Fallback triggered",
            location = location)

    Assert.assertTrue(saved.outfitURL.startsWith("https://fake.storage/"))
    Assert.assertTrue(saved.outfitURL.contains(saved.postUID))
    Assert.assertEquals(location, saved.location)

    val fetched = outfitPostRepository.getPostById(saved.postUID)
    Assert.assertNotNull(fetched)
    Assert.assertEquals("Fallback Upload", fetched?.name)
    Assert.assertEquals(location, fetched?.location)
  }
}
