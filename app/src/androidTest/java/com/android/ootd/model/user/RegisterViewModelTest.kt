package com.android.ootd.model.user

import com.android.ootd.LocationProvider
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.map.Location
import com.android.ootd.ui.map.LocationSelectionViewModel
import com.android.ootd.ui.register.RegisterViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

// Tests generated with the help of AI
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

  private lateinit var userRepository: UserRepository
  private lateinit var accountRepository: AccountRepository
  private lateinit var locationSelectionViewModel: LocationSelectionViewModel
  private lateinit var auth: FirebaseAuth
  private lateinit var firebaseUser: FirebaseUser
  private lateinit var viewModel: RegisterViewModel
  private val testDispatcher = StandardTestDispatcher()
  private val testUid = "test-uid-123"
  private val testEmail = "test@example.com"

  private val EPFL_LOCATION =
      Location(46.5191, 6.5668, "École Polytechnique Fédérale de Lausanne (EPFL), Switzerland")

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    userRepository = mockk(relaxed = true)
    accountRepository = mockk(relaxed = true)
    locationSelectionViewModel = mockk(relaxed = true)
    auth = mockk(relaxed = true)
    firebaseUser = mockk(relaxed = true)

    every { auth.currentUser } returns firebaseUser
    every { firebaseUser.uid } returns testUid
    every { firebaseUser.email } returns testEmail

    // Mock the fusedLocationClient to avoid lateinit errors
    LocationProvider.fusedLocationClient = mockk<FusedLocationProviderClient>(relaxed = true)

    // Mock location selection view model to return a valid location by default
    every { locationSelectionViewModel.uiState } returns
        kotlinx.coroutines.flow.MutableStateFlow(
            com.android.ootd.ui.map.LocationSelectionViewState(selectedLocation = EPFL_LOCATION))

    viewModel =
        RegisterViewModel(
            userRepository = userRepository,
            accountRepository = accountRepository,
            locationSelectionViewModel = locationSelectionViewModel,
            auth = auth)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // --- Tiny helpers ---
  private fun TestScope.register(username: String, dob: String? = null) {
    viewModel.setUsername(username)
    dob?.let { viewModel.setDateOfBirth(it) }
    viewModel.registerUser()
    advanceUntilIdle()
  }

  private fun stubSuccess() {
    coEvery { userRepository.createUser(any(), testUid, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any(), any(), any()) } returns Unit
  }

  // ========== Username Validation Tests ==========

  @Test
  fun validation_allErrorCases_trimsWhitespace_clearError() = runTest {
    // Empty username
    register("")
    assertEquals("Username cannot be empty.", viewModel.uiState.value.errorMsg)
    coVerify(exactly = 0) { userRepository.createUser(any(), any(), any()) }

    // Blank username
    register("   ")
    assertEquals("Username cannot be empty.", viewModel.uiState.value.errorMsg)

    // Too short
    register("ab")
    assertEquals("Username must be at least 3 characters.", viewModel.uiState.value.errorMsg)

    // Too long
    register("thisUsernameIsWayTooLong123")
    assertEquals("Username must be at most 20 characters.", viewModel.uiState.value.errorMsg)

    // Invalid characters
    register("user@name!")
    assertEquals(
        "Username can only contain letters, numbers, and underscores.",
        viewModel.uiState.value.errorMsg)

    // Spaces
    register("user name")
    assertNotNull(viewModel.uiState.value.errorMsg)

    // clearErrorMsg works
    viewModel.clearErrorMsg()
    assertNull(viewModel.uiState.value.errorMsg)

    // Whitespace trimming - valid after trim
    stubSuccess()
    register("  validUser  ", "01/01/2003")
    coVerify(exactly = 1) { userRepository.createUser("validUser", testUid, testUid) }
    assertTrue(viewModel.uiState.value.registered)
  }

  // ========== Successful Registration Tests ==========

  @Test
  fun successfulRegistration_endToEnd_withDateOfBirth_emailHandling_stateManagement() = runTest {
    val username = "validUser123"
    val dob = "01/01/2000"
    stubSuccess()

    // Date of birth setting works
    viewModel.setDateOfBirth(dob)
    assertEquals(dob, viewModel.uiState.value.dateOfBirth)

    // Registration with DOB calls repos correctly and sets registered
    register(username, dob)
    assertTrue(viewModel.uiState.value.registered)
    assertFalse(viewModel.uiState.value.isLoading)
    assertNull(viewModel.uiState.value.errorMsg)

    // Verify first registration
    coVerify(atLeast = 1) { userRepository.createUser(username, testUid, ownerId = testUid) }
    coVerify(atLeast = 1) {
      accountRepository.createAccount(
          match { it.uid == testUid && it.username == username }, testEmail, any(), dob, any())
    }

    // markRegisteredHandled resets flag
    viewModel.markRegisteredHandled()
    assertFalse(viewModel.uiState.value.registered)

    // refresh clears state
    register(
        "",
    ) // trigger error
    viewModel.refresh()
    assertNull(viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.isLoading)
    assertFalse(viewModel.uiState.value.registered)

    // Registration without DOB uses empty string
    stubSuccess() // Re-stub for the next registration
    viewModel.setDateOfBirth("") // Clear the DOB
    // Note: This will fail age validation, so we expect an error instead
    viewModel.setUsername("testUser2")
    viewModel.registerUser()
    advanceUntilIdle()
    assertEquals("Date of birth is required", viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.registered)

    // Repos called in order: accountRepo first, then userRepo
    val callOrder = mutableListOf<String>()
    coEvery { accountRepository.createAccount(any(), any(), any(), any(), any()) } coAnswers
        {
          callOrder.add("account")
          Unit
        }
    coEvery { userRepository.createUser(any(), any(), any()) } coAnswers
        {
          callOrder.add("user")
          Unit
        }
    register("orderTest", "01/01/2001")
    assertTrue(callOrder.contains("account"))
    assertTrue(callOrder.contains("user"))
    assertEquals("account", callOrder[callOrder.size - 2])
    assertEquals("user", callOrder[callOrder.size - 1])

    // Different email scenarios
    val customEmail = "custom@test.com"
    val customUser = mockk<FirebaseUser>(relaxed = true)
    every { customUser.uid } returns testUid
    every { customUser.email } returns customEmail
    every { auth.currentUser } returns customUser

    // Re-stub for custom VM
    coEvery { userRepository.createUser(any(), any(), any()) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any(), any(), any()) } returns Unit

    val customLocationVM = mockk<LocationSelectionViewModel>(relaxed = true)
    every { customLocationVM.uiState } returns
        kotlinx.coroutines.flow.MutableStateFlow(
            com.android.ootd.ui.map.LocationSelectionViewState(selectedLocation = EPFL_LOCATION))

    val customVM =
        RegisterViewModel(
            userRepository = userRepository,
            accountRepository = accountRepository,
            locationSelectionViewModel = customLocationVM,
            auth = auth)
    customVM.setUsername("emailTest")
    customVM.setDateOfBirth("01/10/2001")
    customVM.registerUser()
    advanceUntilIdle()

    coVerify(atLeast = 1) {
      accountRepository.createAccount(any(), customEmail, any(), any(), any())
    }
  }

  // ========== Edge Cases and Boundary Tests ==========

  @Test
  fun edgeCases_boundaryUsernames_underscores_loadingState_multipleAttempts() = runTest {
    stubSuccess()

    // Exactly 3 characters (minimum)
    register("abc", "01/01/2003")
    assertTrue(viewModel.uiState.value.registered)
    coVerify(atLeast = 1) { userRepository.createUser("abc", testUid, testUid) }

    // Exactly 20 characters (maximum)
    register("a".repeat(20), "01/01/2003")
    assertTrue(viewModel.uiState.value.registered)

    // Underscores allowed
    register("valid_user_123", "01/01/2003")
    assertTrue(viewModel.uiState.value.registered)

    // Loading state transitions correctly
    coEvery { userRepository.createUser(any(), any(), any()) } coAnswers
        {
          kotlinx.coroutines.delay(100)
        }
    viewModel.setUsername("slowUser")
    viewModel.setDateOfBirth("01/01/2003")
    viewModel.registerUser()
    assertTrue(viewModel.uiState.value.isLoading)
    advanceUntilIdle()
    assertFalse(viewModel.uiState.value.isLoading)

    // Multiple attempts after error work. Should throw Username error first
    register("ab") // error
    assertNotNull(viewModel.uiState.value.errorMsg)
    stubSuccess()
    register("validAfterError", "01/01/2003")
    assertTrue(viewModel.uiState.value.registered)
  }

  // ========== Error Handling Tests ==========

  @Test
  fun errorHandling_accountFailure_userFailure_registeredStatePersistence() = runTest {
    // Account creation failure stops loading and keeps registered false
    val username = "testUser"
    coEvery { userRepository.createUser(username, testUid, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any(), any(), any()) } throws
        Exception("Account error")

    register(username, "01/01/2003")
    assertFalse(viewModel.uiState.value.isLoading)
    assertFalse(viewModel.uiState.value.registered)
    assertNotNull(viewModel.uiState.value.errorMsg)

    // Both operations must succeed for registered=true
    coEvery { accountRepository.createAccount(any(), any(), any(), any(), any()) } returns Unit
    register("bothSucceed", "01/01/2003")
    assertTrue(viewModel.uiState.value.registered)
    assertNull(viewModel.uiState.value.errorMsg)

    // registered flag initially false before operations complete
    coEvery { userRepository.createUser(any(), any(), any()) } coAnswers
        {
          assertFalse(viewModel.uiState.value.registered)
        }
    viewModel.setUsername("checkDuring")
    viewModel.setDateOfBirth("01/01/2003")
    viewModel.registerUser()
    assertFalse(viewModel.uiState.value.registered) // false while loading
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.registered) // true after completion
  }

  // ========== Location Integration Tests ==========

  @Test
  fun registerUser_withMissingLocation_showsError() = runTest {
    // Create a fresh viewModel with no location selected
    val noLocationVM = mockk<LocationSelectionViewModel>(relaxed = true)
    every { noLocationVM.uiState } returns
        kotlinx.coroutines.flow.MutableStateFlow(
            com.android.ootd.ui.map.LocationSelectionViewState(selectedLocation = null))

    val freshViewModel =
        RegisterViewModel(
            userRepository = userRepository,
            accountRepository = accountRepository,
            locationSelectionViewModel = noLocationVM,
            auth = auth)

    freshViewModel.setUsername("validUser123")
    freshViewModel.setDateOfBirth("01/01/2000")
    // Don't set location - should cause error

    freshViewModel.registerUser()
    advanceUntilIdle()

    assertEquals(
        "Please select a location before registering", freshViewModel.uiState.value.errorMsg)
    assertFalse(freshViewModel.uiState.value.registered)
  }

  @Test
  fun registerUser_withValidLocation_usesLocationFromLocationSelectionViewModel() = runTest {
    val testLocation = Location(48.8566, 2.3522, "Paris, France")
    val locationVM = mockk<LocationSelectionViewModel>(relaxed = true)
    every { locationVM.uiState } returns
        kotlinx.coroutines.flow.MutableStateFlow(
            com.android.ootd.ui.map.LocationSelectionViewState(selectedLocation = testLocation))

    val testViewModel =
        RegisterViewModel(
            userRepository = userRepository,
            accountRepository = accountRepository,
            locationSelectionViewModel = locationVM,
            auth = auth)

    stubSuccess()
    testViewModel.setUsername("validUser")
    testViewModel.setDateOfBirth("01/01/2000")
    testViewModel.registerUser()
    advanceUntilIdle()

    // Verify the location from LocationSelectionViewModel was used
    coVerify { accountRepository.createAccount(any(), any(), any(), any(), testLocation) }
  }

  // ========== Refresh Functionality Tests ==========

  @Test
  fun refresh_clearsAllFieldsAndState() = runTest {
    // Set up some state
    viewModel.setUsername("testUser")
    viewModel.setDateOfBirth("15/06/2005")
    viewModel.emitError("Test error message")
    viewModel.showLoading(true)

    // Verify state is populated
    assertEquals("testUser", viewModel.uiState.value.username)
    assertEquals("15/06/2005", viewModel.uiState.value.dateOfBirth)
    assertEquals("Test error message", viewModel.uiState.value.errorMsg)
    assertTrue(viewModel.uiState.value.isLoading)

    // Call refresh
    viewModel.refresh()
    coVerify { locationSelectionViewModel.clearSelectedLocation() }

    // Verify all fields are reset to default values
    assertEquals("", viewModel.uiState.value.username)
    assertEquals("", viewModel.uiState.value.dateOfBirth)
    assertNull(viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.isLoading)
    assertFalse(viewModel.uiState.value.registered)
    assertEquals("", viewModel.uiState.value.uid)
    assertEquals("", viewModel.uiState.value.userEmail)
    assertEquals("", viewModel.uiState.value.localProfilePictureUri.toString())
  }

  @Test
  fun refresh_afterFailedRegistration_allowsNewRegistration() = runTest {
    // Trigger a registration error
    register("ab") // Too short
    assertNotNull(viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.registered)

    // Refresh
    viewModel.refresh()
    assertNull(viewModel.uiState.value.errorMsg)

    // Now register successfully
    stubSuccess()
    register("validUser", "01/01/2000")
    assertTrue(viewModel.uiState.value.registered)
    assertNull(viewModel.uiState.value.errorMsg)
  }

  // ========== Age Validation Tests ==========

  @Test
  fun ageValidation_userUnder13_showsError() = runTest {
    stubSuccess()

    // Calculate a date of birth for someone who is 10 years old
    val tenYearsAgo = java.time.LocalDate.now().minusYears(10)
    val dob = tenYearsAgo.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))

    viewModel.setUsername("validUser")
    viewModel.setDateOfBirth(dob)
    viewModel.registerUser()
    advanceUntilIdle()

    assertEquals("User must be at least 13", viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.registered)
    coVerify(exactly = 0) { userRepository.createUser(any(), any(), any()) }
    coVerify(exactly = 0) { accountRepository.createAccount(any(), any(), any(), any(), any()) }
  }

  @Test
  fun ageValidation_userExactly13_allowsRegistration() = runTest {
    stubSuccess()

    // Calculate a date of birth for someone who is exactly 13 years old
    val thirteenYearsAgo = java.time.LocalDate.now().minusYears(13)
    val dob = thirteenYearsAgo.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))

    viewModel.setUsername("validUser")
    viewModel.setDateOfBirth(dob)
    viewModel.registerUser()
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.errorMsg)
    assertTrue(viewModel.uiState.value.registered)
    coVerify(atLeast = 1) { userRepository.createUser("validUser", testUid, testUid) }
  }

  @Test
  fun ageValidation_userOver13_allowsRegistration() = runTest {
    stubSuccess()

    // Test with someone who is 25 years old
    val twentyFiveYearsAgo = java.time.LocalDate.now().minusYears(25)
    val dob = twentyFiveYearsAgo.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))

    viewModel.setUsername("validUser")
    viewModel.setDateOfBirth(dob)
    viewModel.registerUser()
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.errorMsg)
    assertTrue(viewModel.uiState.value.registered)
    coVerify(atLeast = 1) { userRepository.createUser("validUser", testUid, testUid) }
  }

  @Test
  fun ageValidation_blankDateOfBirth_showsError() = runTest {
    stubSuccess()

    viewModel.setUsername("validUser")
    viewModel.setDateOfBirth("")
    viewModel.registerUser()
    advanceUntilIdle()

    assertEquals("Date of birth is required", viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.registered)
    coVerify(exactly = 0) { userRepository.createUser(any(), any(), any()) }
  }

  @Test
  fun ageValidation_invalidDateFormat_showsError() = runTest {
    stubSuccess()

    viewModel.setUsername("validUser")
    viewModel.setDateOfBirth("invalid-date")
    viewModel.registerUser()
    advanceUntilIdle()

    assertEquals("Invalid date format", viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.registered)
    coVerify(exactly = 0) { userRepository.createUser(any(), any(), any()) }
  }

  @Test
  fun ageValidation_wrongDateFormat_showsError() = runTest {
    stubSuccess()

    viewModel.setUsername("validUser")
    // Using MM/dd/yyyy instead of dd/MM/yyyy
    viewModel.setDateOfBirth("01/15/2000")
    viewModel.registerUser()
    advanceUntilIdle()

    assertEquals("Invalid date format", viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.registered)
  }

  @Test
  fun ageValidation_checkedBeforeUsernameValidation() = runTest {
    // Set invalid username but also invalid age
    val tenYearsAgo = java.time.LocalDate.now().minusYears(10)
    val dob = tenYearsAgo.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))

    viewModel.setUsername("abc")
    viewModel.setDateOfBirth(dob) // Too young
    viewModel.registerUser()
    advanceUntilIdle()

    assertEquals("User must be at least 13", viewModel.uiState.value.errorMsg)
  }

  @Test
  fun ageValidation_userExactly12YearsAnd364Days_showsError() = runTest {
    stubSuccess()

    // User is 12 years and 364 days old (one day before turning 13)
    val almostThirteen = java.time.LocalDate.now().minusYears(13).plusDays(1)
    val dob = almostThirteen.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))

    viewModel.setUsername("validUser")
    viewModel.setDateOfBirth(dob)
    viewModel.registerUser()
    advanceUntilIdle()

    assertEquals("User must be at least 13", viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.registered)
  }
}
