package com.android.ootd.model.user

import com.android.ootd.ui.register.RegisterViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

  private lateinit var repository: UserRepository
  private lateinit var auth: FirebaseAuth
  private lateinit var firebaseUser: FirebaseUser
  private lateinit var viewModel: RegisterViewModel
  private val testDispatcher = StandardTestDispatcher()
  private val testUid = "test-uid-123"

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    repository = mockk(relaxed = true)
    auth = mockk(relaxed = true)
    firebaseUser = mockk(relaxed = true)

    every { auth.currentUser } returns firebaseUser
    every { firebaseUser.uid } returns testUid

    viewModel = RegisterViewModel(repository, auth)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun registerUserWithValidUsername_callsRepositoryCreateUser() = runTest {
    val username = "validUser123"
    coEvery { repository.createUser(username, testUid) } returns Unit

    viewModel.setUsername(username)
    viewModel.registerUser()
    advanceUntilIdle()

    coVerify(exactly = 1) { repository.createUser(username, testUid) }
  }

  @Test
  fun registerUserWithValidUsername_setsRegisteredToTrueOnSuccess() = runTest {
    val username = "validUser123"
    coEvery { repository.createUser(username, testUid) } returns Unit

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
    coVerify(exactly = 0) { repository.createUser(any(), any()) }
  }

  @Test
  fun registerUserWithUsernameLongerThan20Characters_showsError() = runTest {
    viewModel.setUsername("thisUsernameIsWayTooLong123")
    viewModel.registerUser()
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMsg)
    assertEquals("Username must be at most 20 characters.", viewModel.uiState.value.errorMsg)
    coVerify(exactly = 0) { repository.createUser(any(), any()) }
  }

  @Test
  fun registerUserWithEmptyUsername_showsError() = runTest {
    viewModel.setUsername("")
    viewModel.registerUser()
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMsg)
    assertEquals("Username cannot be empty.", viewModel.uiState.value.errorMsg)
    coVerify(exactly = 0) { repository.createUser(any(), any()) }
  }

  @Test
  fun registerUserWithBlankUsername_showsError() = runTest {
    viewModel.setUsername("   ")
    viewModel.registerUser()
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMsg)
    assertEquals("Username cannot be empty.", viewModel.uiState.value.errorMsg)
    coVerify(exactly = 0) { repository.createUser(any(), any()) }
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
    coVerify(exactly = 0) { repository.createUser(any(), any()) }
  }

  @Test
  fun registerUserWithSpacesInUsername_showsError() = runTest {
    viewModel.setUsername("user name")
    viewModel.registerUser()
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMsg)
    coVerify(exactly = 0) { repository.createUser(any(), any()) }
  }

  @Test
  fun registerUser_trimsWhitespaceBeforeValidation() = runTest {
    val username = "  validUser  "
    coEvery { repository.createUser("validUser", testUid) } returns Unit

    viewModel.setUsername(username)
    viewModel.registerUser()
    advanceUntilIdle()

    coVerify(exactly = 1) { repository.createUser("validUser", testUid) }
  }

  @Test
  fun registerUser_setsIsLoadingToTrueDuringRegistration() = runTest {
    val username = "validUser"
    coEvery { repository.createUser(username, testUid) } coAnswers { kotlinx.coroutines.delay(100) }

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
    coEvery { repository.createUser(username, testUid) } returns Unit

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
    coEvery { repository.createUser(username, testUid) } returns Unit

    viewModel.setUsername(username)
    viewModel.registerUser()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.registered)
    coVerify(exactly = 1) { repository.createUser(username, testUid) }
  }

  @Test
  fun registerUserWithExactly3Characters_succeeds() = runTest {
    val username = "abc"
    coEvery { repository.createUser(username, testUid) } returns Unit

    viewModel.setUsername(username)
    viewModel.registerUser()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.registered)
    coVerify(exactly = 1) { repository.createUser(username, testUid) }
  }

  @Test
  fun registerUserWithExactly20Characters_succeeds() = runTest {
    val username = "a".repeat(20)
    coEvery { repository.createUser(username, testUid) } returns Unit

    viewModel.setUsername(username)
    viewModel.registerUser()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.registered)
    coVerify(exactly = 1) { repository.createUser(username, testUid) }
  }

  @Test
  fun multipleRegisterUserCallsWithErrors_doNotInterfere() = runTest {
    viewModel.setUsername("ab")
    viewModel.registerUser()
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMsg)

    val validUsername = "validUser"
    coEvery { repository.createUser(validUsername, testUid) } returns Unit

    viewModel.setUsername(validUsername)
    viewModel.registerUser()
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.registered)
  }
}
