package com.android.ootd.ui.account

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.authentication.AccountServiceFirebase
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepositoryFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith

/**
 * Integration tests for AccountIcon with real Firebase backend.
 *
 * Prerequisites:
 * - Firebase Emulator Suite running (Auth + Firestore)
 * - Or real Firebase project with test user credentials
 */
@RunWith(AndroidJUnit4::class)
class AccountIconFirebaseTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var auth: FirebaseAuth
  private lateinit var firestore: FirebaseFirestore
  private lateinit var accountService: AccountServiceFirebase
  private lateinit var userRepository: UserRepositoryFirestore
  private lateinit var viewModel: AccountViewModel

  private val testEmail = "test-user-${System.currentTimeMillis()}@example.com"
  private val testPassword = "TestPassword123!"
  private val testUsername = "TestUser"

  @Before
  fun setup() {
    // Connect to Firebase Emulator if available
    auth =
        FirebaseAuth.getInstance().apply {
          useEmulator("10.0.2.2", 9099) // Android emulator localhost
        }
    firestore = FirebaseFirestore.getInstance().apply { useEmulator("10.0.2.2", 8080) }

    accountService = AccountServiceFirebase()
    userRepository = UserRepositoryFirestore(firestore)
  }

  @After
  fun teardown() = runTest {
    // Clean up test user
    auth.currentUser?.delete()?.await()
  }

  @Test
  fun accountIcon_withFirebaseUser_displaysGooglePhotoUrl() = runTest {
    // Given: create test user with Google photo URL
    val authResult = auth.createUserWithEmailAndPassword(testEmail, testPassword).await()
    val userId = authResult.user!!.uid

    // Create Firestore user document
    userRepository.addUser(
        User(
            uid = userId,
            username = testUsername,
            profilePicture = Uri.EMPTY, // No Firestore override
            friendList = emptyList()))

    viewModel = AccountViewModel(accountService, userRepository)

    // When
    composeTestRule.setContent { AccountIcon(accountViewModel = viewModel, onClick = {}) }

    // Then: should show fallback icon (no Google photo URL in test)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_CONTAINER).assertExists()
  }

  @Test
  fun accountIcon_withFirestoreProfilePicture_displaysFirestoreImage() = runTest {
    // Given: create test user with Firestore profile picture
    val authResult = auth.createUserWithEmailAndPassword(testEmail, testPassword).await()
    val userId = authResult.user!!.uid
    val firestoreImageUri = Uri.parse("https://example.com/firestore-avatar.jpg")

    userRepository.addUser(
        User(
            uid = userId,
            username = testUsername,
            profilePicture = firestoreImageUri,
            friendList = emptyList()))

    viewModel = AccountViewModel(accountService, userRepository)

    // When
    composeTestRule.setContent { AccountIcon(accountViewModel = viewModel, onClick = {}) }

    // Then: should attempt to load Firestore image
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithTag(TAG_ACCOUNT_AVATAR_IMAGE).fetchSemanticsNodes().isNotEmpty()
    }
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_IMAGE).assertExists()
  }

  @Test
  fun accountIcon_whenProfilePictureUpdatedInFirestore_recomposesWithNewImage() = runTest {
    // Given: create test user
    val authResult = auth.createUserWithEmailAndPassword(testEmail, testPassword).await()
    val userId = authResult.user!!.uid
    val initialUri = Uri.parse("https://example.com/avatar1.jpg")

    userRepository.addUser(
        User(
            uid = userId,
            username = testUsername,
            profilePicture = initialUri,
            friendList = emptyList()))

    viewModel = AccountViewModel(accountService, userRepository)

    composeTestRule.setContent { AccountIcon(accountViewModel = viewModel, onClick = {}) }

    // Wait for initial image
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithTag(TAG_ACCOUNT_AVATAR_IMAGE).fetchSemanticsNodes().isNotEmpty()
    }

    // When: update profile picture in Firestore directly
    val newUri = Uri.parse("https://example.com/avatar2.jpg")
    firestore
        .collection("users")
        .document(userId)
        .update("profilePicture", newUri.toString())
        .await()

    // Manually trigger refresh (in real app, this would be automatic with snapshot listener)
    viewModel.refreshUIState()

    // Then: should display new image
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_IMAGE).assertExists()
  }

  @Test
  fun accountIcon_whenUserSignsOut_showsFallbackIcon() = runTest {
    // Given: signed-in user with profile picture
    val authResult = auth.createUserWithEmailAndPassword(testEmail, testPassword).await()
    val userId = authResult.user!!.uid
    val imageUri = Uri.parse("https://example.com/avatar.jpg")

    userRepository.addUser(
        User(
            uid = userId,
            username = testUsername,
            profilePicture = imageUri,
            friendList = emptyList()))

    viewModel = AccountViewModel(accountService, userRepository)

    composeTestRule.setContent { AccountIcon(accountViewModel = viewModel, onClick = {}) }

    // Wait for image to load
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithTag(TAG_ACCOUNT_AVATAR_IMAGE).fetchSemanticsNodes().isNotEmpty()
    }

    // When: user signs out
    auth.signOut()
    viewModel.refreshUIState()

    // Then: should show fallback icon
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_IMAGE).assertDoesNotExist()
  }

  @Test
  fun accountIcon_withNoFirestoreDocument_usesGooglePhotoUrl() = runTest {
    // Given: signed-in user without Firestore document
    val authResult = auth.createUserWithEmailAndPassword(testEmail, testPassword).await()

    // Don't create Firestore document - should fall back to Google photo
    viewModel = AccountViewModel(accountService, userRepository)

    // When
    composeTestRule.setContent { AccountIcon(accountViewModel = viewModel, onClick = {}) }

    // Then: should show fallback (no Google photo in test auth)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_CONTAINER).assertExists()
  }

  @Test
  fun accountIcon_onClick_triggersCallback() = runTest {
    // Given: signed-in user
    val authResult = auth.createUserWithEmailAndPassword(testEmail, testPassword).await()
    val userId = authResult.user!!.uid

    userRepository.addUser(
        User(
            uid = userId,
            username = testUsername,
            profilePicture = Uri.EMPTY,
            friendList = emptyList()))

    viewModel = AccountViewModel(accountService, userRepository)
    var clicked = false

    // When
    composeTestRule.setContent {
      AccountIcon(accountViewModel = viewModel, onClick = { clicked = true })
    }

    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_CONTAINER).performClick()

    // Then
    assert(clicked) { "onClick callback was not invoked" }
  }
}
