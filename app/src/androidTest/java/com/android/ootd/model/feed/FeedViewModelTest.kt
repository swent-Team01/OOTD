package com.android.ootd.ui.feed

import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepositoryFirestore
import com.android.ootd.model.feed.FeedRepositoryFirestore
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import com.google.firebase.auth.FirebaseAuth
import junit.framework.TestCase.*
import kotlin.text.get
import kotlin.text.set
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Before
import org.junit.Test

/**
 * Connected (emulator-backed) tests for FeedViewModel.
 *
 * These use the Firebase emulator to verify correct Firestore and Auth behavior.
 */
class FeedViewModelFirebaseTest : FirestoreTest() {

  private lateinit var viewModel: FeedViewModel
  private lateinit var auth: FirebaseAuth
  private lateinit var accountRepo: AccountRepositoryFirestore
  private lateinit var feedRepo: FeedRepositoryFirestore

  @Before
  override fun setUp() {
    super.setUp()
    auth = FirebaseEmulator.auth
    accountRepo = AccountRepositoryFirestore(FirebaseEmulator.firestore)
    feedRepo = FeedRepositoryFirestore(FirebaseEmulator.firestore)

    runBlocking { auth.signInAnonymously().await() }

    viewModel = FeedViewModel(feedRepo, accountRepo)
  }

  @Test
  fun feedLoadsAccountAndEmptyPostsInitially() = runBlocking {
    val uid = auth.currentUser!!.uid
    val account = Account(uid, uid, "bob", friendUids = emptyList())
    FirebaseEmulator.firestore.collection("accounts").document(uid).set(account).await()

    // Give the listener a moment to propagate
    delay(500)

    viewModel.refreshFeedFromFirestore()
    delay(500)

    val state = viewModel.uiState.first()
    assertNotNull(state.currentAccount)
    assertEquals("bob", state.currentAccount?.username)
    assertTrue(state.feedPosts.isEmpty())
  }

  @Test
  fun refreshFeed_populatesPostsFromFriends() = runBlocking {
    val uid = auth.currentUser!!.uid

    // Create account with one friend (self or other)
    val account = Account(uid, uid, "bob", friendUids = listOf(uid))
    FirebaseEmulator.firestore.collection("accounts").document(uid).set(account).await()

    // Create a post visible to this user
    val post =
        OutfitPost(
            postUID = "p1",
            ownerId = uid,
            name = "bob",
            description = "today’s outfit",
            outfitURL = "https://example.com/fake.jpg",
            timestamp = System.currentTimeMillis())
    FirebaseEmulator.firestore.collection("posts").document(post.postUID).set(post).await()

    viewModel.refreshFeedFromFirestore()
    delay(1000)

    val state = viewModel.uiState.first()
    assertTrue(state.feedPosts.isNotEmpty())
    assertEquals("today’s outfit", state.feedPosts.first().description)
    assertEquals(uid, state.feedPosts.first().ownerId)
  }

  @Test
  fun feedClearsWhenUserLogsOut() = runBlocking {
    val uid = auth.currentUser!!.uid
    FirebaseEmulator.firestore
        .collection("accounts")
        .document(uid)
        .set(Account(uid, uid, "bob"))
        .await()

    delay(500)
    auth.signOut()
    delay(500)

    val state = viewModel.uiState.first()
    assertNull(state.currentAccount)
    assertTrue(state.feedPosts.isEmpty())
  }

  @Test
  fun refresh_setsHasPostedToday_whenUserPostedToday() = runBlocking {
    val uid = auth.currentUser!!.uid
    val account = Account(uid, uid, "bob", friendUids = emptyList())
    FirebaseEmulator.firestore.collection("accounts").document(uid).set(account).await()

    val post =
        OutfitPost(
            postUID = "p_today",
            ownerId = uid,
            name = "bob",
            description = "today’s outfit",
            outfitURL = "https://example.com/fake.jpg",
            timestamp = System.currentTimeMillis())
    FirebaseEmulator.firestore.collection("posts").document(post.postUID).set(post).await()

    // allow listeners/emulator to settle
    delay(700)
    viewModel.refreshFeedFromFirestore()
    delay(1000)

    val state = viewModel.uiState.first()
    assertTrue(state.hasPostedToday)
    assertTrue(state.feedPosts.any { it.postUID == post.postUID })
  }

  @Test
  fun refresh_noop_whenNoCurrentAccount() = runBlocking {
    // sign out to clear currentAccount
    auth.signOut()
    delay(500)

    // should not throw and should remain cleared
    viewModel.refreshFeedFromFirestore()
    delay(500)

    val state = viewModel.uiState.first()
    assertNull(state.currentAccount)
    assertTrue(state.feedPosts.isEmpty())
  }

  @Test
  fun observeAuth_loadsAccountOnSignIn() = runBlocking {
    // sign out then sign in again to trigger auth listener
    auth.signOut()
    delay(500)
    auth.signInAnonymously().await()

    val uid = auth.currentUser!!.uid
    val account = Account(uid, uid, "carol", friendUids = emptyList())
    FirebaseEmulator.firestore.collection("accounts").document(uid).set(account).await()

    // allow listener to pick up the account
    delay(700)

    val state = viewModel.uiState.first()
    assertNotNull(state.currentAccount)
    assertEquals("carol", state.currentAccount?.username)
  }
}
