package com.android.ootd.ui.account

/*
 * DISCLAIMER: This file was created/modified with the assistance of GitHub Copilot.
 * Copilot provided suggestions which were reviewed and adapted by the developer.
 */

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.credentials.CredentialManager
import com.android.ootd.model.authentication.AccountServiceFirebase
import com.android.ootd.model.user.User
import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import io.mockk.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AccountScreenFirebaseTest : FirestoreTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockCredentialManager: CredentialManager
  private lateinit var accountService: AccountServiceFirebase
  private lateinit var viewModel: AccountViewModel

  private val testEmail = "test-user-${System.currentTimeMillis()}@example.com"
  private val testPassword = "TestPassword123!"
  private val testUsername = "TestUser"

  @Before
  override fun setUp() {
    super.setUp()
    mockCredentialManager = mockk(relaxed = true)
    accountService = AccountServiceFirebase()
  }

  @After
  override fun tearDown() {
    runTest {
      // Clean up test user
      FirebaseEmulator.auth.currentUser?.delete()?.await()
    }
    clearAllMocks()
    super.tearDown()
  }

  @Test
  fun accountScreen_displaysUserInformation_whenUserIsSignedIn() = runTest {
    // Given: create and sign in test user
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

    // When
    composeTestRule.setContent {
      AccountScreen(accountViewModel = viewModel, credentialManager = mockCredentialManager)
    }

    composeTestRule.waitForIdle()

    // Then
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(UiTestTags.TAG_USERNAME_FIELD).assertTextContains(testUsername)
    composeTestRule.onNodeWithTag(UiTestTags.TAG_GOOGLE_FIELD).assertTextContains(testEmail)
  }

  @Test
  fun accountScreen_displaysProfilePicture_whenAvailable() = runTest {
    // Given: create test user with profile picture
    val profileUri = Uri.parse("https://example.com/profile.jpg")
    val authResult =
        FirebaseEmulator.auth.createUserWithEmailAndPassword(testEmail, testPassword).await()
    val userId = authResult.user!!.uid

    repository.addUser(
        User(
            uid = userId,
            username = testUsername,
            profilePicture = profileUri,
            friendList = emptyList()))

    viewModel = AccountViewModel(accountService, repository)

    // When
    composeTestRule.setContent {
      AccountScreen(accountViewModel = viewModel, credentialManager = mockCredentialManager)
    }

    composeTestRule.waitForIdle()

    // Then
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_IMAGE).assertExists()
  }

  @Test
  fun accountScreen_displaysDefaultAvatar_whenNoProfilePicture() = runTest {
    // Given: create test user without profile picture
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

    // When
    composeTestRule.setContent {
      AccountScreen(accountViewModel = viewModel, credentialManager = mockCredentialManager)
    }

    composeTestRule.waitForIdle()

    // Then
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_IMAGE).assertDoesNotExist()
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER).assertExists()
  }

  @Test
  fun accountScreen_callsOnBack_whenBackButtonClicked() = runTest {
    // Given: create and sign in test user
    val onBack = mockk<() -> Unit>(relaxed = true)
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

    // When
    composeTestRule.setContent {
      AccountScreen(
          accountViewModel = viewModel, credentialManager = mockCredentialManager, onBack = onBack)
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_BACK).performClick()

    // Then
    verify { onBack() }
  }

  @Test
  fun accountScreen_callsOnEditAvatar_whenEditButtonClicked() = runTest {
    // Given: create and sign in test user
    val onEditAvatar = mockk<() -> Unit>(relaxed = true)
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

    // When
    composeTestRule.setContent {
      AccountScreen(
          accountViewModel = viewModel,
          credentialManager = mockCredentialManager,
          onEditAvatar = onEditAvatar)
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_EDIT).performClick()

    // Then
    verify { onEditAvatar() }
  }

  @Test
  fun accountScreen_signOut_triggersSignOutFlow() = runTest {
    // Given: create and sign in test user
    val onSignOut = mockk<() -> Unit>(relaxed = true)
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

    // When
    composeTestRule.setContent {
      AccountScreen(
          accountViewModel = viewModel,
          credentialManager = mockCredentialManager,
          onSignOut = onSignOut)
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(UiTestTags.TAG_SIGNOUT_BUTTON).performClick()

    // Wait for all pending coroutines and UI updates to complete
    composeTestRule.waitForIdle()

    // Wait until the signedOut state is updated
    composeTestRule.waitUntil(timeoutMillis = 5000) { viewModel.uiState.value.signedOut }

    // Then
    coVerify(timeout = 5000) { mockCredentialManager.clearCredentialState(any()) }
    verify(timeout = 5000) { onSignOut() }
  }

  @Test
  fun accountScreen_usesFirestoreProfilePicture_overGooglePhoto() = runTest {
    // Given: create test user with Firestore profile picture
    val firestorePhotoUri = Uri.parse("https://firestore.com/photo.jpg")
    val authResult =
        FirebaseEmulator.auth.createUserWithEmailAndPassword(testEmail, testPassword).await()
    val userId = authResult.user!!.uid

    repository.addUser(
        User(
            uid = userId,
            username = testUsername,
            profilePicture = firestorePhotoUri,
            friendList = emptyList()))

    viewModel = AccountViewModel(accountService, repository)

    // When
    composeTestRule.setContent {
      AccountScreen(accountViewModel = viewModel, credentialManager = mockCredentialManager)
    }

    composeTestRule.waitForIdle()

    // Then
    assert(viewModel.uiState.value.profilePicture == firestorePhotoUri)
  }
}
