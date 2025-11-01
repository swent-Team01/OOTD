package com.android.ootd.utils

import com.android.ootd.model.account.ACCOUNT_COLLECTION_PATH
import com.android.ootd.model.account.Account
import com.android.ootd.model.map.Location
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before

open class AccountFirestoreTest : FirestoreTest() {

  // Test date of birth constant
  val testDateOfBirth = "2000-01-01"

  val testEmail = "123@example.com"

  val EPFL_LOCATION =
      Location(46.5191, 6.5668, "École Polytechnique Fédérale de Lausanne (EPFL), Switzerland")
  lateinit var account1: Account
  lateinit var account2: Account

  @Before
  override fun setUp() {
    super.setUp()

    account1 =
        Account(
            uid = "user1",
            ownerId = currentUser.uid,
            username = "alice_wonder",
            birthday = "1990-01-01",
            googleAccountEmail = "alice@example.com",
            profilePicture = "",
            friendUids = emptyList())

    account2 =
        Account(
            uid = "user2",
            ownerId = currentUser.uid,
            username = "bob_builder",
            birthday = "1992-05-15",
            googleAccountEmail = "bob@example.com",
            profilePicture = "",
            friendUids = emptyList())
    runTest { clearAccountCollection() }
  }

  suspend fun getAccountCount(): Int {
    return FirebaseEmulator.firestore
        .collection(ACCOUNT_COLLECTION_PATH)
        .whereEqualTo("ownerId", currentUser.uid)
        .get()
        .await()
        .size()
  }

  private suspend fun clearAccountCollection() {
    val accounts =
        FirebaseEmulator.firestore
            .collection(ACCOUNT_COLLECTION_PATH)
            .whereEqualTo("ownerId", currentUser.uid)
            .get()
            .await()

    val batch = FirebaseEmulator.firestore.batch()
    accounts.documents.forEach { batch.delete(it.reference) }
    batch.commit().await()

    assert(getAccountCount() == 0) {
      "Account test collection is not empty after clearing, count: ${getAccountCount()}"
    }
  }

  @After
  override fun tearDown() {
    runTest { clearAccountCollection() }
    super.tearDown()
  }
}
