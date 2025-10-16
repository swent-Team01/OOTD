package com.android.ootd.end2end

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.OOTDApp
import com.android.ootd.model.user.UserRepositoryFirestore
import com.android.ootd.screen.enterDate
import com.android.ootd.screen.enterUsername
import com.android.ootd.ui.account.UiTestTags
import com.android.ootd.ui.authentication.SignInScreenTestTags
import com.android.ootd.ui.feed.FeedScreenTestTags
import com.android.ootd.ui.register.RegisterScreenTestTags
import com.android.ootd.ui.search.SearchScreenTestTags
import com.android.ootd.ui.search.UserProfileCardTestTags
import com.android.ootd.ui.search.UserSelectionFieldTestTags
import com.android.ootd.utils.FakeCredentialManager
import com.android.ootd.utils.FakeJwtGenerator
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end test suite for the OOTD app.
 *
 * This test suite validates complete user flows using the full navigation system from MainActivity.
 * These tests simulate real user interactions by:
 * - Using the actual OOTDApp composable with its NavHost
 * - Following the real navigation graph and flow
 * - Testing multi-screen workflows as a user would experience them
 *
 * Some code was made using an AI coding tool
 */
@RunWith(AndroidJUnit4::class)
class End2EndTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var mockFirebaseAuth: FirebaseAuth
  private lateinit var mockFirebaseUser: FirebaseUser
  private lateinit var mockAuthResult: AuthResult
  private lateinit var mockUserRepository: UserRepositoryFirestore
  private lateinit var testUserId: String
  private lateinit var testUsername: String

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    mockFirebaseAuth = mockk(relaxed = true)
    mockFirebaseUser = mockk(relaxed = true)
    mockAuthResult = mockk(relaxed = true)
    mockUserRepository = mockk(relaxed = true)

    // Generate unique identifiers for each test run to avoid conflicts
    val timestamp = System.currentTimeMillis()
    testUserId = "test_user_$timestamp"
    testUsername = "user$timestamp"

    // Ensure clean state
    FirebaseAuth.getInstance().signOut()
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  /**
   * End-to-end test: Complete user journey from sign-in through registration, search, follow and
   * logout
   *
   * This test validates the FULL application flow using the actual navigation system:
   * 1. App starts at Splash screen
   * 2. Navigates to Authentication screen (user not signed in)
   * 3. User clicks Google Sign-In button
   * 4. Authentication succeeds with a new user (no username set)
   * 5. App automatically navigates to Registration screen
   * 6. User enters username and date of birth
   * 7. User clicks Save button
   * 8. App navigates to Feed screen (main app)
   * 9. User clicks search icon to navigate to Search screen
   * 10. User searches for "Greg" in the username field
   * 11. User selects Greg from the suggestions
   * 12. User clicks Follow button on Greg's profile
   * 13. User clicks back button to return to Feed screen
   * 14. User clicks profile icon to navigate to Account screen
   * 15. User clicks Sign Out button
   * 16. App navigates back to Authentication screen
   *
   * This test:
   * - Uses the REAL OOTDApp composable with the full NavHost navigation graph
   * - Simulates actual user interactions across multiple screens
   * - Validates automatic navigation flows between screens
   * - Tests the complete user lifecycle: sign-in → register → search → follow → sign-out
   * - Uses FakeCredentialManager and mocked Firebase to avoid network calls
   */
  @Test
  fun fullAppFlow_newUser_signInAndCompleteRegistration() = runTest {
    // Create a fake Google ID token for a new user
    val fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken("greg", email = "greg@gmail.com")
    val fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)

    // Set up mock Firebase authentication with new unique test user ID
    every { mockFirebaseUser.uid } returns testUserId
    every { mockAuthResult.user } returns mockFirebaseUser

    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockFirebaseAuth
    every { mockFirebaseAuth.currentUser } returns null // Initially not signed in
    every { mockFirebaseAuth.signInWithCredential(any()) } returns Tasks.forResult(mockAuthResult)

    // New user, so userExists returns false initially (no username set yet)
    // After registration, it will return true
    coEvery { mockUserRepository.userExists(any()) } returns false

    // Mock successful user creation - use any() matchers since we're testing the flow, not exact
    // values
    coEvery { mockUserRepository.createUser(any(), any()) } returns Unit

    // STEP 1: Launch the full app
    composeTestRule.setContent {
      OOTDApp(context = context, credentialManager = fakeCredentialManager)
    }

    composeTestRule.waitForIdle()

    // STEP 2: Wait for navigation from Splash to Authentication screen
    // (Since FirebaseAuth.currentUser is null, we should navigate to authentication)
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(SignInScreenTestTags.LOGIN_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify we're on the Sign-In screen
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.APP_LOGO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_TITLE).assertIsDisplayed()

    // STEP 3: Click the Google Sign-In button
    // Update mock to return the signed-in user after sign-in
    every { mockFirebaseAuth.currentUser } returns mockFirebaseUser

    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // STEP 4: Wait for automatic navigation to Registration screen
    // (userExists returns false, so the app should navigate to registration)
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(RegisterScreenTestTags.REGISTER_SAVE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // STEP 5: Verify we're on the Registration screen
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.APP_LOGO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.WELCOME_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_DATE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE).assertIsDisplayed()

    composeTestRule.waitForIdle()

    // STEP 6: Fill in the registration form - enter username FIRST
    // Use unique username for each test run to avoid "username already exists" errors
    composeTestRule.enterUsername(testUsername)
    composeTestRule.waitForIdle()
    // Verify username was entered correctly before moving on
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME)
        .assertTextContains(testUsername)

    // STEP 6: Fill in the registration form - enter date of birth
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.DATE_PICKER_ICON, useUnmergedTree = true)
        .performClick()

    composeTestRule.waitForIdle()

    // Verify date picker is displayed
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_DATE_PICKER).assertIsDisplayed()

    // Enter date and confirm
    composeTestRule.enterDate("10102020")
    composeTestRule.waitForIdle()

    // STEP 7: Save the registration
    // After successful registration, userExists should return true
    coEvery { mockUserRepository.userExists(any()) } returns true

    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE).assertIsEnabled()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE).performClick()

    // STEP 8: App automatically switches to feed screen
    // Wait for navigation to Feed screen after successful registration
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(FeedScreenTestTags.SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify we're on the Feed screen
    composeTestRule.onNodeWithTag(FeedScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(FeedScreenTestTags.TOP_BAR).assertIsDisplayed()

    composeTestRule.waitForIdle()

    // STEP 9: Click on search icon to navigate to search screen
    composeTestRule.onNodeWithTag(FeedScreenTestTags.NAVIGATE_TO_SEARCH_SCREEN).performClick()
    composeTestRule.waitForIdle()

    // Wait for Search screen to appear
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(SearchScreenTestTags.SEARCH_SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify we're on the Search screen
    composeTestRule.onNodeWithTag(SearchScreenTestTags.SEARCH_SCREEN).assertIsDisplayed()

    // STEP 10: Search for "Greg" in the username field
    composeTestRule.onNodeWithTag(UserSelectionFieldTestTags.INPUT_USERNAME).performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(UserSelectionFieldTestTags.INPUT_USERNAME)
        .performTextInput("Greg")
    composeTestRule.waitForIdle()

    // STEP 11: Wait for and click on the username suggestion
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click on the first suggestion (Greg)
    composeTestRule
        .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)[0]
        .performClick()

    composeTestRule.waitForIdle()

    // STEP 12: Wait for the profile card to appear and click Follow button
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // STEP 13: Click back button to return to Feed screen
    composeTestRule.onNodeWithTag(SearchScreenTestTags.GO_BACK_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Wait for Feed screen to appear again
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(FeedScreenTestTags.SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify we're back on the Feed screen
    composeTestRule.onNodeWithTag(FeedScreenTestTags.SCREEN).assertIsDisplayed()

    // STEP 14: User clicks profile Icon to navigate to Account screen
    // Wait for the AccountIcon to be fully initialized and visible
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click on the account icon
    composeTestRule
        .onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER, useUnmergedTree = true)
        .performClick()

    composeTestRule.waitForIdle()

    // Wait for Account screen to appear
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(UiTestTags.TAG_ACCOUNT_TITLE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify we're on the Account screen
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(UiTestTags.TAG_SIGNOUT_BUTTON).assertIsDisplayed()

    composeTestRule.waitForIdle()

    // STEP 15: User clicks signout Button
    composeTestRule.onNodeWithTag(UiTestTags.TAG_SIGNOUT_BUTTON).performClick()

    composeTestRule.waitForIdle()

    // Wait for navigation back to Authentication screen after logout
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(SignInScreenTestTags.LOGIN_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Verify we're back on the Sign-In screen after logout
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.APP_LOGO).assertIsDisplayed()

    unmockkStatic(FirebaseAuth::class)
  }
}
