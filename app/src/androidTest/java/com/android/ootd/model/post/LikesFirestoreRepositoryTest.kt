package com.android.ootd.model.posts

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LikesFirestoreRepositoryTest : FirestoreTest() {

  private lateinit var repo: LikesRepository

  @Before
  override fun setUp() {
    super.setUp()
    repo = likesRepository
  }

  private suspend fun createPost(ownerId: String): String {
    val postId = outfitPostRepository.getNewPostId()

    val post =
        OutfitPost(
            postUID = postId,
            ownerId = ownerId,
            name = "TestUser",
            userProfilePicURL = "",
            outfitURL = "fake.jpg",
            description = "",
            itemsID = emptyList(),
            timestamp = System.currentTimeMillis())

    outfitPostRepository.savePostToFirestore(post)
    return postId
  }

  @Test
  fun likePost_createsDocument() = runTest {
    FirebaseEmulator.auth.signInAnonymously().await()
    val user = FirebaseEmulator.auth.currentUser!!.uid

    // Only create a post — no accounts needed
    val postId = createPost(user)

    val like = Like(postId = postId, likerUserId = user, timestamp = System.currentTimeMillis())

    repo.likePost(like)

    val snap =
        FirebaseEmulator.firestore
            .collection(LIKE_COLLECTION_PATH)
            .document(postId)
            .collection("users")
            .document(user)
            .get()
            .await()

    assertTrue(snap.exists())
  }

  @Test
  fun unlikePost_deletesDocument() = runTest {
    FirebaseEmulator.auth.signInAnonymously().await()
    val user = FirebaseEmulator.auth.currentUser!!.uid

    val postId = createPost(user)
    repo.likePost(Like(postId, user, System.currentTimeMillis()))
    repo.unlikePost(postId, user)

    val snap =
        FirebaseEmulator.firestore
            .collection(LIKE_COLLECTION_PATH)
            .document(postId)
            .collection("users")
            .document(user)
            .get()
            .await()

    assertFalse(snap.exists())
  }

  @Test
  fun isPostLikedByUser_returnsCorrectState() = runTest {
    FirebaseEmulator.auth.signInAnonymously().await()
    val user = FirebaseEmulator.auth.currentUser!!.uid

    val postId = createPost(user)

    assertFalse(repo.isPostLikedByUser(postId, user))

    repo.likePost(Like(postId, user, System.currentTimeMillis()))

    assertTrue(repo.isPostLikedByUser(postId, user))
  }

  @Test
  fun getLikeCount_returnsCorrectValue() = runTest {
    FirebaseEmulator.auth.signInAnonymously().await()
    val user = FirebaseEmulator.auth.currentUser!!.uid

    val postId = createPost(user)

    assertEquals(0, repo.getLikeCount(postId))

    repo.likePost(Like(postId, user, System.currentTimeMillis()))

    assertEquals(1, repo.getLikeCount(postId))
  }
}
