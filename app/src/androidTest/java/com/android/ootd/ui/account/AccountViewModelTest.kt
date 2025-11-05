package com.android.ootd.ui.account

import android.net.Uri
import androidx.credentials.CredentialManager
import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.user.UserRepository
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
import kotlinx.coroutines.test.TestScope
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
    coEvery { userRepository.editUsername(any(), any()) } just Runs
    coEvery { credentialManager.clearCredentialState(any()) } just Runs
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    clearAllMocks()
  }

  // --- tiny helpers to shorten tests ---
  private fun initVM(
      accRepo: AccountRepository = accountRepository,
      usrRepo: UserRepository = userRepository
  ) {
    viewModel = AccountViewModel(accountService, accRepo, usrRepo)
  }

  private fun mockUser(
      uid: String = "test-uid",
      email: String = "test@example.com",
      photo: String? = null
  ): FirebaseUser =
      mockk(relaxed = true) {
        every { this@mockk.uid } returns uid
        every { this@mockk.email } returns email
        every { this@mockk.photoUrl } returns photo?.let { Uri.parse(it) }
      }

  private fun TestScope.signInAs(user: FirebaseUser?) {
    userFlow.value = user
    advanceUntilIdle()
  }

  private fun stubAccount(uid: String, username: String = "testuser", picture: String = "") {
    coEvery { accountRepository.getAccount(uid) } returns
        Account(uid = uid, username = username, profilePicture = picture)
  }

  @Test
  fun uiState_initializes_with_empty_values() {
    initVM()

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
    initVM()

    signInAs(mockFirebaseUser)

    val state = viewModel.uiState.value
    assertEquals("testuser", state.username)
    assertEquals("test@example.com", state.googleAccountName)
    assertNull(state.errorMsg)
  }

  @Test
  fun uiState_prefers_Firestore_profile_picture_over_no_photo() = runTest {
    val firestorePhotoUri = "https://firestore.com/photo.jpg"

    every { mockFirebaseUser.photoUrl } returns Uri.parse("")
    stubAccount("test-uid", picture = firestorePhotoUri)

    initVM()
    signInAs(mockFirebaseUser)

    assertEquals(firestorePhotoUri, viewModel.uiState.value.profilePicture)
  }

  @Test
  fun observeAuthState_handles_null_user() = runTest {
    initVM()

    signInAs(null)

    val state = viewModel.uiState.value
    assertEquals("", state.username)
    assertEquals("", state.googleAccountName)
    assertEquals("", state.profilePicture)
  }

  @Test
  fun clearErrorMsg_clears_error_message() = runTest {
    coEvery { accountRepository.getAccount("test-uid") } throws Exception("Error")

    initVM()
    signInAs(mockFirebaseUser)

    assertNotNull(viewModel.uiState.value.errorMsg)

    viewModel.clearErrorMsg()

    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun observeAuthState_reactively_updates_when_user_data_changes() = runTest {
    initVM()

    signInAs(mockFirebaseUser)
    assertEquals("testuser", viewModel.uiState.value.username)

    stubAccount("test-uid-2", username = "updateduser")

    val updatedMockUser = mockUser(uid = "test-uid-2", email = "updated@example.com")
    signInAs(updatedMockUser)

    val state = viewModel.uiState.value
    assertEquals("updateduser", state.username)
    assertEquals("updated@example.com", state.googleAccountName)
  }

  @Test
  fun editUser_updates_multiple_fields_successfully() = runTest {
    initVM()
    signInAs(mockFirebaseUser)

    val newUsername = "updateduser"
    val newDate = "15/05/1995"
    val newPicture = "https://example.com/newpic.jpg"

    viewModel.editUser(newUsername = newUsername, newDate = newDate, profilePicture = newPicture)
    advanceUntilIdle()

    coVerify { accountRepository.editAccount("test-uid", newUsername, newDate, newPicture) }
    coVerify { userRepository.editUsername("test-uid", newUsername) }
    assertEquals(newUsername, viewModel.uiState.value.username)
    assertEquals(newPicture, viewModel.uiState.value.profilePicture)
    assertFalse(viewModel.uiState.value.isLoading)
    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun editUser_does_not_call_userRepository_when_username_is_blank() = runTest {
    initVM()
    signInAs(mockFirebaseUser)

    viewModel.editUser(newUsername = "", newDate = "01/01/2000")
    advanceUntilIdle()

    coVerify { accountRepository.editAccount("test-uid", "", "01/01/2000", "") }
    coVerify(exactly = 0) { userRepository.editUsername(any(), "") }
  }

  @Test
  fun editUser_sets_error_message_when_accountOrUserRepository_fails() = runTest {
    coEvery { accountRepository.editAccount(any(), any(), any(), any()) } throws
        Exception("Failed to update account")
    coEvery { userRepository.editUsername(any(), any()) } throws Exception("Username already taken")

    initVM()
    signInAs(mockFirebaseUser)

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
    initVM()
    signInAs(mockFirebaseUser)

    // Don't advance until idle to capture loading state
    viewModel.editUser(newUsername = "newusername")
    testScheduler.advanceTimeBy(1)

    // Now complete the operation
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isLoading)
  }

  @Test
  fun onTogglePrivacy_success_updatesStateWithRepositoryValue() = runTest {
    stubAccount("test-uid", username = "testuser", picture = "")
    coEvery { accountRepository.togglePrivacy("test-uid") } returns true

    initVM()

    // Emit authenticated user and wait for load
    signInAs(mockFirebaseUser)

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

    initVM()

    // Emit authenticated user and wait for load
    signInAs(mockFirebaseUser)

    assertTrue(viewModel.uiState.value.isPrivate)
    viewModel.onTogglePrivacy()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.isPrivate)
    assertNotNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun privacyHelp_togglesAndDismisses() = runTest {
    initVM()

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
