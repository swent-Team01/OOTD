package com.android.ootd.model.user

import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.LocationRepository
import com.android.ootd.ui.register.RegisterViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
class RegisterUserRepositoryTest {

  private lateinit var userRepository: UserRepository
  private lateinit var accountRepository: AccountRepository
  private lateinit var accountService: AccountService

  private lateinit var locationRepository: LocationRepository
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
    accountService = mockk(relaxed = true)
    locationRepository = mockk(relaxed = true)
    auth = mockk(relaxed = true)
    firebaseUser = mockk(relaxed = true)

    every { auth.currentUser } returns firebaseUser
    every { firebaseUser.uid } returns testUid
    every { firebaseUser.email } returns testEmail
    every { accountService.currentUser } returns flowOf(firebaseUser)

    viewModel =
        RegisterViewModel(
            userRepository, accountRepository, accountService, locationRepository, auth)

    // Set a default location for all tests to prevent MissingLocationException
    viewModel.setLocation(EPFL_LOCATION)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // --- Tiny helpers ---
  private fun TestScope.register(username: String, dob: String? = null) {
  @Test
  fun registerUserWithValidUsername_callsRepositoryCreateUser() = runTest {
    val username = "validUser123"
    val dateOfBirth = "01/01/2000"
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    dob?.let { viewModel.setDateOfBirth(it) }
    viewModel.registerUser()
    advanceUntilIdle()
  }

  private fun stubSuccess() {
    coEvery { userRepository.createUser(any(), testUid, any()) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any()) } returns Unit

    coVerify(exactly = 1) { userRepository.createUser(username, testUid) }
    coVerify(exactly = 1) {
      accountRepository.createAccount(
          match { it.uid == testUid && it.username == username }, testEmail, dateOfBirth, any())
    }
  }

  @Test
  fun registerUserWithValidUsername_setsRegisteredToTrueOnSuccess() = runTest {
    val username = "validUser123"
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    viewModel.registerUser()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.registered)
    assertFalse(viewModel.uiState.value.isLoading)
  }

  @Test
  fun registerUserWithUsernameShorterThan3Characters_showsError() = runTest {
    viewModel.setUsername("ab")
    viewModel.registerUser()
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMsg)
    assertEquals("Username must be at least 3 characters.", viewModel.uiState.value.errorMsg)
    coVerify(exactly = 0) { userRepository.createUser(any(), any()) }
  }

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
    register("  validUser  ")
    coVerify(exactly = 1) { userRepository.createUser("validUser", testUid, any()) }
    assertTrue(viewModel.uiState.value.registered)
  }

  @Test
  fun successfulRegistration_endToEnd_withDateOfBirth_emailHandling_stateManagement() = runTest {
    val username = "validUser123"
    val dob = "01/01/2000"
    stubSuccess()
  @Test
  fun registerUser_trimsWhitespaceBeforeValidation() = runTest {
    val username = "  validUser  "
    coEvery { userRepository.createUser("validUser", testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    viewModel.registerUser()
    advanceUntilIdle()

    coVerify(exactly = 1) { userRepository.createUser("validUser", testUid) }
    coVerify(exactly = 1) { accountRepository.createAccount(any(), any(), any(), any()) }
  }

  @Test
  fun registerUser_setsIsLoadingToTrueDuringRegistration() = runTest {
    val username = "validUser"
    coEvery { userRepository.createUser(username, testUid) } coAnswers
        {
          kotlinx.coroutines.delay(100)
        }
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    viewModel.registerUser()

    assertTrue(viewModel.uiState.value.isLoading)

    // Date of birth setting works
    viewModel.setDateOfBirth(dob)
    assertEquals(dob, viewModel.uiState.value.dateOfBirth)

    // Registration with DOB calls repos correctly and sets registered
    register(username, dob)
    assertTrue(viewModel.uiState.value.registered)
    assertFalse(viewModel.uiState.value.isLoading)
    assertNull(viewModel.uiState.value.errorMsg)

    // Verify first registration
    coVerify(atLeast = 1) { userRepository.createUser(username, testUid, any()) }
    coVerify(atLeast = 1) {
      accountRepository.createAccount(
          match { it.uid == testUid && it.username == username }, testEmail, dob)
    }
  @Test
  fun markRegisteredHandled_resetsRegisteredFlag() = runTest {
    val username = "validUser"
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    viewModel.registerUser()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.registered)

    // markRegisteredHandled resets flag
    viewModel.markRegisteredHandled()
    assertFalse(viewModel.uiState.value.registered)

    // refresh clears state
    register("") // trigger error
    viewModel.refresh()
    assertNull(viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.isLoading)
    assertFalse(viewModel.uiState.value.registered)

    // Registration without DOB uses empty string
    stubSuccess() // Re-stub for the next registration
    viewModel.setDateOfBirth("") // Clear the DOB
    register("testUser2")
    coVerify(atLeast = 1) {
      accountRepository.createAccount(match { it.username == "testUser2" }, testEmail, "")
    }
  @Test
  fun registerUserWithValidUsernameContainingUnderscores_succeeds() = runTest {
    val username = "valid_user_123"
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } returns Unit

    // Repos called in order: accountRepo first, then userRepo
    val callOrder = mutableListOf<String>()
    coEvery { accountRepository.createAccount(any(), any(), any()) } coAnswers
        {
          callOrder.add("account")
          Unit
        }
    coEvery { userRepository.createUser(any(), any(), any()) } coAnswers
        {
          callOrder.add("user")
          Unit
        }
    register("orderTest")
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
    coEvery { accountRepository.createAccount(any(), any(), any()) } returns Unit
    assertTrue(viewModel.uiState.value.registered)
    coVerify(exactly = 1) { userRepository.createUser(username, testUid) }
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) }
  }

  @Test
  fun registerUserWithExactly3Characters_succeeds() = runTest {
    val username = "abc"
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } returns Unit

    val customVM = RegisterViewModel(userRepository, accountRepository, accountService, auth)
    customVM.setUsername("emailTest")
    customVM.registerUser()
    advanceUntilIdle()

    coVerify(atLeast = 1) { accountRepository.createAccount(any(), customEmail, any()) }
  }

  @Test
  fun edgeCases_boundaryUsernames_underscores_loadingState_multipleAttempts() = runTest {
    stubSuccess()
    assertTrue(viewModel.uiState.value.registered)
    coVerify(exactly = 1) { userRepository.createUser(username, testUid) }
    coVerify(exactly = 1) { accountRepository.createAccount(any(), any(), any(), any()) }
  }

  @Test
  fun registerUserWithExactly20Characters_succeeds() = runTest {
    val username = "a".repeat(20)
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    viewModel.registerUser()
    advanceUntilIdle()

    // Exactly 3 characters (minimum)
    register("abc")
    assertTrue(viewModel.uiState.value.registered)
    coVerify(exactly = 1) { userRepository.createUser("abc", testUid, any()) }
    coVerify(exactly = 1) { userRepository.createUser(username, testUid) }
    coVerify(exactly = 1) { accountRepository.createAccount(any(), any(), any(), any()) }
  }

  @Test
  fun multipleRegisterUserCallsWithErrors_doNotInterfere() = runTest {
    viewModel.setUsername("ab")
    viewModel.registerUser()
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMsg)

    val validUsername = "validUser"
    coEvery { userRepository.createUser(validUsername, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } returns Unit

    viewModel.setUsername(validUsername)
    viewModel.registerUser()
    advanceUntilIdle()

    // Exactly 20 characters (maximum)
    register("a".repeat(20))
    assertTrue(viewModel.uiState.value.registered)

    // Underscores allowed
    register("valid_user_123")
    assertTrue(viewModel.uiState.value.registered)

    // Loading state transitions correctly
    coEvery { userRepository.createUser(any(), any(), any()) } coAnswers
  }

  @Test
  fun enterDateWorksCorrectly() = runTest {
    val date = "12/12/2012"
    viewModel.setDateOfBirth(date)
    assertNotNull(viewModel.uiState.value.dateOfBirth)
    assertEquals(viewModel.uiState.value.dateOfBirth, date)
  }

  @Test
  fun registerUser_callsAccountRepositoryWithCorrectDateOfBirth() = runTest {
    val username = "testUser"
    val dateOfBirth = "15/03/1995"
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    viewModel.setDateOfBirth(dateOfBirth)
    viewModel.registerUser()
    advanceUntilIdle()

    coVerify(exactly = 1) {
      accountRepository.createAccount(
          match { it.uid == testUid && it.username == username }, testEmail, dateOfBirth, any())
    }
  }

  @Test
  fun registerUser_usesEmptyDateIfNotSet() = runTest {
    val username = "testUser"
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    // Don't set dateOfBirth - should use default empty string
    viewModel.registerUser()
    advanceUntilIdle()

    coVerify(exactly = 1) {
      accountRepository.createAccount(
          match { it.uid == testUid && it.username == username }, testEmail, "", any())
    }
  }

  @Test
  fun registerUser_accountCreationFailure_stopsLoading() = runTest {
    val username = "testUser"
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } throws
        Exception("Account error")

    viewModel.setUsername(username)
    viewModel.registerUser()
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isLoading)
  }

  @Test
  fun registerUser_bothRepositoriesCalledInOrder() = runTest {
    val username = "testUser"
    val dateOfBirth = "01/01/2000"
    val callOrder = mutableListOf<String>()

    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } coAnswers
        {
          callOrder.add("accountRepository")
        }
    coEvery { userRepository.createUser(username, testUid) } coAnswers
        {
          kotlinx.coroutines.delay(100)
        }
    viewModel.setUsername("slowUser")

    viewModel.setUsername(username)
    viewModel.setDateOfBirth(dateOfBirth)
    viewModel.registerUser()
    advanceUntilIdle()

    assertEquals(2, callOrder.size)
    assertEquals("accountRepository", callOrder[0])
    assertEquals("userRepository", callOrder[1])
  }

  @Test
  fun registerUser_bothSucceed_setsRegisteredToTrue() = runTest {
    val username = "testUser"
    val dateOfBirth = "01/01/2000"
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    viewModel.setDateOfBirth(dateOfBirth)
    viewModel.registerUser()
    assertTrue(viewModel.uiState.value.isLoading)
    advanceUntilIdle()
    assertFalse(viewModel.uiState.value.isLoading)

    // Multiple attempts after error work
    register("ab") // error
    assertNotNull(viewModel.uiState.value.errorMsg)
    stubSuccess()
    register("validAfterError")
    assertTrue(viewModel.uiState.value.registered)
  }

  @Test
  fun errorHandling_accountFailure_userFailure_registeredStatePersistence() = runTest {
    // Account creation failure stops loading and keeps registered false
    val username = "testUser"
    coEvery { userRepository.createUser(username, testUid, any()) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any()) } throws
        Exception("Account error")
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } throws
        Exception("Account creation failed")

    viewModel.setUsername(username)
    viewModel.registerUser()
    advanceUntilIdle()

    register(username)
    assertFalse(viewModel.uiState.value.isLoading)
    assertFalse(viewModel.uiState.value.registered)
    assertNotNull(viewModel.uiState.value.errorMsg)

    // Both operations must succeed for registered=true
    coEvery { accountRepository.createAccount(any(), any(), any()) } returns Unit
    register("bothSucceed")
  @Test
  fun registerUser_bothOperationsSucceed_noErrorMessage() = runTest {
    val username = "testUser"
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    viewModel.registerUser()
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.errorMsg)
    assertTrue(viewModel.uiState.value.registered)
    assertNull(viewModel.uiState.value.errorMsg)

    // registered flag initially false before operations complete
    coEvery { userRepository.createUser(any(), any(), any()) } coAnswers
        {
          assertFalse(viewModel.uiState.value.registered)
        }
    viewModel.setUsername("checkDuring")
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    viewModel.registerUser()
    assertFalse(viewModel.uiState.value.registered) // false while loading
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.registered) // true after completion

    // Now it should be true
    assertTrue(viewModel.uiState.value.registered)
  }

  // Email implementation tests
  @Test
  fun registerUser_passesCorrectEmailToAccountRepository() = runTest {
    val username = "testUser"
    val dateOfBirth = "01/01/2000"
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    viewModel.setDateOfBirth(dateOfBirth)
    viewModel.registerUser()
    advanceUntilIdle()

    coVerify(exactly = 1) {
      accountRepository.createAccount(
          match { it.uid == testUid && it.username == username }, testEmail, dateOfBirth, any())
    }
  }

  @Test
  fun registerUser_withDifferentEmail_usesCorrectEmail() = runTest {
    val username = "anotherUser"
    val differentEmail = "different@example.com"

    // Create a new firebase user with different email
    val differentFirebaseUser = mockk<FirebaseUser>(relaxed = true)
    every { differentFirebaseUser.uid } returns testUid
    every { differentFirebaseUser.email } returns differentEmail
    every { auth.currentUser } returns differentFirebaseUser
    every { accountService.currentUser } returns flowOf(differentFirebaseUser)

    // Create new viewModel with updated auth
    val newViewModel =
        RegisterViewModel(
            userRepository, accountRepository, accountService, locationRepository, auth)

    // Set location for the new viewModel instance
    newViewModel.setLocation(EPFL_LOCATION)

    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } returns Unit

    newViewModel.setUsername(username)
    newViewModel.registerUser()
    advanceUntilIdle()

    coVerify(exactly = 1) {
      accountRepository.createAccount(
          match { it.uid == testUid && it.username == username }, differentEmail, "", any())
    }
  }

  @Test
  fun registerUser_emailFromAccountService_isUsedInAccountCreation() = runTest {
    val username = "emailTestUser"
    val customEmail = "custom@test.com"

    // Setup a custom firebase user with specific email
    val customFirebaseUser = mockk<FirebaseUser>(relaxed = true)
    every { customFirebaseUser.uid } returns testUid
    every { customFirebaseUser.email } returns customEmail
    every { accountService.currentUser } returns flowOf(customFirebaseUser)
    every { auth.currentUser } returns customFirebaseUser

    val testViewModel =
        RegisterViewModel(
            userRepository, accountRepository, accountService, locationRepository, auth)

    // Set location for the test viewModel instance
    testViewModel.setLocation(EPFL_LOCATION)

    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } returns Unit

    testViewModel.setUsername(username)
    testViewModel.registerUser()
    advanceUntilIdle()

    // Verify that the custom email is passed to createAccount
    coVerify(exactly = 1) {
      accountRepository.createAccount(match { it.username == username }, customEmail, "", any())
    }
  }

  // Location functionality tests
  @Test
  fun setLocation_updatesSelectedLocationAndQuery() = runTest {
    val location = com.android.ootd.model.map.Location(48.8566, 2.3522, "Paris")

    viewModel.setLocation(location)

    assertEquals(location, viewModel.uiState.value.selectedLocation)
    assertEquals("Paris", viewModel.uiState.value.locationQuery)
  }

  @Test
  fun setLocationQuery_withEmptyQuery_clearsSuggestions() = runTest {
    viewModel.setLocationQuery("")
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.locationSuggestions.isEmpty())
    assertFalse(viewModel.uiState.value.isLoadingLocations)
  }

  @Test
  fun setLocationQuery_withNonEmptyQuery_fetchesSuggestions() = runTest {
    val mockLocations =
        listOf(Location(48.8566, 2.3522, "Paris"), Location(51.5074, -0.1278, "London"))
    coEvery { locationRepository.search("Par") } returns mockLocations

    viewModel.setLocationQuery("Par")
    advanceUntilIdle()

    assertEquals(mockLocations, viewModel.uiState.value.locationSuggestions)
    assertFalse(viewModel.uiState.value.isLoadingLocations)
  }

  @Test
  fun clearLocationSuggestions_clearsTheList() = runTest {
    val mockLocations = listOf(Location(48.8566, 2.3522, "Paris"))
    viewModel.setLocationSuggestions(mockLocations)

    viewModel.clearLocationSuggestions()

    assertTrue(viewModel.uiState.value.locationSuggestions.isEmpty())
  }

  @Test
  fun registerUser_withMissingLocation_showsError() = runTest {
    // Create a fresh viewModel without setting location
    val freshViewModel =
        RegisterViewModel(
            userRepository, accountRepository, accountService, locationRepository, auth)

    freshViewModel.setUsername("validUser123")
    freshViewModel.setDateOfBirth("01/01/2000")
    // Don't set location - should cause error

    freshViewModel.registerUser()
    advanceUntilIdle()

    assertEquals(
        "Please select a location before registering", freshViewModel.uiState.value.errorMsg)
    assertFalse(freshViewModel.uiState.value.registered)
  }
}
