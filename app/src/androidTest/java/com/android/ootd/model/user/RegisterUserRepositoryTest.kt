package com.android.ootd.model.user

import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.authentication.AccountService
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

    viewModel = RegisterViewModel(userRepository, accountRepository, accountService, auth)
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
    coEvery { userRepository.createUser(any(), testUid, any()) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any()) } returns Unit
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

    val customVM = RegisterViewModel(userRepository, accountRepository, accountService, auth)
    customVM.setUsername("emailTest")
    customVM.registerUser()
    advanceUntilIdle()

    coVerify(atLeast = 1) { accountRepository.createAccount(any(), customEmail, any()) }
  }

  @Test
  fun edgeCases_boundaryUsernames_underscores_loadingState_multipleAttempts() = runTest {
    stubSuccess()

    // Exactly 3 characters (minimum)
    register("abc")
    assertTrue(viewModel.uiState.value.registered)
    coVerify(exactly = 1) { userRepository.createUser("abc", testUid, any()) }

    // Exactly 20 characters (maximum)
    register("a".repeat(20))
    assertTrue(viewModel.uiState.value.registered)

    // Underscores allowed
    register("valid_user_123")
    assertTrue(viewModel.uiState.value.registered)

    // Loading state transitions correctly
    coEvery { userRepository.createUser(any(), any(), any()) } coAnswers
        {
          kotlinx.coroutines.delay(100)
        }
    viewModel.setUsername("slowUser")
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

    register(username)
    assertFalse(viewModel.uiState.value.isLoading)
    assertFalse(viewModel.uiState.value.registered)
    assertNotNull(viewModel.uiState.value.errorMsg)

    // Both operations must succeed for registered=true
    coEvery { accountRepository.createAccount(any(), any(), any()) } returns Unit
    register("bothSucceed")
    assertTrue(viewModel.uiState.value.registered)
    assertNull(viewModel.uiState.value.errorMsg)

    // registered flag initially false before operations complete
    coEvery { userRepository.createUser(any(), any(), any()) } coAnswers
        {
          assertFalse(viewModel.uiState.value.registered)
        }
    viewModel.setUsername("checkDuring")
    viewModel.registerUser()
    assertFalse(viewModel.uiState.value.registered) // false while loading
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.registered) // true after completion
  }
}
