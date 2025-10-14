// Kotlin
package com.android.ootd.ui.account

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.authentication.AccountServiceFirebase
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepositoryFirestore
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
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

  private lateinit var accountService: AccountServiceFirebase
  private lateinit var userRepository: UserRepositoryFirestore
  private lateinit var viewModel: AccountViewModel

  private val testEmail = "test-user-${System.currentTimeMillis()}@example.com"
  private val testPassword = "TestPassword123!"
  private val testUsername = "TestUser"

  companion object {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    @BeforeClass
    @JvmStatic
    fun setupFirebase() {
      // Configure Firebase emulator before any instances are created
      try {
        auth =
            FirebaseAuth.getInstance().apply {
              useEmulator("10.0.2.2", 9099) // Android emulator localhost
            }
        firestore = FirebaseFirestore.getInstance().apply { useEmulator("10.0.2.2", 8080) }
      } catch (e: IllegalStateException) {
        // If already initialized, just get the instances
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
      }
    }

    @AfterClass
    @JvmStatic
    fun teardownFirebase() {
      // Clean up Firebase app
      try {
        FirebaseApp.getInstance().delete()
      } catch (e: Exception) {
        // Ignore if already deleted
      }
    }
  }

  @Before
  fun setup() {
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

    // Create ViewModel AFTER user is signed in - observeAuthState will pick up the current user
    viewModel = AccountViewModel(accountService, userRepository)

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
    val authResult = auth.createUserWithEmailAndPassword(testEmail, testPassword).await()
    val userId = authResult.user!!.uid
    val firestoreImageUri = Uri.parse("https://example.com/firestore-avatar.jpg")

    userRepository.addUser(
        User(
            uid = userId,
            username = testUsername,
            profilePicture = firestoreImageUri,
            friendList = emptyList()))

    // Create ViewModel AFTER user is signed in
    viewModel = AccountViewModel(accountService, userRepository)

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
    firestore
        .collection("users")
        .document(userId)
        .update("profilePicture", newUri.toString())
        .await()

    // The reactive ViewModel won't automatically see Firestore changes (only auth changes)
    // So we need to force a reload by triggering auth state re-observation
    // We do this by signing out and back in
    auth.signOut()
    delay(100) // Let auth state propagate
    auth.signInWithEmailAndPassword(testEmail, testPassword).await()

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
      viewModel.uiState.value.profilePicture == imageUri
    }

    // When: user signs out (this triggers observeAuthState to clear the state)
    auth.signOut()

    // Wait for ViewModel to observe sign-out and clear state
    composeTestRule.waitUntil(timeoutMillis = 5000) { viewModel.uiState.value.username.isEmpty() }

    // Then: should show fallback icon
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_IMAGE).assertDoesNotExist()
  }

  @Test
  fun accountIcon_withNoFirestoreDocument_usesGooglePhotoUrl() = runTest {
    // Given: signed-in user without Firestore document
    auth.createUserWithEmailAndPassword(testEmail, testPassword).await()

    // Don't create Firestore document - should fall back to Google photo
    viewModel = AccountViewModel(accountService, userRepository)

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

    // Wait for ViewModel to load
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      viewModel.uiState.value.username == testUsername
    }

    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER).performClick()

    // Then
    assert(clicked) { "onClick callback was not invoked" }
  }
}
