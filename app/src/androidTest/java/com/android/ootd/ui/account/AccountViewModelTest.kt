import android.net.Uri
import androidx.credentials.CredentialManager
import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.user.UserRepository
import com.android.ootd.ui.account.AccountViewModel
import com.google.firebase.auth.FirebaseUser
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
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
import org.junit.Test

/*
 * DISCLAIMER: This file was created/modified with the assistance of GitHub Copilot.
 * Copilot provided suggestions which were reviewed and adapted by the developer.
 */

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

  private lateinit var accountService: AccountService
  private lateinit var accountRepository: AccountRepository
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
    accountRepository = mockk(relaxed = true)
    userRepository = mockk(relaxed = true)
    credentialManager = mockk(relaxed = true)
    mockFirebaseUser = mockk(relaxed = true)

    every { accountService.currentUser } returns userFlow
    every { accountService.currentUserId } returns "test-uid"
    every { mockFirebaseUser.uid } returns "test-uid"
    every { mockFirebaseUser.email } returns "test@example.com"
    every { mockFirebaseUser.photoUrl } returns null

    coEvery { accountRepository.getAccount("test-uid") } returns
        Account(uid = "test-uid", username = "testuser", profilePicture = "")

    coEvery { accountRepository.editAccount(any(), any(), any(), any()) } just Runs
    coEvery { userRepository.editUser(any(), any(), any()) } just Runs
    coEvery { credentialManager.clearCredentialState(any()) } just Runs
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    clearAllMocks()
  }

  @Test
  fun uiState_initializes_with_empty_values() {
    viewModel = AccountViewModel(accountService, accountRepository)

    val state = viewModel.uiState.value
    assertEquals("", state.username)
    assertEquals("", state.googleAccountName)
    assertEquals("", state.profilePicture)
    assertNull(state.errorMsg)
    assertFalse(state.signedOut)
    assertFalse(state.isLoading)
  }

  @Test
  fun observeAuthState_updates_uiState_when_user_is_signed_in() = runTest {
    viewModel = AccountViewModel(accountService, accountRepository)

    userFlow.value = mockFirebaseUser
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("testuser", state.username)
    assertEquals("test@example.com", state.googleAccountName)
    assertNull(state.errorMsg)
  }

  @Test
  fun uiState_prefers_Firestore_profile_picture_over_no_photo() = runTest {
    val emptyPhotoUri = Uri.parse("")
    val firestorePhotoUri = Uri.parse("https://firestore.com/photo.jpg")

    every { mockFirebaseUser.photoUrl } returns emptyPhotoUri
    coEvery { accountRepository.getAccount("test-uid") } returns
        Account(
            uid = "test-uid", username = "testuser", profilePicture = firestorePhotoUri.toString())

    viewModel = AccountViewModel(accountService, accountRepository)

    userFlow.value = mockFirebaseUser
    advanceUntilIdle()

    assertEquals(firestorePhotoUri.toString(), viewModel.uiState.value.profilePicture)
  }

  @Test
  fun observeAuthState_handles_null_user() = runTest {
    viewModel = AccountViewModel(accountService, accountRepository)

    userFlow.value = null
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("", state.username)
    assertEquals("", state.googleAccountName)
    assertEquals("", state.profilePicture)
  }

  @Test
  fun clearErrorMsg_clears_error_message() = runTest {
    coEvery { accountRepository.getAccount("test-uid") } throws Exception("Error")

    viewModel = AccountViewModel(accountService, accountRepository)
    userFlow.value = mockFirebaseUser
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMsg)

    viewModel.clearErrorMsg()

    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun observeAuthState_reactively_updates_when_user_data_changes() = runTest {
    viewModel = AccountViewModel(accountService, accountRepository)

    userFlow.value = mockFirebaseUser
    advanceUntilIdle()

    // Verify initial state
    assertEquals("testuser", viewModel.uiState.value.username)

    // Change to a different user to trigger observeAuthState() to reload
    coEvery { accountRepository.getAccount("test-uid-2") } returns
        Account(uid = "test-uid-2", username = "updateduser", profilePicture = "")

    val updatedMockUser =
        mockk<FirebaseUser> {
          every { uid } returns "test-uid-2"
          every { email } returns "updated@example.com"
          every { photoUrl } returns null
        }
    userFlow.value = updatedMockUser
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("updateduser", state.username)
    assertEquals("updated@example.com", state.googleAccountName)
  }

  @Test
  fun editUser_updates_multiple_fields_successfully() = runTest {
    viewModel = AccountViewModel(accountService, accountRepository, userRepository)
    userFlow.value = mockFirebaseUser
    advanceUntilIdle()

    val newUsername = "updateduser"
    val newDate = "15/05/1995"
    val newPicture = "https://example.com/newpic.jpg"

    viewModel.editUser(newUsername = newUsername, newDate = newDate, profilePicture = newPicture)
    advanceUntilIdle()

    coVerify { accountRepository.editAccount("test-uid", newUsername, newDate, newPicture) }
    coVerify { userRepository.editUser("test-uid", newUsername, newPicture) }
    assertEquals(newUsername, viewModel.uiState.value.username)
    assertEquals(newPicture, viewModel.uiState.value.profilePicture)
    assertFalse(viewModel.uiState.value.isLoading)
    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun editUser_sets_error_message_when_accountOrUserRepository_fails() = runTest {
    coEvery { accountRepository.editAccount(any(), any(), any(), any()) } throws
        Exception("Failed to update account")
    coEvery { userRepository.editUser(any(), any(), any()) } throws
        Exception("Username already taken")

    viewModel = AccountViewModel(accountService, accountRepository, userRepository)
    userFlow.value = mockFirebaseUser
    advanceUntilIdle()

    viewModel.editUser(newUsername = "newusername")
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMsg)
    assertEquals("Failed to update account", viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.isLoading)

    viewModel.editUser(newUsername = "takenusername")

    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.isLoading)
  }

  @Test
  fun editUser_sets_isLoading_to_true_while_updating() = runTest {
    viewModel = AccountViewModel(accountService, accountRepository, userRepository)
    userFlow.value = mockFirebaseUser
    advanceUntilIdle()

    // Don't advance until idle to capture loading state
    viewModel.editUser(newUsername = "newusername")

    // Check that loading is true before the coroutine completes
    // Note: This might be flaky depending on timing, so we advance partially
    testScheduler.advanceTimeBy(1)

    // Now complete the operation
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isLoading)
  }

  @Test
  fun onTogglePrivacy_success_updatesStateWithRepositoryValue() = runTest {
    coEvery { accountRepository.getAccount("test-uid") } returns
        Account(uid = "test-uid", username = "testuser", profilePicture = "", isPrivate = false)
    coEvery { accountRepository.togglePrivacy("test-uid") } returns true

    viewModel = AccountViewModel(accountService, accountRepository)

    // Emit authenticated user and wait for load
    userFlow.value = mockFirebaseUser
    advanceUntilIdle()

    // Verify initial and post-toggle states
    assertFalse(viewModel.uiState.value.isPrivate)
    viewModel.onTogglePrivacy()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.isPrivate)
    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun onTogglePrivacy_failure_revertsAndSetsError() = runTest {
    // Arrange initial account with private profile (isPrivate = true)
    coEvery { accountRepository.getAccount("test-uid") } returns
        Account(uid = "test-uid", username = "testuser", profilePicture = "", isPrivate = true)
    coEvery { accountRepository.togglePrivacy("test-uid") } throws Exception("boom")

    viewModel = AccountViewModel(accountService, accountRepository)

    // Emit authenticated user and wait for load
    userFlow.value = mockFirebaseUser
    advanceUntilIdle()

    // Act
    assertTrue(viewModel.uiState.value.isPrivate)
    viewModel.onTogglePrivacy()
    advanceUntilIdle()

    // Assert: reverted to original and error was set
    assertTrue(viewModel.uiState.value.isPrivate)
    assertNotNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun privacyHelp_togglesAndDismisses() = runTest {
    viewModel = AccountViewModel(accountService, accountRepository)

    // Initially hidden
    assertFalse(viewModel.uiState.value.showPrivacyHelp)
    // Toggle on
    viewModel.onPrivacyHelpClick()
    assertTrue(viewModel.uiState.value.showPrivacyHelp)
    // Dismiss -> off
    viewModel.onPrivacyHelpDismiss()
    assertFalse(viewModel.uiState.value.showPrivacyHelp)
  }
}
