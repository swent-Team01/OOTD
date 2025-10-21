package com.android.ootd.utils

import android.util.Log
import com.android.ootd.model.account.ACCOUNT_COLLECTION_PATH
import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before

open class AccountFirestoreTest : BaseTest() {

  lateinit var accountRepository: AccountRepository

  val account1 =
      Account(
          uid = "user1",
          ownerId = "user1",
          username = "alice_wonder",
          birthday = "1990-01-01",
          googleAccountEmail = "alice@example.com",
          profilePicture = "",
          friendUids = emptyList())

  val account2 =
      Account(
          uid = "user2",
          ownerId = "user2",
          username = "bob_builder",
          birthday = "1992-05-15",
          googleAccountEmail = "bob@example.com",
          profilePicture = "",
          friendUids = emptyList())

  suspend fun getAccountCount(): Int {
    return FirebaseEmulator.firestore.collection(ACCOUNT_COLLECTION_PATH).get().await().size()
  }

  private suspend fun clearAccountCollection() {
    val accounts = FirebaseEmulator.firestore.collection(ACCOUNT_COLLECTION_PATH).get().await()

    val batch = FirebaseEmulator.firestore.batch()
    accounts.documents.forEach { batch.delete(it.reference) }
    batch.commit().await()

    assert(getAccountCount() == 0) {
      "Account test collection is not empty after clearing, count: ${getAccountCount()}"
    }
  }

  override fun createInitializedRepository(): com.android.ootd.model.user.UserRepository {
    return com.android.ootd.model.user.UserRepositoryFirestore(db = FirebaseEmulator.firestore)
  }

  @Before
  override fun setUp() {
    super.setUp()
    accountRepository = AccountRepositoryFirestore(db = FirebaseEmulator.firestore)

    runTest {
      FirebaseEmulator.auth.signInAnonymously().await()
      val accountCount = getAccountCount()
      if (accountCount > 0) {
        Log.w(
            "AccountFirestoreTest",
            "Warning: Account test collection is not empty at the beginning of the test, count: $accountCount")
        clearAccountCollection()
      }
    }
  }

  @After
  override fun tearDown() {
    runTest { clearAccountCollection() }
    FirebaseEmulator.clearFirestoreEmulator()
    super.tearDown()
  }
}
