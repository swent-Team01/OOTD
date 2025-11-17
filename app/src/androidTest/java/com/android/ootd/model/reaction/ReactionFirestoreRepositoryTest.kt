package com.android.ootd.model.reactions

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ReactionFirestoreRepositoryTest : FirestoreTest() {

  private lateinit var repo: ReactionFirestoreRepository

  private fun uid() = FirebaseEmulator.auth.currentUser!!.uid

  private fun newPostId() = FirebaseEmulator.firestore.collection("posts").document().id

  /** Creates a real temporary image file so Storage upload works on the emulator */
  private fun createTempImageUri(): String {
    val f = File.createTempFile("reaction_test_", ".jpg")
    f.writeBytes(ByteArray(256) { 0xAB.toByte() })
    return Uri.fromFile(f).toString()
  }

  /** Creates a post document so rules allow reactions */
  private suspend fun createPost(postId: String = newPostId(), ownerId: String = uid()): String {
    val data =
        mapOf(
            "postUID" to postId,
            "ownerId" to ownerId,
            "name" to "Tester",
            "userProfilePicURL" to "pic.jpg",
            "outfitURL" to "fit.jpg",
            "description" to "",
            "itemsID" to emptyList<String>(),
            "timestamp" to System.currentTimeMillis())
    FirebaseEmulator.firestore.collection("posts").document(postId).set(data).await()
    return postId
  }

  @Before
  override fun setUp() {
    super.setUp()
    repo = ReactionFirestoreRepository(FirebaseEmulator.firestore, FirebaseEmulator.storage)
  }

  @Test
  fun addOrReplaceReaction_createsReactionDocument() = runTest {
    val postId = createPost()
    val userId = uid()

    repo.addOrReplaceReaction(postId, userId, createTempImageUri())

    val snap =
        FirebaseEmulator.firestore
            .collection("reactions")
            .document(postId)
            .collection("users")
            .document(userId)
            .get()
            .await()

    Assert.assertTrue(snap.exists())
    Assert.assertEquals(postId, snap.getString("postId"))
    Assert.assertEquals(userId, snap.getString("ownerId"))
    Assert.assertNotNull(snap.getString("reactionURL"))
  }

  @Test
  fun addOrReplaceReaction_overwritesExistingReaction() = runTest {
    val postId = createPost()
    val userId = uid()

    repo.addOrReplaceReaction(postId, userId, createTempImageUri())
    val r1 = repo.getUserReaction(postId, userId)
    Assert.assertNotNull(r1)

    // Replace reaction
    Thread.sleep(50) // ensure file changes
    repo.addOrReplaceReaction(postId, userId, createTempImageUri())
    val r2 = repo.getUserReaction(postId, userId)

    Assert.assertNotNull(r2)
    Assert.assertNotEquals(r1!!.reactionURL, r2!!.reactionURL)
  }

  @Test
  fun getReaction_returnsNullIfNotExists() = runTest {
    val postId = createPost()
    val result = repo.getUserReaction(postId, uid())
    Assert.assertNull(result)
  }

  @Test
  fun deleteReaction_removesFirestoreDocAndImage() = runTest {
    val postId = createPost()
    val userId = uid()

    repo.addOrReplaceReaction(postId, userId, createTempImageUri())

    repo.deleteReaction(postId, userId)

    // Firestore doc deleted?
    val doc =
        FirebaseEmulator.firestore
            .collection("reactions")
            .document(postId)
            .collection("users")
            .document(userId)
            .get()
            .await()

    Assert.assertFalse(doc.exists())

    // Storage file deleted?
    val fileRef = FirebaseEmulator.storage.reference.child("images/reactions/$postId/$userId.jpg")

    val exists = runCatching { fileRef.downloadUrl.await() }.isSuccess
    Assert.assertFalse("Reaction image should be deleted", exists)
  }
}
