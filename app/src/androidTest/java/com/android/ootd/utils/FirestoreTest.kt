package com.android.ootd.utils

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

  @Before
  override fun setUp() {
    super.setUp()

    runTest {
      FirebaseEmulator.clearFirestoreEmulator()
      FirebaseEmulator.auth.signInAnonymously().await()
      FirebaseEmulator.firestore.collection("users").get().await()
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
