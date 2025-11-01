package com.android.ootd.model.user

import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.authentication.AccountService
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
  private lateinit var accountService: AccountService

  private lateinit var locationRepository: LocationRepository
  private lateinit var auth: FirebaseAuth
  private lateinit var firebaseUser: FirebaseUser
  private lateinit var viewModel: RegisterViewModel
  private val testDispatcher = StandardTestDispatcher()
  private val testUid = "test-uid-123"
  private val testEmail = "test@example.com"

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    userRepository = mockk(relaxed = true)
    accountRepository = mockk(relaxed = true)
    accountService = mockk(relaxed = true)
    auth = mockk(relaxed = true)
    firebaseUser = mockk(relaxed = true)

    every { auth.currentUser } returns firebaseUser
    every { firebaseUser.uid } returns testUid
    every { firebaseUser.email } returns testEmail
    every { accountService.currentUser } returns flowOf(firebaseUser)

    viewModel =
        RegisterViewModel(
            userRepository, accountRepository, accountService, locationRepository, auth)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun registerUserWithValidUsername_callsRepositoryCreateUser() = runTest {
    val username = "validUser123"
    val dateOfBirth = "01/01/2000"
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    viewModel.setDateOfBirth(dateOfBirth)
    viewModel.registerUser()
    advanceUntilIdle()

    coVerify(exactly = 1) { userRepository.createUser(username, testUid) }
    coVerify(exactly = 1) {
      accountRepository.createAccount(
          match { it.uid == testUid && it.username == username }, testEmail, dateOfBirth)
    }
  }

  @Test
  fun registerUserWithValidUsername_setsRegisteredToTrueOnSuccess() = runTest {
    val username = "validUser123"
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any()) } returns Unit

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
  fun registerUserWithUsernameLongerThan20Characters_showsError() = runTest {
    viewModel.setUsername("thisUsernameIsWayTooLong123")
    viewModel.registerUser()
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMsg)
    assertEquals("Username must be at most 20 characters.", viewModel.uiState.value.errorMsg)
    coVerify(exactly = 0) { userRepository.createUser(any(), any()) }
  }

  @Test
  fun registerUserWithEmptyUsername_showsError() = runTest {
    viewModel.setUsername("")
    viewModel.registerUser()
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMsg)
    assertEquals("Username cannot be empty.", viewModel.uiState.value.errorMsg)
    coVerify(exactly = 0) { userRepository.createUser(any(), any()) }
  }

  @Test
  fun registerUserWithBlankUsername_showsError() = runTest {
    viewModel.setUsername("   ")
    viewModel.registerUser()
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMsg)
    assertEquals("Username cannot be empty.", viewModel.uiState.value.errorMsg)
    coVerify(exactly = 0) { userRepository.createUser(any(), any()) }
  }

  @Test
  fun registerUserWithInvalidCharacters_showsError() = runTest {
    viewModel.setUsername("user@name!")
    viewModel.registerUser()
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMsg)
    assertEquals(
        "Username can only contain letters, numbers, and underscores.",
        viewModel.uiState.value.errorMsg)
    coVerify(exactly = 0) { userRepository.createUser(any(), any()) }
  }

  @Test
  fun registerUserWithSpacesInUsername_showsError() = runTest {
    viewModel.setUsername("user name")
    viewModel.registerUser()
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMsg)
    coVerify(exactly = 0) { userRepository.createUser(any(), any()) }
  }

  @Test
  fun registerUser_trimsWhitespaceBeforeValidation() = runTest {
    val username = "  validUser  "
    coEvery { userRepository.createUser("validUser", testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    viewModel.registerUser()
    advanceUntilIdle()

    coVerify(exactly = 1) { userRepository.createUser("validUser", testUid) }
    coVerify(exactly = 1) { accountRepository.createAccount(any(), any(), any()) }
  }

  @Test
  fun registerUser_setsIsLoadingToTrueDuringRegistration() = runTest {
    val username = "validUser"
    coEvery { userRepository.createUser(username, testUid) } coAnswers
        {
          kotlinx.coroutines.delay(100)
        }
    coEvery { accountRepository.createAccount(any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    viewModel.registerUser()

    assertTrue(viewModel.uiState.value.isLoading)

    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.isLoading)
  }

  @Test
  fun clearErrorMsg_clearsErrorMessage() = runTest {
    viewModel.setUsername("")
    viewModel.registerUser()
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMsg)

    viewModel.clearErrorMsg()

    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun markRegisteredHandled_resetsRegisteredFlag() = runTest {
    val username = "validUser"
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    viewModel.registerUser()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.registered)

    viewModel.markRegisteredHandled()

    assertFalse(viewModel.uiState.value.registered)
  }

  @Test
  fun refresh_clearsErrorAndResetsState() = runTest {
    viewModel.setUsername("")
    viewModel.registerUser()
    advanceUntilIdle()

    viewModel.refresh()

    assertNull(viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.isLoading)
    assertFalse(viewModel.uiState.value.registered)
  }

  @Test
  fun registerUserWithValidUsernameContainingUnderscores_succeeds() = runTest {
    val username = "valid_user_123"
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    viewModel.registerUser()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.registered)
    coVerify(exactly = 1) { userRepository.createUser(username, testUid) }
    coVerify(exactly = 1) { accountRepository.createAccount(any(), any(), any()) }
  }

  @Test
  fun registerUserWithExactly3Characters_succeeds() = runTest {
    val username = "abc"
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    viewModel.registerUser()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.registered)
    coVerify(exactly = 1) { userRepository.createUser(username, testUid) }
    coVerify(exactly = 1) { accountRepository.createAccount(any(), any(), any()) }
  }

  @Test
  fun registerUserWithExactly20Characters_succeeds() = runTest {
    val username = "a".repeat(20)
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    viewModel.registerUser()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.registered)
    coVerify(exactly = 1) { userRepository.createUser(username, testUid) }
    coVerify(exactly = 1) { accountRepository.createAccount(any(), any(), any()) }
  }

  @Test
  fun multipleRegisterUserCallsWithErrors_doNotInterfere() = runTest {
    viewModel.setUsername("ab")
    viewModel.registerUser()
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMsg)

    val validUsername = "validUser"
    coEvery { userRepository.createUser(validUsername, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any()) } returns Unit

    viewModel.setUsername(validUsername)
    viewModel.registerUser()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.registered)
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
    coEvery { accountRepository.createAccount(any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    viewModel.setDateOfBirth(dateOfBirth)
    viewModel.registerUser()
    advanceUntilIdle()

    coVerify(exactly = 1) {
      accountRepository.createAccount(
          match { it.uid == testUid && it.username == username }, testEmail, dateOfBirth)
    }
  }

  @Test
  fun registerUser_usesEmptyDateIfNotSet() = runTest {
    val username = "testUser"
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    // Don't set dateOfBirth - should use default empty string
    viewModel.registerUser()
    advanceUntilIdle()

    coVerify(exactly = 1) {
      accountRepository.createAccount(
          match { it.uid == testUid && it.username == username }, testEmail, "")
    }
  }

  @Test
  fun registerUser_accountCreationFailure_stopsLoading() = runTest {
    val username = "testUser"
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any()) } throws
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

    coEvery { accountRepository.createAccount(any(), any(), any()) } coAnswers
        {
          callOrder.add("accountRepository")
        }
    coEvery { userRepository.createUser(username, testUid) } coAnswers
        {
          callOrder.add("userRepository")
        }

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
    coEvery { accountRepository.createAccount(any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    viewModel.setDateOfBirth(dateOfBirth)
    viewModel.registerUser()
    advanceUntilIdle()

    // registered should be true only when BOTH succeed
    assertTrue(viewModel.uiState.value.registered)
    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun registerUser_accountCreationFails_registeredRemainsFalse() = runTest {
    val username = "testUser"
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any()) } throws
        Exception("Account creation failed")

    viewModel.setUsername(username)
    viewModel.registerUser()
    advanceUntilIdle()

    // registered should remain false when account creation fails
    assertFalse(viewModel.uiState.value.registered)
    assertNotNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun registerUser_bothOperationsSucceed_noErrorMessage() = runTest {
    val username = "testUser"
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    viewModel.registerUser()
    advanceUntilIdle()

    assertNull(viewModel.uiState.value.errorMsg)
    assertTrue(viewModel.uiState.value.registered)
  }

  @Test
  fun registerUser_registeredInitiallyFalseBeforeOperations() = runTest {
    val username = "testUser"
    coEvery { userRepository.createUser(username, testUid) } coAnswers
        {
          // Check state during operation
          assertFalse(viewModel.uiState.value.registered)
        }
    coEvery { accountRepository.createAccount(any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    viewModel.registerUser()

    // registered should be false while loading
    assertFalse(viewModel.uiState.value.registered)

    advanceUntilIdle()

    // Now it should be true
    assertTrue(viewModel.uiState.value.registered)
  }

  // Email implementation tests
  @Test
  fun registerUser_passesCorrectEmailToAccountRepository() = runTest {
    val username = "testUser"
    val dateOfBirth = "01/01/2000"
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any()) } returns Unit

    viewModel.setUsername(username)
    viewModel.setDateOfBirth(dateOfBirth)
    viewModel.registerUser()
    advanceUntilIdle()

    coVerify(exactly = 1) {
      accountRepository.createAccount(
          match { it.uid == testUid && it.username == username }, testEmail, dateOfBirth)
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

    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any()) } returns Unit

    newViewModel.setUsername(username)
    newViewModel.registerUser()
    advanceUntilIdle()

    coVerify(exactly = 1) {
      accountRepository.createAccount(
          match { it.uid == testUid && it.username == username }, differentEmail, "")
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

    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any()) } returns Unit

    testViewModel.setUsername(username)
    testViewModel.registerUser()
    advanceUntilIdle()

    // Verify that the custom email is passed to createAccount
    coVerify(exactly = 1) {
      accountRepository.createAccount(match { it.username == username }, customEmail, "")
    }
  }
}
