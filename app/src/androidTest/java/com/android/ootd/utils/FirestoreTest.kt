package com.android.ootd.utils

import android.util.Log
import com.android.ootd.model.feed.POSTS_COLLECTION_PATH
import com.android.ootd.model.user.USER_COLLECTION_PATH
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before

open class FirestoreTest() : BaseTest() {

  suspend fun getUserCount(): Int {
    return FirebaseEmulator.firestore.collection(USER_COLLECTION_PATH).get().await().size()
  }

  private suspend fun clearTestCollection() {
    val users = FirebaseEmulator.firestore.collection(USER_COLLECTION_PATH).get().await()

    val batch = FirebaseEmulator.firestore.batch()
    users.documents.forEach { batch.delete(it.reference) }
    batch.commit().await()

    assert(getUserCount() == 0) {
      "Test collection is not empty after clearing, count: ${getUserCount()}"
    }
  }

  private suspend fun clearPosts() {
    // Only delete posts authored by the signed-in user to satisfy rules
    val currentUid = requireNotNull(FirebaseEmulator.auth.currentUser?.uid)
    val docs =
        FirebaseEmulator.firestore
            .collection(POSTS_COLLECTION_PATH)
            .whereEqualTo("uid", currentUid)
            .get()
            .await()
            .documents
    docs.forEach { it.reference.delete().await() }
  }

  @Before
  override fun setUp() {
    super.setUp()

    runTest {
      FirebaseEmulator.clearFirestoreEmulator()
      FirebaseEmulator.auth.signInAnonymously().await()
      val userCount = getUserCount()
      if (userCount > 0) {
        Log.w(
            "FirebaseEmulatedTest",
            "Warning: Test collection is not empty at the beginning of the test, count: $userCount",
        )
        clearTestCollection()
        clearPosts()
      }
    }
  }

  @After
  override fun tearDown() {
    FirebaseEmulator.clearFirestoreEmulator()
    super.tearDown()
  }
}
