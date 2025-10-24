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

  @Before
  override fun setUp() {
    super.setUp()

    runTest {
      FirebaseEmulator.clearFirestoreEmulator()
      FirebaseEmulator.auth.signInAnonymously().await()
    }
  }

  @After
  override fun tearDown() {
    FirebaseEmulator.clearFirestoreEmulator()
    super.tearDown()
  }
}
