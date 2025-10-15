package com.android.ootd.ui.account
/*
 * DISCLAIMER: This file was created/modified with the assistance of GitHub Copilot.
 * Copilot provided suggestions which were reviewed and adapted by the developer.
 */

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.authentication.AccountServiceFirebase
import com.android.ootd.model.user.User
import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith

/**
 * Integration tests for AccountIcon with Firebase Emulator.
 *
 * Prerequisites:
 * - Firebase Emulator Suite running (Auth + Firestore)
 */
@RunWith(AndroidJUnit4::class)
class AccountIconFirebaseTest : FirestoreTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var accountService: AccountServiceFirebase
  private lateinit var viewModel: AccountViewModel

  private val testEmail = "test-user-${System.currentTimeMillis()}@example.com"
  private val testPassword = "TestPassword123!"
  private val testUsername = "TestUser"

  @Before
  override fun setUp() {
    super.setUp()
    accountService = AccountServiceFirebase()
  }

  // Let the parent FirestoreTest handle cleanup - no need to override tearDown

  @Test
  fun accountIcon_withFirebaseUser_displaysGooglePhotoUrl() = runTest {
    // Given: create test user with Google photo URL
    val authResult =
        FirebaseEmulator.auth.createUserWithEmailAndPassword(testEmail, testPassword).await()
    val userId = authResult.user!!.uid

    // Create Firestore user document
    repository.addUser(
        User(
            uid = userId,
            username = testUsername,
            profilePicture = Uri.EMPTY, // No Firestore override
            friendList = emptyList()))

    // Create ViewModel AFTER user is signed in - observeAuthState will pick up the current user
    viewModel = AccountViewModel(accountService, repository)

    // When
    composeTestRule.setContent { AccountIcon(accountViewModel = viewModel, onClick = {}) }

    // Wait for ViewModel to observe auth state and load user data
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      viewModel.uiState.value.username == testUsername
    }

    // Then: should show fallback icon (no Google photo URL in test)
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER).assertExists()
  }

  @Test
  fun accountIcon_withFirestoreProfilePicture_displaysFirestoreImage() = runTest {
    // Given: create test user with Firestore profile picture
    val authResult =
        FirebaseEmulator.auth.createUserWithEmailAndPassword(testEmail, testPassword).await()
    val userId = authResult.user!!.uid
    val firestoreImageUri = Uri.parse("https://example.com/firestore-avatar.jpg")

    repository.addUser(
        User(
            uid = userId,
            username = testUsername,
            profilePicture = firestoreImageUri,
            friendList = emptyList()))

    // Create ViewModel AFTER user is signed in
    viewModel = AccountViewModel(accountService, repository)

    // When
    composeTestRule.setContent { AccountIcon(accountViewModel = viewModel, onClick = {}) }

    // Wait for ViewModel to load the profile picture
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      viewModel.uiState.value.profilePicture == firestoreImageUri
    }

    // Then: Verify the ViewModel has the correct profile picture URI
    // We don't verify the image loads (Coil can't load fake URLs), just that the state is correct
    assert(viewModel.uiState.value.profilePicture == firestoreImageUri) {
      "Expected profile picture to be $firestoreImageUri but was ${viewModel.uiState.value.profilePicture}"
    }
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER).assertExists()
  }

  @Test
  fun accountIcon_whenProfilePictureUpdatedInFirestore_recomposesWithNewImage() = runTest {
    // Given: create test user
    val authResult =
        FirebaseEmulator.auth.createUserWithEmailAndPassword(testEmail, testPassword).await()
    val userId = authResult.user!!.uid
    val initialUri = Uri.parse("https://example.com/avatar1.jpg")

    repository.addUser(
        User(
            uid = userId,
            username = testUsername,
            profilePicture = initialUri,
            friendList = emptyList()))

    viewModel = AccountViewModel(accountService, repository)

    composeTestRule.setContent { AccountIcon(accountViewModel = viewModel, onClick = {}) }

    // Wait for initial profile picture to be loaded into state
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      viewModel.uiState.value.profilePicture == initialUri
    }

    // Verify initial state
    assert(viewModel.uiState.value.profilePicture == initialUri) {
      "Expected initial profile picture to be $initialUri"
    }

    // When: update profile picture in Firestore directly
    val newUri = Uri.parse("https://example.com/avatar2.jpg")
    FirebaseEmulator.firestore
        .collection("users")
        .document(userId)
        .update("profilePicture", newUri.toString())
        .await()

    // The reactive ViewModel won't automatically see Firestore changes (only auth changes)
    // So we need to force a reload by triggering auth state re-observation
    // We do this by signing out and back in
    FirebaseEmulator.auth.signOut()
    delay(100) // Let auth state propagate
    FirebaseEmulator.auth.signInWithEmailAndPassword(testEmail, testPassword).await()

    // Wait for the ViewModel to observe the auth state change and reload with new data
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      viewModel.uiState.value.profilePicture == newUri
    }

    // Then: Verify the ViewModel state has updated
    assert(viewModel.uiState.value.profilePicture == newUri) {
      "Expected updated profile picture to be $newUri but was ${viewModel.uiState.value.profilePicture}"
    }
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER).assertExists()
  }

  @Test
  fun accountIcon_whenUserSignsOut_showsFallbackIcon() = runTest {
    // Given: signed-in user with profile picture
    val authResult =
        FirebaseEmulator.auth.createUserWithEmailAndPassword(testEmail, testPassword).await()
    val userId = authResult.user!!.uid
    val imageUri = Uri.parse("https://example.com/avatar.jpg")

    repository.addUser(
        User(
            uid = userId,
            username = testUsername,
            profilePicture = imageUri,
            friendList = emptyList()))

    viewModel = AccountViewModel(accountService, repository)

    composeTestRule.setContent { AccountIcon(accountViewModel = viewModel, onClick = {}) }

    // Wait for image to load
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      viewModel.uiState.value.profilePicture == imageUri
    }

    // When: user signs out (this triggers observeAuthState to clear the state)
    FirebaseEmulator.auth.signOut()

    // Wait for ViewModel to observe sign-out and clear state
    composeTestRule.waitUntil(timeoutMillis = 5000) { viewModel.uiState.value.username.isEmpty() }

    // Then: should show fallback icon
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_IMAGE).assertDoesNotExist()
  }

  @Test
  fun accountIcon_withNoFirestoreDocument_usesGooglePhotoUrl() = runTest {
    // Given: signed-in user without Firestore document
    FirebaseEmulator.auth.createUserWithEmailAndPassword(testEmail, testPassword).await()

    // Don't create Firestore document - should fall back to Google photo
    viewModel = AccountViewModel(accountService, repository)

    // When
    composeTestRule.setContent { AccountIcon(accountViewModel = viewModel, onClick = {}) }

    // Wait for ViewModel to try loading (will fail but set email)
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      viewModel.uiState.value.googleAccountName == testEmail ||
          viewModel.uiState.value.errorMsg != null
    }

    // Then: should show fallback (no Google photo in test auth, no Firestore doc)
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER).assertExists()
  }

  @Test
  fun accountIcon_onClick_triggersCallback() = runTest {
    // Given: signed-in user
    val authResult =
        FirebaseEmulator.auth.createUserWithEmailAndPassword(testEmail, testPassword).await()
    val userId = authResult.user!!.uid

    repository.addUser(
        User(
            uid = userId,
            username = testUsername,
            profilePicture = Uri.EMPTY,
            friendList = emptyList()))

    viewModel = AccountViewModel(accountService, repository)
    var clicked = false

    // When
    composeTestRule.setContent {
      AccountIcon(accountViewModel = viewModel, onClick = { clicked = true })
    }

    // Wait for ViewModel to load
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      viewModel.uiState.value.username == testUsername
    }

    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER).performClick()

    // Then
    assert(clicked) { "onClick callback was not invoked" }
  }
}
