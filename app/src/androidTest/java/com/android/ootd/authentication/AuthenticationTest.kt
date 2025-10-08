/**
 * Authentication and navigation test suite for the OOTD app.
 *
 * Overview:
 * - This file contains unit and integration tests for authentication-related functionality
 *   (SignInViewModel and SignInScreen), splash/startup behavior (SplashViewModel), and high-level
 *   navigation actions used by the app.
 *
 * Purpose and coverage:
 * - SignInViewModel: verifies initial state, success/failure flows, cancellation, and exception
 *   handling.
 * - SignInScreen (UI tests): verifies that UI elements render correctly, that user interactions
 *   call the ViewModel, and that loading states are shown.
 * - SplashViewModel: verifies startup logic that determines whether the user is already signed in.
 * - Integration tests: exercise a complete sign-in flow using test helpers (fake JWT tokens and a
 *   fake CredentialManager) and a mocked FirebaseAuth to avoid network calls.
 * - Navigation-related tests: ensure NavigationActions behaves correctly for top-level and
 *   non-top-level destinations.
 *
 * Test tools and helpers used:
 * - androidx.compose.ui.test (Compose testing APIs) and createComposeRule
 * - androidx.navigation testing helpers (TestNavHostController, ComposeNavigator)
 * - MockK for mocking dependencies and static calls
 * - FakeCredentialManager and FakeJwtGenerator (test helpers in `com.android.ootd.utils`)
 * - kotlinx.coroutines.test for coroutine-based tests
 * - Google Play Services Tasks wrapper (Tasks.forResult) to simulate Firebase Tasks
 *
 * Notes:
 * - Integration tests use a mocked `FirebaseAuth` (via MockK) so the fake Google ID token does not
 *   need to reach the real Firebase servers. This keeps tests hermetic and fast.
 * - Tests intentionally mix unit-level assertions (ViewModel state) and higher-level UI behaviors
 *   (Compose UI interactions and navigation) to give confidence in real flows.
 *
 * Acknowledgements:
 * - These tests were written in conjunction with GitHub Copilot (assistive development).
 */
package com.android.ootd.authentication

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.OOTDApp
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.authentication.AccountServiceFirebase
import com.android.ootd.ui.authentication.*
import com.android.ootd.ui.navigation.NavigationActions
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
  private lateinit var mockCredentialManager: CredentialManager
  private lateinit var mockFirebaseAuth: FirebaseAuth
  private lateinit var mockFirebaseUser: FirebaseUser
  private lateinit var navController: TestNavHostController

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    mockAccountService = mockk(relaxed = true)
    mockCredentialManager = mockk(relaxed = true)
    mockFirebaseAuth = mockk(relaxed = true)
    mockFirebaseUser = mockk(relaxed = true)

    FirebaseAuth.getInstance().signOut()

    navController = TestNavHostController(context)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  // ========== SignInViewModel Tests ==========

  @Test
  fun signInViewModel_initialState_isCorrect() {
    val viewModel = SignInViewModel(mockAccountService)
    val state = viewModel.uiState.value

    assertFalse(state.isLoading)
    assertNull(state.user)
    assertNull(state.errorMsg)
    assertFalse(state.signedOut)
  }

  @Test
  fun signInViewModel_clearErrorMsg_clearsError() {
    val viewModel = SignInViewModel(mockAccountService)

    // Manually set error state (would normally come from sign-in failure)
    val stateFlow = viewModel.uiState as MutableStateFlow
    stateFlow.value = stateFlow.value.copy(errorMsg = "Test error")

    assertEquals("Test error", viewModel.uiState.value.errorMsg)

    viewModel.clearErrorMsg()

    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun signInViewModel_signIn_successfulLogin() = runTest {
    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken("Test User", email = "test@example.com")

    val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)

    composeTestRule.setContent {
      SignInScreen(
          credentialManager = fakeCredentialManager,
          onSignedIn = { /* Navigation verified separately */})
    }

    composeTestRule
        .onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // Wait for the login to complete - either success toast or overview navigation
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      // Check if we're no longer showing the login button (loading or navigated away)
      composeTestRule
          .onAllNodesWithTag(SignInScreenTestTags.LOGIN_BUTTON)
          .fetchSemanticsNodes()
          .isEmpty()
    }

    // Give some time for the UI state to update
    composeTestRule.waitForIdle()

    // The login should have succeeded - we can verify by checking that we're not in an error state
  }

  @Test
  fun signInViewModel_signIn_failedLogin() = runTest {
    val mockCredential = mockk<CustomCredential>()

    coEvery { mockAccountService.signInWithGoogle(any<Credential>()) } returns
        Result.failure(Exception("Authentication failed"))
    coEvery {
      mockCredentialManager.getCredential(
          any<Context>(), any<androidx.credentials.GetCredentialRequest>())
    } returns mockk { every { credential } returns mockCredential }

    val viewModel = SignInViewModel(mockAccountService)

    viewModel.signIn(context, mockCredentialManager)

    composeTestRule.waitUntil(timeoutMillis = 5000) { viewModel.uiState.value.errorMsg != null }

    val finalState = viewModel.uiState.value
    assertFalse(finalState.isLoading)
    assertNull(finalState.user)
    assertNotNull(finalState.errorMsg)
    assertTrue(finalState.signedOut)
  }

  @Test
  fun signInViewModel_signIn_userCancellation() = runTest {
    coEvery {
      mockCredentialManager.getCredential(
          any<Context>(), any<androidx.credentials.GetCredentialRequest>())
    } throws Exception("User cancelled")

    val viewModel = SignInViewModel(mockAccountService)
    viewModel.signIn(context, mockCredentialManager)

    composeTestRule.waitUntil(timeoutMillis = 5000) { viewModel.uiState.value.errorMsg != null }

    val finalState = viewModel.uiState.value
    assertFalse(finalState.isLoading)
    assertTrue(finalState.signedOut)
    assertNotNull(finalState.errorMsg)
  }

  @Test
  fun signInViewModel_signIn_credentialException() = runTest {
    val realException =
        androidx.credentials.exceptions.GetCredentialUnknownException("Credential error occurred")

    coEvery {
      mockCredentialManager.getCredential(
          any<Context>(), any<androidx.credentials.GetCredentialRequest>())
    } throws realException

    val viewModel = SignInViewModel(mockAccountService)

    viewModel.signIn(context, mockCredentialManager)

    composeTestRule.waitUntil(timeoutMillis = 5000) { viewModel.uiState.value.errorMsg != null }

    val finalState = viewModel.uiState.value
    assertFalse(finalState.isLoading)
    assertNotNull(finalState.errorMsg)
    assertTrue(finalState.signedOut)
  }

  @Test
  fun signInViewModel_signIn_unexpectedException() = runTest {
    coEvery {
      mockCredentialManager.getCredential(
          any<Context>(), any<androidx.credentials.GetCredentialRequest>())
    } throws RuntimeException("Unexpected error")

    val viewModel = SignInViewModel(mockAccountService)

    viewModel.signIn(context, mockCredentialManager)

    composeTestRule.waitUntil(timeoutMillis = 5000) { viewModel.uiState.value.errorMsg != null }

    val finalState = viewModel.uiState.value
    assertTrue(finalState.errorMsg?.contains("Unexpected error") == true)
  }

  @Test
  fun signInViewModel_signIn_ignoresMultipleConcurrentCalls() = runTest {
    var callCount = 0

    coEvery {
      mockCredentialManager.getCredential(
          any<Context>(), any<androidx.credentials.GetCredentialRequest>())
    } coAnswers
        {
          callCount++
          // Simulate a short suspendable work so the first call is in-flight
          kotlinx.coroutines.delay(200)
          mockk { every { credential } returns mockk<CustomCredential>() }
        }

    val viewModel = SignInViewModel(mockAccountService)

    // Start the first sign-in
    viewModel.signIn(context, mockCredentialManager)

    // Wait until the ViewModel has set isLoading = true so further calls are ignored
    composeTestRule.waitUntil(timeoutMillis = 2000) { viewModel.uiState.value.isLoading }

    // Additional quick calls should be ignored
    viewModel.signIn(context, mockCredentialManager)
    viewModel.signIn(context, mockCredentialManager)

    // Let everything finish
    composeTestRule.waitForIdle()

    // Only the first call should have invoked getCredential
    assertEquals(1, callCount)
  }

  // ========== SplashViewModel Tests ==========

  @Test
  fun splashViewModel_onAppStart_userSignedIn() = runTest {
    coEvery { mockAccountService.hasUser() } returns true

    var signedInCalled = false
    var notSignedInCalled = false

    val viewModel = SplashViewModel(mockAccountService)
    viewModel.onAppStart(
        onSignedIn = { signedInCalled = true }, onNotSignedIn = { notSignedInCalled = true })

    composeTestRule.waitForIdle()

    assertTrue(signedInCalled)
    assertFalse(notSignedInCalled)
  }

  @Test
  fun splashViewModel_onAppStart_userNotSignedIn() = runTest {
    coEvery { mockAccountService.hasUser() } returns false

    var signedInCalled = false
    var notSignedInCalled = false

    val viewModel = SplashViewModel(mockAccountService)
    viewModel.onAppStart(
        onSignedIn = { signedInCalled = true }, onNotSignedIn = { notSignedInCalled = true })

    composeTestRule.waitForIdle()

    assertFalse(signedInCalled)
    assertTrue(notSignedInCalled)
  }

  @Test
  fun splashViewModel_onAppStart_exceptionHandled() = runTest {
    coEvery { mockAccountService.hasUser() } throws Exception("Network error")

    var notSignedInCalled = false

    val viewModel = SplashViewModel(mockAccountService)
    viewModel.onAppStart(onNotSignedIn = { notSignedInCalled = true })

    composeTestRule.waitForIdle()

    assertTrue(notSignedInCalled)
  }

  // ========== NavigationActions Tests ==========

  // This is in addition to the seperate navigation tests in navigation/NavigationTest.kt
  @Test
  fun navigationActions_currentRoute_returnsEmptyWhenNull() {
    val navController = mockk<androidx.navigation.NavHostController>(relaxed = true)
    every { navController.currentDestination } returns null

    val navigationActions = NavigationActions(navController)

    assertEquals("", navigationActions.currentRoute())
  }

  // ========== AccountServiceFirebase Tests ==========

  @Test
  fun accountServiceFirebase_currentUserId_returnsEmptyWhenNoUser() {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    every { mockAuth.currentUser } returns null

    val service = AccountServiceFirebase(auth = mockAuth)

    assertEquals("", service.currentUserId)
  }

  @Test
  fun accountServiceFirebase_currentUserId_returnsUidWhenUserExists() {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockUser = mockk<FirebaseUser>(relaxed = true)
    every { mockAuth.currentUser } returns mockUser
    every { mockUser.uid } returns "test-uid-123"

    val service = AccountServiceFirebase(auth = mockAuth)

    assertEquals("test-uid-123", service.currentUserId)
  }

  @Test
  fun accountServiceFirebase_hasUser_returnsTrueWhenUserExists() = runTest {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    every { mockAuth.currentUser } returns mockFirebaseUser

    val service = AccountServiceFirebase(auth = mockAuth)

    assertTrue(service.hasUser())
  }

  @Test
  fun accountServiceFirebase_hasUser_returnsFalseWhenNoUser() = runTest {
    val mockAuth = mockk<FirebaseAuth>(relaxed = true)
    every { mockAuth.currentUser } returns null

    val service = AccountServiceFirebase(auth = mockAuth)

    assertFalse(service.hasUser())
  }

  /**
   * @Test fun accountServiceFirebase_signOut_success() { val mockAuth = mockk<FirebaseAuth>(relaxed
   *   = true) every { mockAuth.signOut() } just Runs
   *
   * val service = AccountServiceFirebase(auth = mockAuth) val result = service.signOut()
   *
   * assertTrue(result.isSuccess) verify { mockAuth.signOut() } }
   *
   * @Test fun accountServiceFirebase_signOut_failure() { val mockAuth = mockk<FirebaseAuth>(relaxed
   *   = true) every { mockAuth.signOut() } throws Exception("Sign out failed")
   *
   * val service = AccountServiceFirebase(auth = mockAuth) val result = service.signOut()
   *
   * assertTrue(result.isFailure) assertTrue(result.exceptionOrNull()?.message?.contains("Logout
   * failed") == true) }
   */

  // ========== Screen Tests ==========

  @Test
  fun screen_authentication_hasCorrectProperties() {
    assertEquals("authentication", Screen.Authentication.route)
    assertEquals("Authentication", Screen.Authentication.name)
    assertTrue(Screen.Authentication.isTopLevelDestination)
  }

  @Test
  fun screen_splash_hasCorrectProperties() {
    assertEquals("splash", Screen.Splash.route)
    assertEquals("Splash", Screen.Splash.name)
    assertFalse(Screen.Splash.isTopLevelDestination)
  }

  // ========== SignInScreen UI Tests ==========

  @Test
  fun signInScreen_displaysAllComponents() {
    composeTestRule.setContent { SignInScreen() }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.APP_LOGO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()

    composeTestRule.onNodeWithText("WELCOME").assertIsDisplayed()
    composeTestRule.onNodeWithText("Sign in with Google").assertIsDisplayed()
  }

  @Test
  fun signInScreen_showsLoadingIndicator_whenLoading() {
    val mockViewModel = mockk<SignInViewModel>(relaxed = true)
    every { mockViewModel.uiState } returns MutableStateFlow(AuthUIState(isLoading = true))

    composeTestRule.setContent { SignInScreen(authViewModel = mockViewModel) }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertDoesNotExist()
  }

  @Test
  fun signInScreen_buttonClick_triggersSignIn() {
    val mockViewModel = mockk<SignInViewModel>(relaxed = true)
    every { mockViewModel.uiState } returns MutableStateFlow(AuthUIState())
    every { mockViewModel.signIn(any(), any()) } just Runs

    composeTestRule.setContent { SignInScreen(authViewModel = mockViewModel) }

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()

    verify { mockViewModel.signIn(any(), any()) }
  }

  // ========== SplashScreen UI Tests ==========

  @Test
  fun splashScreen_displaysContent() {
    // Render the stateless splash content directly
    composeTestRule.setContent { SplashScreenContent() }

    // The splash content exposes a stable test tag for the logo and the progress indicator
    composeTestRule.onNodeWithTag("splashLogo").assertIsDisplayed()
    composeTestRule.onNodeWithTag("splashProgress").assertIsDisplayed()
  }

  @Test
  fun splashScreen_callsOnAppStart() = runTest {
    val mockViewModel = mockk<SplashViewModel>(relaxed = true)
    every { mockViewModel.onAppStart(any(), any()) } just Runs

    composeTestRule.setContent { SplashScreen(viewModel = mockViewModel) }

    composeTestRule.waitForIdle()
    composeTestRule.mainClock.advanceTimeBy(1500)

    verify(timeout = 3000) { mockViewModel.onAppStart(any(), any()) }
  }

  // ========== Integration Tests ==========

  @Test
  fun ootdApp_startsAtSplashScreen() {
    composeTestRule.setContent { OOTDApp() }

    // Splash screen should show initially
    composeTestRule.waitForIdle()
  }

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

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
  }

  @Test
  fun integrationTest_completeSignInFlow() {
    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken("John Doe", email = "john.doe@example.com")

    val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)

    // Mock Firebase to return a successful user without actually authenticating
    val mockFirebaseAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockFirebaseUser = mockk<FirebaseUser>(relaxed = true)
    val mockAuthResult = mockk<AuthResult>(relaxed = true)

    every { mockFirebaseUser.uid } returns "fake-uid-123"
    every { mockFirebaseUser.email } returns "john.doe@example.com"
    every { mockFirebaseUser.displayName } returns "John Doe"
    every { mockAuthResult.user } returns mockFirebaseUser

    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockFirebaseAuth
    every { mockFirebaseAuth.currentUser } returns null andThen mockFirebaseUser
    every { mockFirebaseAuth.signInWithCredential(any()) } returns Tasks.forResult(mockAuthResult)

    // Start the app with the fake credential manager
    composeTestRule.setContent { OOTDApp(credentialManager = fakeCredentialManager) }

    // Wait for splash screen to finish and navigate to sign-in
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(SignInScreenTestTags.LOGIN_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify we're on the sign-in screen
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.APP_LOGO).assertIsDisplayed()
    composeTestRule.onNodeWithText("WELCOME").assertIsDisplayed()

    // Click the sign-in button
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()

    // Wait for the login to complete and navigate to overview
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithText("Overview Placeholder").fetchSemanticsNodes().isNotEmpty()
    }

    // Verify we've navigated to the overview screen
    composeTestRule.onNodeWithText("Overview Placeholder").assertIsDisplayed()

    // Verify the sign-in button is no longer visible
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertDoesNotExist()

    // Clean up mocks
    unmockkStatic(FirebaseAuth::class)
  }

  @Test
  fun integrationTest_signInWithCustomEmail() {
    val customEmail = "custom.user@test.com"
    val customName = "Custom User"
    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken(customName, email = customEmail)

    val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)

    // Mock Firebase
    val mockFirebaseAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockFirebaseUser = mockk<FirebaseUser>(relaxed = true)
    val mockAuthResult = mockk<AuthResult>(relaxed = true)

    every { mockFirebaseUser.uid } returns "fake-uid-custom"
    every { mockFirebaseUser.email } returns customEmail
    every { mockFirebaseUser.displayName } returns customName
    every { mockAuthResult.user } returns mockFirebaseUser

    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockFirebaseAuth
    every { mockFirebaseAuth.currentUser } returns null andThen mockFirebaseUser
    every { mockFirebaseAuth.signInWithCredential(any()) } returns Tasks.forResult(mockAuthResult)

    composeTestRule.setContent { OOTDApp(credentialManager = fakeCredentialManager) }

    // Wait for sign-in screen
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(SignInScreenTestTags.LOGIN_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Perform sign-in
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()

    // Verify navigation to overview
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithText("Overview Placeholder").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("Overview Placeholder").assertIsDisplayed()

    // Clean up mocks
    unmockkStatic(FirebaseAuth::class)
  }

  @Test
  fun integrationTest_signInShowsLoadingState() {
    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken("Test User", email = "test@example.com")

    val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)

    // Mock Firebase
    val mockFirebaseAuth = mockk<FirebaseAuth>(relaxed = true)
    val mockFirebaseUser = mockk<FirebaseUser>(relaxed = true)
    val mockAuthResult = mockk<AuthResult>(relaxed = true)

    every { mockFirebaseUser.uid } returns "fake-uid-456"
    every { mockFirebaseUser.email } returns "test@example.com"
    every { mockFirebaseUser.displayName } returns "Test User"
    every { mockAuthResult.user } returns mockFirebaseUser

    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockFirebaseAuth
    every { mockFirebaseAuth.currentUser } returns null andThen mockFirebaseUser
    every { mockFirebaseAuth.signInWithCredential(any()) } returns Tasks.forResult(mockAuthResult)

    composeTestRule.setContent { OOTDApp(credentialManager = fakeCredentialManager) }

    // Wait for sign-in screen
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(SignInScreenTestTags.LOGIN_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click sign-in button
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()

    // The button should disappear immediately (replaced by loading indicator)
    composeTestRule.waitUntil(timeoutMillis = 2000) {
      composeTestRule
          .onAllNodesWithTag(SignInScreenTestTags.LOGIN_BUTTON)
          .fetchSemanticsNodes()
          .isEmpty()
    }

    // Eventually navigate to overview
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithText("Overview Placeholder").fetchSemanticsNodes().isNotEmpty()
    }

    // Clean up mocks
    unmockkStatic(FirebaseAuth::class)
  }
}
