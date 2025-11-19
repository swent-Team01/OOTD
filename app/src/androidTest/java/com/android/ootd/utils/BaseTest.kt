package com.android.ootd.utils

import NotificationRepository
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryFirestore
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.feed.FeedRepositoryFirestore
import com.android.ootd.model.feed.FeedRepositoryProvider
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.ItemsRepositoryFirestore
import com.android.ootd.model.notifications.NotificationRepositoryFirestore
import com.android.ootd.model.post.OutfitPostRepository
import com.android.ootd.model.post.OutfitPostRepositoryFirestore
import com.android.ootd.model.post.OutfitPostRepositoryProvider
import com.android.ootd.model.posts.LikesFirestoreRepository
import com.android.ootd.model.posts.LikesRepository
import com.android.ootd.model.posts.LikesRepositoryProvider
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import com.android.ootd.model.user.UserRepositoryFirestore
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

  val userRepository: UserRepository
    get() = UserRepositoryProvider.repository

  val itemsRepository: ItemsRepository
    get() = ItemsRepositoryFirestore(FirebaseEmulator.firestore)

  val feedRepository: FeedRepository
    get() = FeedRepositoryProvider.repository

  val likesRepository: LikesRepository
    get() = LikesRepositoryProvider.repository

  lateinit var accountRepository: AccountRepository
  lateinit var notificationsRepository: NotificationRepository
  val outfitPostRepository: OutfitPostRepository
    get() = OutfitPostRepositoryProvider.repository

  val currentUser: FirebaseUser
    get() {
      return FirebaseEmulator.auth.currentUser!!
    }

  init {
    assert(FirebaseEmulator.isRunning) { "FirebaseEmulator must be running" }
  }

  open val user1 = User(uid = "0", username = "Hank", profilePicture = "Hank.jpg")

  open val user2 = User(uid = "1", username = "John", profilePicture = "John.jpg")

  @Before
  open fun setUp() {
    accountRepository = AccountRepositoryFirestore(db = FirebaseEmulator.firestore)
    notificationsRepository = NotificationRepositoryFirestore(db = FirebaseEmulator.firestore)
    UserRepositoryProvider.repository = UserRepositoryFirestore(FirebaseEmulator.firestore)
    FeedRepositoryProvider.repository = FeedRepositoryFirestore(FirebaseEmulator.firestore)
    LikesRepositoryProvider.repository = LikesFirestoreRepository(FirebaseEmulator.firestore)
    OutfitPostRepositoryProvider.repository =
        OutfitPostRepositoryFirestore(FirebaseEmulator.firestore, FirebaseEmulator.storage)
    UserRepositoryProvider.repository = UserRepositoryFirestore(FirebaseEmulator.firestore)
  }

  @After
  open fun tearDown() {
    if (FirebaseEmulator.isRunning) {
      FirebaseEmulator.auth.signOut()
      FirebaseEmulator.clearAuthEmulator()
    }
  }
}
