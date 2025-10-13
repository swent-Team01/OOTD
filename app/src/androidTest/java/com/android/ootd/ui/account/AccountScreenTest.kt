package com.android.ootd.ui.account
/*
 * DISCLAIMER: This file was created/modified with the assistance of GitHub Copilot.
 * Copilot provided suggestions which were reviewed and adapted by the developer.
 */
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.credentials.CredentialManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import com.android.ootd.ui.theme.OOTDTheme
import com.google.firebase.auth.FirebaseUser
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var mockAccountService: AccountService
  private lateinit var mockUserRepository: UserRepository
  private lateinit var mockCredentialManager: CredentialManager
  private lateinit var mockFirebaseUser: FirebaseUser
  private lateinit var viewModel: AccountViewModel

  private val userFlow = MutableStateFlow<FirebaseUser?>(null)

  @Before
  fun setup() {
    mockAccountService = mockk(relaxed = true)
    mockUserRepository = mockk(relaxed = true)
    mockCredentialManager = mockk(relaxed = true)
    mockFirebaseUser = mockk(relaxed = true)

    every { mockAccountService.currentUser } returns userFlow
    every { mockFirebaseUser.uid } returns "test-uid"
    every { mockFirebaseUser.email } returns "user1@google.com"
    every { mockFirebaseUser.photoUrl } returns null

    coEvery { mockUserRepository.getUser("test-uid") } returns
        User(uid = "test-uid", username = "user1", profilePicture = Uri.EMPTY)

    viewModel = AccountViewModel(mockAccountService, mockUserRepository)
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  @Test
  fun accountScreen_showsAllMainElements_withoutAvatar() {
    userFlow.value = mockFirebaseUser

    composeTestRule.setContent {
      OOTDTheme {
        AccountScreen(accountViewModel = viewModel, credentialManager = mockCredentialManager)
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(TAG_ACCOUNT_BACK).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_CONTAINER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_IMAGE).assertDoesNotExist()
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_EDIT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_USERNAME_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_USERNAME_CLEAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_GOOGLE_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(TAG_SIGNOUT_BUTTON).assertIsDisplayed()
  }

  @Test
  fun accountScreen_showsAvatarImage_whenProfilePictureExists() {
    val avatarUri = Uri.parse("https://example.com/avatar.jpg")
    every { mockFirebaseUser.photoUrl } returns avatarUri
    coEvery { mockUserRepository.getUser("test-uid") } returns
        User(uid = "test-uid", username = "user1", profilePicture = avatarUri)

    userFlow.value = mockFirebaseUser

    composeTestRule.setContent {
      OOTDTheme {
        AccountScreen(accountViewModel = viewModel, credentialManager = mockCredentialManager)
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_IMAGE).assertIsDisplayed()
  }

  @Test
  fun accountScreen_displaysCorrectUserInfo() {
    userFlow.value = mockFirebaseUser

    composeTestRule.setContent {
      OOTDTheme {
        AccountScreen(accountViewModel = viewModel, credentialManager = mockCredentialManager)
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(TAG_USERNAME_FIELD).assertTextContains("user1")
    composeTestRule.onNodeWithTag(TAG_GOOGLE_FIELD).assertTextContains("user1@google.com")
  }

  @Test
  fun accountScreen_callsOnBack_whenBackButtonClicked() {
    val onBack = mockk<() -> Unit>(relaxed = true)
    userFlow.value = mockFirebaseUser

    composeTestRule.setContent {
      OOTDTheme {
        AccountScreen(
            accountViewModel = viewModel,
            credentialManager = mockCredentialManager,
            onBack = onBack)
      }
    }

    composeTestRule.onNodeWithTag(TAG_ACCOUNT_BACK).performClick()
    verify { onBack() }
  }

  @Test
  fun accountScreen_callsOnEditAvatar_whenEditButtonClicked() {
    val onEditAvatar = mockk<() -> Unit>(relaxed = true)
    userFlow.value = mockFirebaseUser

    composeTestRule.setContent {
      OOTDTheme {
        AccountScreen(
            accountViewModel = viewModel,
            credentialManager = mockCredentialManager,
            onEditAvatar = onEditAvatar)
      }
    }

    composeTestRule.onNodeWithTag(TAG_ACCOUNT_EDIT).performClick()
    verify { onEditAvatar() }
  }

  @Test
  fun accountScreen_showsLoadingIndicator_whenLoading() {
    userFlow.value = null

    composeTestRule.setContent {
      OOTDTheme {
        AccountScreen(accountViewModel = viewModel, credentialManager = mockCredentialManager)
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_LOADING).assertDoesNotExist()
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

  private lateinit var accountService: AccountService
  private lateinit var userRepository: UserRepository
  private lateinit var credentialManager: CredentialManager
  private lateinit var viewModel: AccountViewModel
  private lateinit var mockFirebaseUser: FirebaseUser

  private val testDispatcher = StandardTestDispatcher()
  private val userFlow = MutableStateFlow<FirebaseUser?>(null)

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    accountService = mockk(relaxed = true)
    userRepository = mockk(relaxed = true)
    credentialManager = mockk(relaxed = true)
    mockFirebaseUser = mockk(relaxed = true)

    every { accountService.currentUser } returns userFlow
    every { mockFirebaseUser.uid } returns "test-uid"
    every { mockFirebaseUser.email } returns "test@example.com"
    every { mockFirebaseUser.photoUrl } returns null

    coEvery { userRepository.getUser("test-uid") } returns
        User(uid = "test-uid", username = "testuser", profilePicture = Uri.EMPTY)

    coEvery { credentialManager.clearCredentialState(any()) } just Runs
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    clearAllMocks()
  }

  @Test
  fun uiState_initializes_with_empty_values() {
    viewModel = AccountViewModel(accountService, userRepository)

    val state = viewModel.uiState.value
    assertEquals("", state.username)
    assertEquals("", state.googleAccountName)
    assertNull(state.profilePicture)
    assertNull(state.errorMsg)
    assertFalse(state.signedOut)
    assertFalse(state.isLoading)
  }

  @Test
  fun refreshUIState_updates_uiState_when_user_is_signed_in() = runTest {
    viewModel = AccountViewModel(accountService, userRepository)

    userFlow.value = mockFirebaseUser
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("testuser", state.username)
    assertEquals("test@example.com", state.googleAccountName)
    assertNull(state.errorMsg)
  }

  @Test
  fun uiState_uses_Google_photo_when_Firestore_has_no_profile_picture() = runTest {
    val googlePhotoUri = Uri.parse("https://google.com/photo.jpg")
    every { mockFirebaseUser.photoUrl } returns googlePhotoUri

    viewModel = AccountViewModel(accountService, userRepository)

    userFlow.value = mockFirebaseUser
    advanceUntilIdle()

    assertEquals(googlePhotoUri, viewModel.uiState.value.profilePicture)
  }

  @Test
  fun uiState_prefers_Firestore_profile_picture_over_Google_photo() = runTest {
    val googlePhotoUri = Uri.parse("https://google.com/photo.jpg")
    val firestorePhotoUri = Uri.parse("https://firestore.com/photo.jpg")

    every { mockFirebaseUser.photoUrl } returns googlePhotoUri
    coEvery { userRepository.getUser("test-uid") } returns
        User(uid = "test-uid", username = "testuser", profilePicture = firestorePhotoUri)

    viewModel = AccountViewModel(accountService, userRepository)

    userFlow.value = mockFirebaseUser
    advanceUntilIdle()

    assertEquals(firestorePhotoUri, viewModel.uiState.value.profilePicture)
  }

  @Test
  fun refreshUIState_handles_repository_error_gracefully() = runTest {
    coEvery { userRepository.getUser("test-uid") } throws Exception("Network error")

    viewModel = AccountViewModel(accountService, userRepository)

    userFlow.value = mockFirebaseUser
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNotNull(state.errorMsg)
    assertTrue(state.errorMsg!!.contains("Network error"))
  }

  @Test
  fun refreshUIState_handles_null_user() = runTest {
    viewModel = AccountViewModel(accountService, userRepository)

    userFlow.value = null
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("", state.username)
    assertEquals("", state.googleAccountName)
    assertNull(state.profilePicture)
  }

  @Test
  fun signOut_succeeds_and_updates_state() = runTest {
    coEvery { accountService.signOut() } returns Result.success(Unit)

    viewModel = AccountViewModel(accountService, userRepository)
    viewModel.signOut(credentialManager)
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.signedOut)
    assertNull(viewModel.uiState.value.errorMsg)
    coVerify { accountService.signOut() }
    coVerify { credentialManager.clearCredentialState(any()) }
  }

  @Test
  fun signOut_failure_updates_error_message() = runTest {
    val errorMessage = "Sign out failed"
    coEvery { accountService.signOut() } returns Result.failure(IllegalStateException(errorMessage))

    viewModel = AccountViewModel(accountService, userRepository)
    viewModel.signOut(credentialManager)
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.signedOut)
    assertEquals(errorMessage, viewModel.uiState.value.errorMsg)
  }

  @Test
  fun clearErrorMsg_clears_error_message() = runTest {
    coEvery { userRepository.getUser("test-uid") } throws Exception("Error")

    viewModel = AccountViewModel(accountService, userRepository)
    userFlow.value = mockFirebaseUser
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMsg)

    viewModel.clearErrorMsg()

    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun signOut_clears_credential_state_even_on_failure() = runTest {
    coEvery { accountService.signOut() } returns Result.failure(IllegalStateException("Error"))

    viewModel = AccountViewModel(accountService, userRepository)
    viewModel.signOut(credentialManager)
    advanceUntilIdle()

    coVerify { credentialManager.clearCredentialState(any()) }
  }

  @Test
  fun refreshUIState_sets_loading_state_correctly() = runTest {
    viewModel = AccountViewModel(accountService, userRepository)

    userFlow.value = mockFirebaseUser

    // Loading should be false after refresh completes
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isLoading)
  }
}
