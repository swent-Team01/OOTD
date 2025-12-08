package com.android.ootd.utils

import NotificationRepository
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.AccountRepositoryFirestore
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.ai.AIModelProvider
import com.android.ootd.model.consent.ConsentRepository
import com.android.ootd.model.consent.ConsentRepositoryFirestore
import com.android.ootd.model.consent.ConsentRepositoryProvider
import com.android.ootd.model.feed.FeedRepository
import com.android.ootd.model.feed.FeedRepositoryFirestore
import com.android.ootd.model.feed.FeedRepositoryProvider
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.ItemsRepositoryFirestore
import com.android.ootd.model.items.ItemsRepositoryProvider
import com.android.ootd.model.notifications.NotificationRepositoryFirestore
import com.android.ootd.model.notifications.NotificationRepositoryProvider
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

  val accountRepository: AccountRepository
    get() = AccountRepositoryProvider.repository

  val notificationsRepository: NotificationRepository
    get() = NotificationRepositoryProvider.repository

  val outfitPostRepository: OutfitPostRepository
    get() = OutfitPostRepositoryProvider.repository

  val consentRepository: ConsentRepository
    get() = ConsentRepositoryProvider.repository

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
    AccountRepositoryProvider.repository =
        AccountRepositoryFirestore(db = FirebaseEmulator.firestore)
    NotificationRepositoryProvider.repository =
        NotificationRepositoryFirestore(db = FirebaseEmulator.firestore)
    UserRepositoryProvider.repository = UserRepositoryFirestore(FirebaseEmulator.firestore)
    FeedRepositoryProvider.repository = FeedRepositoryFirestore(FirebaseEmulator.firestore)
    LikesRepositoryProvider.repository = LikesFirestoreRepository(FirebaseEmulator.firestore)
    OutfitPostRepositoryProvider.repository =
        OutfitPostRepositoryFirestore(FirebaseEmulator.firestore, FirebaseEmulator.storage)
    UserRepositoryProvider.repository = UserRepositoryFirestore(FirebaseEmulator.firestore)
    ConsentRepositoryProvider.repository = ConsentRepositoryFirestore(FirebaseEmulator.firestore)
    ItemsRepositoryProvider.repository = ItemsRepositoryFirestore(FirebaseEmulator.firestore)
    AIModelProvider.useMock() // Use mock AI model to avoid Firebase AI DataStore issues
  }

  @After
  open fun tearDown() {
    if (FirebaseEmulator.isRunning) {
      FirebaseEmulator.auth.signOut()
      FirebaseEmulator.clearAuthEmulator()
    }
    AIModelProvider.reset() // Reset to default Firebase AI model
  }
}
