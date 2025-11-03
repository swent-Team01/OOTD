/**
 * Authentication and navigation tests for the OOTD app. Tests cover SignInViewModel, SignInScreen
 * UI, SplashViewModel, and integration flows.
 */
package com.android.ootd.authentication

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.OOTDApp
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.authentication.AccountServiceFirebase
import com.android.ootd.model.user.UserRepositoryFirestore
import com.android.ootd.ui.authentication.*
import com.android.ootd.ui.navigation.Screen
import com.android.ootd.utils.FakeCredentialManager
import com.android.ootd.utils.FakeJwtGenerator
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthenticationExtensiveTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var mockAccountService: AccountService

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    mockAccountService = mockk(relaxed = true)
    FirebaseAuth.getInstance().signOut()
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  // ========================================================================
  // SignInViewModel Tests
  // ========================================================================

  @Test
  fun signInViewModel_initialState() {
    val viewModel = SignInViewModel(mockAccountService)
    val state = viewModel.uiState.value

    assertFalse(state.isLoading)
    assertNull(state.user)
    assertNull(state.errorMsg)
    assertFalse(state.signedOut)
  }

  @Test
  fun signInViewModel_clearErrorMsg() {
    val viewModel = SignInViewModel(mockAccountService)
    val stateFlow = viewModel.uiState as MutableStateFlow
    stateFlow.value = stateFlow.value.copy(errorMsg = "Test error")

    viewModel.clearErrorMsg()

    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun signInViewModel_successfulLogin() = runTest {
    val fakeToken = FakeJwtGenerator.createFakeGoogleIdToken("User", email = "test@example.com")
    val fakeCredManager = FakeCredentialManager.create(fakeToken)

    composeTestRule.setContent {
      SignInScreen(credentialManager = fakeCredManager, onSignedIn = {})
    }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(SignInScreenTestTags.LOGIN_BUTTON)
          .fetchSemanticsNodes()
          .isEmpty()
    }
  }

  @Test
  fun signInViewModel_failedLogin() = runTest {
    val mockCredManager = mockk<androidx.credentials.CredentialManager>()
    val mockCredential = mockk<androidx.credentials.CustomCredential>()
    val mockResponse = mockk<androidx.credentials.GetCredentialResponse>()
    every { mockResponse.credential } returns mockCredential

    coEvery { mockAccountService.signInWithGoogle(any<androidx.credentials.Credential>()) } returns
        Result.failure(Exception("Auth failed"))
    coEvery {
      mockCredManager.getCredential(
          any<Context>(), any<androidx.credentials.GetCredentialRequest>())
    } returns mockResponse

    val viewModel = SignInViewModel(mockAccountService)
    viewModel.signIn(context, mockCredManager)

    composeTestRule.waitUntil(timeoutMillis = 5000) { viewModel.uiState.value.errorMsg != null }

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNotNull(state.errorMsg)
    assertTrue(state.signedOut)
  }

  @Test
  fun signInViewModel_userCancellation() = runTest {
    val mockCredManager = mockk<androidx.credentials.CredentialManager>()
    coEvery {
      mockCredManager.getCredential(
          any<Context>(), any<androidx.credentials.GetCredentialRequest>())
    } throws Exception("User cancelled")

    val viewModel = SignInViewModel(mockAccountService)
    viewModel.signIn(context, mockCredManager)

    composeTestRule.waitUntil(timeoutMillis = 5000) { viewModel.uiState.value.errorMsg != null }

    assertTrue(viewModel.uiState.value.signedOut)
  }

  @Test
  fun signInViewModel_credentialException() = runTest {
    val realException =
        androidx.credentials.exceptions.GetCredentialUnknownException("Credential error occurred")

    val mockCredManager = mockk<androidx.credentials.CredentialManager>()
    coEvery {
      mockCredManager.getCredential(
          any<Context>(), any<androidx.credentials.GetCredentialRequest>())
    } throws realException

    val viewModel = SignInViewModel(mockAccountService)

    viewModel.signIn(context, mockCredManager)

    composeTestRule.waitUntil(timeoutMillis = 5000) { viewModel.uiState.value.errorMsg != null }

    val finalState = viewModel.uiState.value
    assertFalse(finalState.isLoading)
    assertNotNull(finalState.errorMsg)
    assertTrue(finalState.signedOut)
  }

  @Test
  fun signInViewModel_unexpectedException() = runTest {
    val mockCredManager = mockk<androidx.credentials.CredentialManager>()
    coEvery {
      mockCredManager.getCredential(
          any<Context>(), any<androidx.credentials.GetCredentialRequest>())
    } throws RuntimeException("Unexpected error")

    val viewModel = SignInViewModel(mockAccountService)

    viewModel.signIn(context, mockCredManager)

    composeTestRule.waitUntil(timeoutMillis = 5000) { viewModel.uiState.value.errorMsg != null }

    val finalState = viewModel.uiState.value
    assertTrue(finalState.errorMsg?.contains("Unexpected error") == true)
  }

  @Test
  fun signInViewModel_ignoresMultipleConcurrentCalls() = runTest {
    var callCount = 0
    val mockCredManager = mockk<androidx.credentials.CredentialManager>()
    val mockCredential = mockk<androidx.credentials.CustomCredential>()
    val mockResponse = mockk<androidx.credentials.GetCredentialResponse>()
    every { mockResponse.credential } returns mockCredential

    coEvery {
      mockCredManager.getCredential(
          any<Context>(), any<androidx.credentials.GetCredentialRequest>())
    } coAnswers
        {
          callCount++
          kotlinx.coroutines.delay(200)
          mockResponse
        }

    val viewModel = SignInViewModel(mockAccountService)
    viewModel.signIn(context, mockCredManager)

    composeTestRule.waitUntil(timeoutMillis = 2000) { viewModel.uiState.value.isLoading }

    viewModel.signIn(context, mockCredManager)
    viewModel.signIn(context, mockCredManager)

    composeTestRule.waitForIdle()

    assertEquals(1, callCount)
  }

  // ========================================================================
  // SplashViewModel Tests
  // ========================================================================

  @Test
  fun splashViewModel_userSignedInAndExists() = runTest {
    val mockUserRepo = mockk<UserRepositoryFirestore>(relaxed = true)
    coEvery { mockAccountService.hasUser() } returns true
    coEvery { mockAccountService.currentUserId } returns "uid-123"
    coEvery { mockUserRepo.userExists("uid-123") } returns true

    var signedIn = false
    var notSignedIn = false

    val viewModel = SplashViewModel(mockAccountService)
    viewModel.onAppStart(
        userRepository = mockUserRepo,
        onSignedIn = { signedIn = true },
        onNotSignedIn = { notSignedIn = true })

    composeTestRule.waitForIdle()

    assertTrue(signedIn)
    assertFalse(notSignedIn)
  }

  @Test
  fun splashViewModel_userSignedInButNotInDatabase() = runTest {
    val mockUserRepo = mockk<UserRepositoryFirestore>(relaxed = true)
    coEvery { mockAccountService.hasUser() } returns true
    coEvery { mockAccountService.currentUserId } returns "uid-456"
    coEvery { mockUserRepo.userExists("uid-456") } returns false

    var signedIn = false
    var notSignedIn = false

    val viewModel = SplashViewModel(mockAccountService)
    viewModel.onAppStart(
        userRepository = mockUserRepo,
        onSignedIn = { signedIn = true },
        onNotSignedIn = { notSignedIn = true })

    composeTestRule.waitForIdle()

    assertFalse(signedIn)
    assertTrue(notSignedIn)
  }

  @Test
  fun splashViewModel_userNotSignedIn() = runTest {
    coEvery { mockAccountService.hasUser() } returns false

    var notSignedIn = false

    val viewModel = SplashViewModel(mockAccountService)
    viewModel.onAppStart(onNotSignedIn = { notSignedIn = true })

    composeTestRule.waitForIdle()

    assertTrue(notSignedIn)
  }

  @Test
  fun splashViewModel_exceptionHandled() = runTest {
    coEvery { mockAccountService.hasUser() } throws Exception("Network error")

    var notSignedIn = false

    val viewModel = SplashViewModel(mockAccountService)
    viewModel.onAppStart(onNotSignedIn = { notSignedIn = true })

    composeTestRule.waitForIdle()

    assertTrue(notSignedIn)
  }

  // ========================================================================
  // AccountServiceFirebase Tests
  // ========================================================================

  @Test
  fun accountService_currentUserId() {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockUser = mockk<FirebaseUser>(relaxed = true)

    every { mockAuth.currentUser } returns null
    assertEquals("", AccountServiceFirebase(auth = mockAuth).currentUserId)

    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "test-uid"
    assertEquals("test-uid", AccountServiceFirebase(auth = mockAuth).currentUserId)
  }

  @Test
  fun accountService_hasUser() = runTest {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)

    every { mockAuth.currentUser } returns null
    assertFalse(AccountServiceFirebase(auth = mockAuth).hasUser())

    every { mockAuth.currentUser } returns mockk()
    assertTrue(AccountServiceFirebase(auth = mockAuth).hasUser())
  }

  // ========================================================================
  // Screen Properties Tests
  // ========================================================================

  @Test
  fun screen_properties() {
    assertEquals("authentication", Screen.Authentication.route)
    assertTrue(Screen.Authentication.isTopLevelDestination)

    assertEquals("splash", Screen.Splash.route)
    assertFalse(Screen.Splash.isTopLevelDestination)
  }

  // ========================================================================
  // SignInScreen UI Tests
  // ========================================================================

  @Test
  fun signInScreen_displaysComponents() {
    composeTestRule.setContent { SignInScreen() }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.APP_LOGO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_TITLE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("WELCOME").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sign in with Google").assertIsDisplayed()
  }

  @Test
  fun signInScreen_showsLoadingWhenLoading() {
    val mockViewModel = mockk<SignInViewModel>(relaxed = true)
    every { mockViewModel.uiState } returns MutableStateFlow(AuthUIState(isLoading = true))

    composeTestRule.setContent { SignInScreen(authViewModel = mockViewModel) }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertDoesNotExist()
  }

  @Test
  fun signInScreen_buttonClickTriggersSignIn() {
    val mockViewModel = mockk<SignInViewModel>(relaxed = true)
    every { mockViewModel.uiState } returns MutableStateFlow(AuthUIState())
    every {
      mockViewModel.signIn(any<Context>(), any<androidx.credentials.CredentialManager>())
    } just Runs

    composeTestRule.setContent { SignInScreen(authViewModel = mockViewModel) }

    composeTestRule
        .onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON)
        .performScrollTo()
        .performClick()

    verify { mockViewModel.signIn(any<Context>(), any<androidx.credentials.CredentialManager>()) }
  }

  // ========================================================================
  // SplashScreen UI Tests
  // ========================================================================

  @Test
  fun splashScreen_displaysContent() {
    composeTestRule.setContent { SplashScreenContent() }

    composeTestRule.onNodeWithTag("splashLogo").assertIsDisplayed()
    composeTestRule.onNodeWithTag("splashProgress").assertIsDisplayed()
  }

  @Test
  fun splashScreen_callsOnAppStart() = runTest {
    val mockViewModel = mockk<SplashViewModel>(relaxed = true)
    every { mockViewModel.onAppStart(any(), any(), any()) } just Runs

    composeTestRule.setContent { SplashScreen(viewModel = mockViewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1500)

    verify(timeout = 3000) { mockViewModel.onAppStart(any(), any(), any()) }
  }

  // ========================================================================
  // Integration Tests
  // ========================================================================

  @Test
  fun ootdApp_navigatesToSignInWhenNotAuthenticated() {
    FirebaseAuth.getInstance().signOut()

    composeTestRule.setContent { OOTDApp() }

    composeTestRule.waitUntil(timeoutMillis = 3000) {
      composeTestRule
          .onAllNodesWithTag(SignInScreenTestTags.LOGIN_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()
  }

  @Test
  fun signIn_navigatesToRegisterWhenUserHasNoName() = runTest {
    val signInMocks = setupMocksForSignIn("uid-no-name", userExists = false)
    val viewModel = signInMocks.viewModel

    var wentToRegister = false
    var wentToOverview = false

    composeTestRule.setContent {
      SignInScreen(
          authViewModel = viewModel,
          credentialManager =
              FakeCredentialManager.create(
                  FakeJwtGenerator.createFakeGoogleIdToken("No Name", "no.name@example.com")),
          onRegister = { wentToRegister = true },
          onSignedIn = { wentToOverview = true })
    }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
    composeTestRule.waitUntil(timeoutMillis = 10000) { wentToRegister || wentToOverview }

    assertTrue(wentToRegister)
    assertFalse(wentToOverview)

    unmockkStatic(FirebaseAuth::class)
  }

  @Test
  fun signIn_navigatesToOverviewWhenUserHasName() = runTest {
    val signInMocks = setupMocksForSignIn("uid-has-name", userExists = true)
    val viewModel = signInMocks.viewModel

    var wentToRegister = false
    var wentToOverview = false

    composeTestRule.setContent {
      SignInScreen(
          authViewModel = viewModel,
          credentialManager =
              FakeCredentialManager.create(
                  FakeJwtGenerator.createFakeGoogleIdToken("Has Name", "has.name@example.com")),
          onRegister = { wentToRegister = true },
          onSignedIn = { wentToOverview = true })
    }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
    composeTestRule.waitUntil(timeoutMillis = 10000) { wentToRegister || wentToOverview }

    assertFalse(wentToRegister)
    assertTrue(wentToOverview)

    unmockkStatic(FirebaseAuth::class)
  }

  @Test
  fun signIn_invokesUserExistsWithCurrentUid() = runTest {
    val signInMocks = setupMocksForSignIn("uid-to-check", userExists = false)
    val viewModel = signInMocks.viewModel

    var navigationCalled = false

    composeTestRule.setContent {
      SignInScreen(
          authViewModel = viewModel,
          credentialManager =
              FakeCredentialManager.create(
                  FakeJwtGenerator.createFakeGoogleIdToken("Check UID", "check.uid@example.com")),
          onRegister = { navigationCalled = true },
          onSignedIn = { navigationCalled = true })
    }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()
    composeTestRule.waitUntil(timeoutMillis = 10000) { navigationCalled }

    coVerify(atLeast = 1) { signInMocks.userRepo.userExists("uid-to-check") }

    unmockkStatic(FirebaseAuth::class)
  }

  // ========================================================================
  // Helper Functions
  // ========================================================================

  private fun setupMocksForSignIn(uid: String, userExists: Boolean): SignInMocks {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockUser = mockk<FirebaseUser>(relaxed = true)
    val mockAuthResult = mockk<AuthResult>(relaxed = true)
    val mockUserRepo = mockk<UserRepositoryFirestore>(relaxed = true)

    every { mockUser.uid } returns uid
    every { mockAuthResult.user } returns mockUser

    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockAuth
    every { mockAuth.currentUser } returns mockUser
    every { mockAuth.signInWithCredential(any()) } returns Tasks.forResult(mockAuthResult)

    coEvery { mockUserRepo.userExists(uid) } returns userExists

    val viewModel =
        SignInViewModel(
            repository = AccountServiceFirebase(auth = mockAuth), userRepository = mockUserRepo)

    return SignInMocks(mockAuth, mockUser, mockUserRepo, viewModel)
  }

  private data class SignInMocks(
      val auth: FirebaseAuth,
      val user: FirebaseUser,
      val userRepo: UserRepositoryFirestore,
      val viewModel: SignInViewModel
  )
}
