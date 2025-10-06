package com.android.ootd.utils

import android.util.Log
import com.android.ootd.model.user.USER_COLLECTION_PATH
import com.android.ootd.model.user.UserRepository
import com.android.ootd.model.user.UserRepositoryFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before

open class FirestoreTest() : BaseTest() {

  suspend fun getUserCount(): Int {
    return FirebaseEmulator.firestore.collection(USER_COLLECTION_PATH).get().await().size()
  }

  private suspend fun clearTestCollection() {
    val user = FirebaseEmulator.auth.currentUser ?: return
    val users =
        FirebaseEmulator.firestore
            .collection(USER_COLLECTION_PATH)
            .whereEqualTo("ownerId", user.uid)
            .get()
            .await()

    val batch = FirebaseEmulator.firestore.batch()
    users.documents.forEach { batch.delete(it.reference) }
    batch.commit().await()

    assert(getUserCount() == 0) {
      "Test collection is not empty after clearing, count: ${getUserCount()}"
    }
  }

  override fun createInitializedRepository(): UserRepository {
    return UserRepositoryFirestore(db = FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() {
    super.setUp()
    runTest {
      val userCount = getUserCount()
      if (userCount > 0) {
        Log.w(
            "FirebaseEmulatedTest",
            "Warning: Test collection is not empty at the beginning of the test, count: $userCount",
        )
        clearTestCollection()
      }
    }
  }

  @After
  override fun tearDown() {
    runTest { clearTestCollection() }
    FirebaseEmulator.clearFirestoreEmulator()
    super.tearDown()
  }
}
