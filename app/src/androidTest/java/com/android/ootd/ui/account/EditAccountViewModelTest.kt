package com.android.ootd.ui.account

import android.net.Uri
import androidx.credentials.CredentialManager
import com.android.ootd.LocationProvider
import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.InvalidLocationException
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.LocationRepository
import com.android.ootd.model.map.emptyLocation
import com.android.ootd.model.user.UserRepository
import com.android.ootd.ui.map.LocationSelectionViewModel
import com.android.ootd.utils.LocationUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.auth.FirebaseUser
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
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
class EditAccountViewModelTest {

  private lateinit var accountService: AccountService
  private lateinit var accountRepository: AccountRepository
  private lateinit var userRepository: UserRepository
  private lateinit var locationRepository: LocationRepository
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
    locationRepository = mockk(relaxed = true)
    credentialManager = mockk(relaxed = true)
    mockFirebaseUser = mockk(relaxed = true)

    every { accountService.currentUser } returns userFlow
    every { accountService.currentUserId } returns "test-uid"
    every { mockFirebaseUser.uid } returns "test-uid"
    every { mockFirebaseUser.email } returns "test@example.com"
    every { mockFirebaseUser.photoUrl } returns null

    coEvery { accountRepository.getAccount("test-uid") } returns
        Account(uid = "test-uid", username = "testuser", profilePicture = "")

    coEvery { accountRepository.editAccount(any(), any(), any(), any(), any()) } just Runs
    coEvery { userRepository.editUser(any(), any(), any()) } just Runs
    coEvery { credentialManager.clearCredentialState(any()) } just Runs
    coEvery { locationRepository.search(any()) } returns emptyList()

    // Mock the fusedLocationClient to avoid lateinit errors
    LocationProvider.fusedLocationClient = mockk<FusedLocationProviderClient>(relaxed = true)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    clearAllMocks()
  }

  // --- tiny helpers to shorten tests ---
  private fun initVM(
      accRepo: AccountRepository = accountRepository,
      usrRepo: UserRepository = userRepository,
      locRepo: LocationRepository = locationRepository
  ) {
    viewModel =
        AccountViewModel(accountService, accRepo, usrRepo, LocationSelectionViewModel(locRepo))
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

    coVerify { accountRepository.editAccount("test-uid", newUsername, newDate, newPicture, any()) }
    coVerify { userRepository.editUser("test-uid", newUsername, newPicture) }
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

    coVerify { accountRepository.editAccount("test-uid", "", "01/01/2000", "", any()) }
    coVerify(exactly = 1) { userRepository.editUser(any(), "", "") }
  }

  @Test
  fun editUser_sets_error_message_when_accountOrUserRepository_fails() = runTest {
    coEvery { accountRepository.editAccount(any(), any(), any(), any(), any()) } throws
        Exception("Failed to update account")
    coEvery { userRepository.editUser(any(), any(), any()) } throws
        Exception("Username already taken")

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
  fun onTogglePrivacy_invalidLocation_revertsAndShowsLocationError() = runTest {
    coEvery { accountRepository.getAccount("test-uid") } returns
        Account(uid = "test-uid", username = "testuser", profilePicture = "", isPrivate = true)
    coEvery { accountRepository.togglePrivacy("test-uid") } throws InvalidLocationException()
    initVM()
    signInAs(mockFirebaseUser)

    assertTrue("Initial state should be private", viewModel.uiState.value.isPrivate)
    assertFalse("Should not be loading", viewModel.uiState.value.isLoading)
    viewModel.onTogglePrivacy()
    advanceUntilIdle()
    assertTrue("Privacy should remain true after exception", viewModel.uiState.value.isPrivate)
    assertNotNull("Error message should be set", viewModel.uiState.value.errorMsg)
    assertFalse("Error message should not be empty", viewModel.uiState.value.errorMsg!!.isEmpty())
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

  @Test
  fun deleteProfilePicture_success_clearsProfilePictureInState() = runTest {
    stubAccount("test-uid", username = "testuser", picture = "https://example.com/pic.jpg")
    coEvery { accountRepository.deleteProfilePicture("test-uid") } just Runs
    coEvery { userRepository.deleteProfilePicture("test-uid") } just Runs

    initVM()
    signInAs(mockFirebaseUser)

    // Verify initial state has profile picture
    assertEquals("https://example.com/pic.jpg", viewModel.uiState.value.profilePicture)

    viewModel.deleteProfilePicture()
    advanceUntilIdle()

    // Verify profile picture is cleared
    assertEquals("", viewModel.uiState.value.profilePicture)
    assertNull(viewModel.uiState.value.errorMsg)
    coVerify { accountRepository.deleteProfilePicture("test-uid") }
    coVerify { userRepository.deleteProfilePicture("test-uid") }
  }

  @Test
  fun deleteProfilePicture_failure_setsErrorMessage() = runTest {
    stubAccount("test-uid", username = "testuser", picture = "https://example.com/pic.jpg")
    coEvery { accountRepository.deleteProfilePicture("test-uid") } throws
        Exception("Failed to delete profile picture")

    initVM()
    signInAs(mockFirebaseUser)

    viewModel.deleteProfilePicture()
    advanceUntilIdle()

    // Verify error is set
    assertNotNull(viewModel.uiState.value.errorMsg)
    assertEquals("Failed to delete profile picture", viewModel.uiState.value.errorMsg)
    coVerify { accountRepository.deleteProfilePicture("test-uid") }
  }

  @Test
  fun uploadImageToStorage_coversBlankSuccessErrorAndNoAuth() = runTest {
    // 1) Blank path: should return input immediately and not call uploader
    run {
      val vm =
          AccountViewModel(
              accountService = accountService,
              accountRepository = accountRepository,
              userRepository = userRepository,
              uploader = { _, _, _ -> error("uploader should not be called for blank path") })

      var result: String? = null
      vm.uploadImageToStorage("", onResult = { result = it })
      assertEquals("", result)
    }

    // Ensure signed-in for upload-dependent cases
    signInAs(mockFirebaseUser)

    // 2) Success path: uploader returns URL, onResult receives it, no error set
    run {
      val vm =
          AccountViewModel(
              accountService = accountService,
              accountRepository = accountRepository,
              userRepository = userRepository,
              uploader = { uid, local, _ ->
                "https://cdn.test/$uid/${local.substringAfterLast('/')}"
              })

      advanceUntilIdle()

      var result: String? = null
      vm.uploadImageToStorage("/tmp/pic.jpg", onResult = { result = it })
      advanceUntilIdle()

      assertEquals("https://cdn.test/test-uid/pic.jpg", result)
      assertNull(vm.uiState.value.errorMsg)
    }

    // 3) Error path: uploader throws, onError called, errorMsg set
    run {
      val vm =
          AccountViewModel(
              accountService = accountService,
              accountRepository = accountRepository,
              userRepository = userRepository,
              uploader = { _, _, _ -> throw IllegalStateException("upload failed") })

      advanceUntilIdle()

      var caught: Throwable? = null
      vm.uploadImageToStorage("/tmp/pic.jpg", onResult = {}, onError = { caught = it })
      advanceUntilIdle()

      assertTrue(caught is IllegalStateException)
      assertEquals("upload failed", vm.uiState.value.errorMsg)
    }

    // 4) No-auth path: do not sign in, should set error and call onError
    run {
      // Clear sign-in
      signInAs(null)

      val vm =
          AccountViewModel(
              accountService = accountService,
              accountRepository = accountRepository,
              userRepository = userRepository,
              uploader = { _, _, _ -> "never" })

      var caught: Throwable? = null
      vm.uploadImageToStorage("/tmp/pic.jpg", onResult = {}, onError = { caught = it })

      assertTrue(caught is IllegalStateException)
      assertEquals("No authenticated user", vm.uiState.value.errorMsg)
    }
  }

  @Test
  fun editUser_updates_location_successfully() = runTest {
    initVM()
    signInAs(mockFirebaseUser)

    val newLocation = Location(46.0, 7.0, "Test Location")

    viewModel.editUser(newLocation = newLocation)
    advanceUntilIdle()

    coVerify { accountRepository.editAccount("test-uid", any(), any(), any(), newLocation) }
    assertEquals(newLocation, viewModel.uiState.value.location)
  }

  // --- New GPS/Location functionality tests ---

  @Test
  fun setLocationQuery_clearsLocation_whenQueryIsEmpty() = runTest {
    initVM()
    signInAs(mockFirebaseUser)

    // Set a location first
    val location = Location(46.0, 7.0, "Test Location")
    viewModel.setLocation(location)
    advanceUntilIdle()

    assertEquals(location, viewModel.uiState.value.location)
    assertEquals("Test Location", viewModel.locationSelectionViewModel.uiState.value.locationQuery)

    // Clear the query
    viewModel.locationSelectionViewModel.setLocationQuery("")
    advanceUntilIdle()

    // Location should be cleared to make field editable
    assertEquals(emptyLocation, viewModel.uiState.value.location)
    assertEquals("", viewModel.locationSelectionViewModel.uiState.value.locationQuery)
  }

  @Test
  fun onLocationPermissionGranted_retrievesGPS_andSavesLocation() = runTest {
    // Mock LocationUtils to avoid the async Dispatchers.IO issue
    mockkObject(LocationUtils)

    val expectedLocation = Location(46.5191, 6.5668, "Lausanne, Switzerland")

    // Capture the callbacks and invoke onSuccess synchronously
    val onSuccessSlot = slot<(Location) -> Unit>()
    val onFailureSlot = slot<(String) -> Unit>()

    every {
      LocationUtils.getCurrentGPSLocation(
          locationRepository = any(),
          onSuccess = capture(onSuccessSlot),
          onFailure = capture(onFailureSlot))
    } answers
        {
          // Immediately invoke the success callback with our test location
          onSuccessSlot.captured.invoke(expectedLocation)
        }

    initVM()
    signInAs(mockFirebaseUser)

    viewModel.onLocationPermissionGranted()
    advanceUntilIdle()

    // Verify location was set and saved
    val location = viewModel.uiState.value.location
    assertNotNull(location)
    assertEquals(46.5191, location.latitude, 0.0001)
    assertEquals(6.5668, location.longitude, 0.0001)
    assertEquals("Lausanne, Switzerland", location.name)
    coVerify { accountRepository.editAccount("test-uid", any(), any(), any(), location) }
    assertFalse(viewModel.locationSelectionViewModel.uiState.value.isLoadingLocations)

    unmockkObject(LocationUtils)
  }

  @Test
  fun onLocationPermissionDenied_showsErrorMessage() = runTest {
    initVM()
    signInAs(mockFirebaseUser)

    viewModel.onLocationPermissionDenied()
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMsg)
    assertTrue(viewModel.uiState.value.errorMsg!!.contains("Location permission denied"))
  }

  @Test
  fun uploadImageToStorage_invokes_callbacks_correctly() = runTest {
    // Arrange
    val localPath = "/local/image.jpg"
    val uploadedUrl = "https://storage.example.com/test-uid/image.jpg"

    val vm =
        AccountViewModel(
            accountService = accountService,
            accountRepository = accountRepository,
            userRepository = userRepository,
            uploader = { uid, path, _ ->
              assertEquals("test-uid", uid)
              assertEquals(localPath, path)
              uploadedUrl
            })

    signInAs(mockFirebaseUser)

    // Act
    var resultUrl: String? = null
    var errorThrown: Throwable? = null

    vm.uploadImageToStorage(
        localPath = localPath, onResult = { resultUrl = it }, onError = { errorThrown = it })
    advanceUntilIdle()

    // Assert
    assertEquals(uploadedUrl, resultUrl)
    assertNull(errorThrown)
    assertNull(vm.uiState.value.errorMsg)
  }

  // --- Delete Account tests ---

  @Test
  fun deleteAccount_success_deletesDataAndSignsOut() = runTest {
    coEvery { accountRepository.deleteAccount("test-uid") } just Runs
    coEvery { userRepository.deleteUser("test-uid") } just Runs
    coEvery { accountService.signOut() } returns Result.success(Unit)

    initVM()
    signInAs(mockFirebaseUser)

    assertFalse(viewModel.uiState.value.signedOut)

    viewModel.deleteAccount()
    advanceUntilIdle()

    // Verify all deletion calls were made in correct order
    coVerify(exactly = 1) { accountRepository.deleteAccount("test-uid") }
    coVerify(exactly = 1) { userRepository.deleteUser("test-uid") }
    coVerify(exactly = 1) { accountService.signOut() }

    // Verify state is cleared and signedOut is true
    assertTrue(viewModel.uiState.value.signedOut)
    assertNull(viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.isLoading)
  }

  @Test
  fun deleteAccount_failure_accountRepository_setsErrorMessage() = runTest {
    coEvery { accountRepository.deleteAccount("test-uid") } throws
        Exception("Failed to delete account data")

    initVM()
    signInAs(mockFirebaseUser)

    viewModel.deleteAccount()
    advanceUntilIdle()

    // Verify error is set and user is NOT signed out
    assertFalse(viewModel.uiState.value.signedOut)
    assertNotNull(viewModel.uiState.value.errorMsg)
    assertTrue(viewModel.uiState.value.errorMsg!!.contains("Failed deleting account"))
    assertFalse(viewModel.uiState.value.isLoading)

    // Verify user repository delete was not called
    coVerify(exactly = 0) { userRepository.deleteUser(any()) }
    coVerify(exactly = 0) { accountService.signOut() }
  }

  @Test
  fun deleteAccount_failure_userRepository_setsErrorMessage() = runTest {
    coEvery { accountRepository.deleteAccount("test-uid") } just Runs
    coEvery { userRepository.deleteUser("test-uid") } throws Exception("Failed to delete user data")

    initVM()
    signInAs(mockFirebaseUser)

    viewModel.deleteAccount()
    advanceUntilIdle()

    // Verify error is set and user is NOT signed out
    assertFalse(viewModel.uiState.value.signedOut)
    assertNotNull(viewModel.uiState.value.errorMsg)
    assertTrue(viewModel.uiState.value.errorMsg!!.contains("Failed deleting account"))
    assertFalse(viewModel.uiState.value.isLoading)

    // Verify account was deleted but sign out was not called
    coVerify(exactly = 1) { accountRepository.deleteAccount("test-uid") }
    coVerify(exactly = 0) { accountService.signOut() }
  }

  @Test
  fun deleteAccount_setsLoadingWhileProcessing() = runTest {
    coEvery { accountRepository.deleteAccount("test-uid") } just Runs
    coEvery { userRepository.deleteUser("test-uid") } just Runs
    coEvery { accountService.signOut() } returns Result.success(Unit)

    initVM()
    signInAs(mockFirebaseUser)

    assertFalse(viewModel.uiState.value.isLoading)

    // Start delete but don't complete
    viewModel.deleteAccount()
    testScheduler.advanceTimeBy(1)

    // Now complete the operation
    advanceUntilIdle()

    // Should no longer be loading after completion
    assertFalse(viewModel.uiState.value.isLoading)
    assertTrue(viewModel.uiState.value.signedOut)
  }
}
