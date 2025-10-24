package com.android.ootd.utils

import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryFirestore
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import com.android.ootd.model.user.UserRepositoryProvider
import com.google.firebase.auth.FirebaseUser
import org.junit.After
import org.junit.Before

const val UI_WAIT_TIMEOUT = 5_000L

/**
 * Base class for all tests, providing common setup and utility functions.
 *
 * It also handles gracefully automatic sign-in and only runs firebase emulators.
 */
abstract class BaseTest() {

  abstract fun createInitializedRepository(): UserRepository

  val repository: UserRepository
    get() = UserRepositoryProvider.repository

  lateinit var accountRepository: AccountRepository

  val currentUser: FirebaseUser
    get() {
      return FirebaseEmulator.auth.currentUser!!
    }

  init {
    assert(FirebaseEmulator.isRunning) { "FirebaseEmulator must be running" }
  }

  open val user1 = User(uid = "0", username = "Hank")

  open val user2 = User(uid = "1", username = "John")

  @Before
  open fun setUp() {
    UserRepositoryProvider.repository = createInitializedRepository()
    accountRepository = AccountRepositoryFirestore(db = FirebaseEmulator.firestore)
  }

  @After
  open fun tearDown() {
    if (FirebaseEmulator.isRunning) {
      FirebaseEmulator.auth.signOut()
      FirebaseEmulator.clearAuthEmulator()
    }
  }
}
