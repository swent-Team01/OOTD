package com.android.ootd.ui.account

/*
 * DISCLAIMER: This file was created/modified with the assistance of GitHub Copilot.
 * Copilot provided suggestions which were reviewed and adapted by the developer.
 */

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.credentials.CredentialManager
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AccountScreenFirebaseTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockUserRepository: UserRepository
  private lateinit var mockCredentialManager: CredentialManager
  private lateinit var mockFirebaseUser: FirebaseUser
  private lateinit var viewModel: AccountViewModel

  private val userFlow = MutableStateFlow<FirebaseUser?>(null)

  // Create a fake AccountService instead of mocking
  private val fakeAccountService =
      object : AccountService {
        override val currentUser: Flow<FirebaseUser?> = userFlow
        override val currentUserId: String = "test-uid"
        override val accountName: String = "test@example.com"

        override suspend fun hasUser(): Boolean = userFlow.value != null

        override suspend fun signInWithGoogle(
            credential: androidx.credentials.Credential
        ): Result<FirebaseUser> {
          throw NotImplementedError("Not used in these tests")
        }

        override fun signOut(): Result<Unit> = Result.success(Unit)
      }

  @Before
  fun setup() {
    mockUserRepository = mockk(relaxed = true)
    mockCredentialManager = mockk(relaxed = true)
    mockFirebaseUser = mockk(relaxed = true)

    every { mockFirebaseUser.uid } returns "test-uid"
    every { mockFirebaseUser.email } returns "test@example.com"
    every { mockFirebaseUser.photoUrl } returns null

    coEvery { mockUserRepository.getUser("test-uid") } returns
        User(
            uid = "test-uid",
            username = "testuser",
            profilePicture = Uri.EMPTY,
            friendList = emptyList())

    viewModel = AccountViewModel(fakeAccountService, mockUserRepository)
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  @Test
  fun accountScreen_displaysUserInformation_whenUserIsSignedIn() = runTest {
    userFlow.value = mockFirebaseUser

    composeTestRule.setContent {
      AccountScreen(accountViewModel = viewModel, credentialManager = mockCredentialManager)
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(TAG_ACCOUNT_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_USERNAME_FIELD).assertTextContains("testuser")
    composeTestRule.onNodeWithTag(TAG_GOOGLE_FIELD).assertTextContains("test@example.com")
  }

  @Test
  fun accountScreen_displaysProfilePicture_whenAvailable() = runTest {
    val profileUri = Uri.parse("https://example.com/profile.jpg")
    every { mockFirebaseUser.photoUrl } returns profileUri

    coEvery { mockUserRepository.getUser("test-uid") } returns
        User(
            uid = "test-uid",
            username = "testuser",
            profilePicture = profileUri,
            friendList = emptyList())

    userFlow.value = mockFirebaseUser

    composeTestRule.setContent {
      AccountScreen(accountViewModel = viewModel, credentialManager = mockCredentialManager)
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_IMAGE).assertExists()
  }

  @Test
  fun accountScreen_displaysDefaultAvatar_whenNoProfilePicture() = runTest {
    userFlow.value = mockFirebaseUser

    composeTestRule.setContent {
      AccountScreen(accountViewModel = viewModel, credentialManager = mockCredentialManager)
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_IMAGE).assertDoesNotExist()
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_CONTAINER).assertExists()
  }

  @Test
  fun accountScreen_callsOnBack_whenBackButtonClicked() {
    val onBack = mockk<() -> Unit>(relaxed = true)
    userFlow.value = mockFirebaseUser

    composeTestRule.setContent {
      AccountScreen(
          accountViewModel = viewModel, credentialManager = mockCredentialManager, onBack = onBack)
    }

    composeTestRule.onNodeWithTag(TAG_ACCOUNT_BACK).performClick()
    verify { onBack() }
  }

  @Test
  fun accountScreen_callsOnEditAvatar_whenEditButtonClicked() {
    val onEditAvatar = mockk<() -> Unit>(relaxed = true)
    userFlow.value = mockFirebaseUser

    composeTestRule.setContent {
      AccountScreen(
          accountViewModel = viewModel,
          credentialManager = mockCredentialManager,
          onEditAvatar = onEditAvatar)
    }

    composeTestRule.onNodeWithTag(TAG_ACCOUNT_EDIT).performClick()
    verify { onEditAvatar() }
  }

  @Test
  fun accountScreen_signOut_triggersSignOutFlow() {
    val onSignOut = mockk<() -> Unit>(relaxed = true)
    userFlow.value = mockFirebaseUser

    composeTestRule.setContent {
      AccountScreen(
          accountViewModel = viewModel,
          credentialManager = mockCredentialManager,
          onSignOut = onSignOut)
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(TAG_SIGNOUT_BUTTON).performClick()

    // Wait for all pending coroutines and UI updates to complete
    composeTestRule.waitForIdle()

    // Wait until the signedOut state is updated
    composeTestRule.waitUntil(timeoutMillis = 5000) { viewModel.uiState.value.signedOut }

    coVerify(timeout = 5000) { mockCredentialManager.clearCredentialState(any()) }
    verify(timeout = 5000) { onSignOut() }
  }

  @Test
  fun accountScreen_handlesUserRepositoryError() {
    // Create a fresh Flow for this test to avoid interference
    val errorUserFlow = MutableStateFlow<FirebaseUser?>(null)

    // Create a fake AccountService with the new Flow
    val errorAccountService =
        object : AccountService {
          override val currentUser: Flow<FirebaseUser?> = errorUserFlow
          override val currentUserId: String = "test-uid"
          override val accountName: String = "test@example.com"

          override suspend fun hasUser(): Boolean = errorUserFlow.value != null

          override suspend fun signInWithGoogle(
              credential: androidx.credentials.Credential
          ): Result<FirebaseUser> {
            throw NotImplementedError("Not used in these tests")
          }

          override fun signOut(): Result<Unit> = Result.success(Unit)
        }

    // Reconfigure the mock to throw an exception BEFORE creating the ViewModel
    coEvery { mockUserRepository.getUser("test-uid") } throws Exception("Failed to fetch user")

    // Create a fresh ViewModel with the error configuration
    val errorViewModel = AccountViewModel(errorAccountService, mockUserRepository)

    composeTestRule.setContent {
      AccountScreen(accountViewModel = errorViewModel, credentialManager = mockCredentialManager)
    }

    // Wait for compose to settle before triggering the error
    composeTestRule.waitForIdle()

    // Now set the user, which will trigger the error flow
    errorUserFlow.value = mockFirebaseUser

    // Wait for UI to process the error
    composeTestRule.waitForIdle()

    // Wait until error message is set - use a longer timeout
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      errorViewModel.uiState.value.errorMsg != null
    }

    // Error message should be displayed via Toast
    // Since we can't easily test Toast, verify the state is updated
    assert(errorViewModel.uiState.value.errorMsg != null)
  }

  @Test
  fun accountScreen_usesFirestoreProfilePicture_overGooglePhoto() = runTest {
    val googlePhotoUri = Uri.parse("https://google.com/photo.jpg")
    val firestorePhotoUri = Uri.parse("https://firestore.com/photo.jpg")

    every { mockFirebaseUser.photoUrl } returns googlePhotoUri

    coEvery { mockUserRepository.getUser("test-uid") } returns
        User(
            uid = "test-uid",
            username = "testuser",
            profilePicture = firestorePhotoUri,
            friendList = emptyList())

    userFlow.value = mockFirebaseUser

    composeTestRule.setContent {
      AccountScreen(accountViewModel = viewModel, credentialManager = mockCredentialManager)
    }

    composeTestRule.waitForIdle()

    assert(viewModel.uiState.value.profilePicture == firestorePhotoUri)
  }
}
