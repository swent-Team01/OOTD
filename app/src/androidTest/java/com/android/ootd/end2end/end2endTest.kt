package com.android.ootd.end2end

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.ui.account.AccountPageTestTags
import com.android.ootd.ui.account.UiTestTags
import com.android.ootd.ui.authentication.SignInScreenTestTags
import com.android.ootd.ui.feed.FeedScreenTestTags
import com.android.ootd.ui.feed.FeedScreenTestTags.NAVIGATE_TO_NOTIFICATIONS_SCREEN
import com.android.ootd.ui.navigation.NavigationTestTags
import com.android.ootd.ui.navigation.Tab
import com.android.ootd.ui.post.FitCheckScreenTestTags
import com.android.ootd.utils.BaseEnd2EndTest
import com.android.ootd.utils.enterDateOfBirth
import com.android.ootd.utils.enterUsername
import com.android.ootd.utils.verifyFeedScreenAppears
import com.android.ootd.utils.verifySignInScreenAppears
import kotlinx.coroutines.runBlocking
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
class End2EndTest : BaseEnd2EndTest() {

  /**
   * End-to-end test: Complete user journey from sign-in through registration, navigation,
   * attempting to post an outfit, and logout
   *
   * This test validates the FULL application flow using the actual navigation system:
   * 1. App starts at Splash screen
   * 2. Navigates to Authentication screen (user not signed in)
   * 3. User clicks Google Sign-In button
   * 4. Authentication succeeds with a new user (no username set)
   * 5. App automatically navigates to Registration screen
   * 6. User enters username, date of birth, and location
   * 7. User clicks Save button
   * 8. App navigates directly to Feed screen (onboarding flagged as seen via test setup)
   * 9. User navigates to Search screen via bottom navigation tab
   * 10. User returns to Feed screen via bottom navigation tab
   * 11. User clicks "Do a Fit Check" button to start posting a new outfit
   * 12. User reaches FitCheck screen and verifies "Add Fit Photo" button
   * 13. User navigates back to Feed screen from FitCheck using back button
   * 14. User clicks notification icon in top bar
   * 15. User navigates to Account page via bottom navigation tab and click sign out
   *
   * LIMITATIONS:
   * - Camera/Gallery intents cannot be tested in Compose UI tests without mocking
   * - Taking photos launches external activities that break ComposeTestRule
   * - Search/follow flow requires real Firebase data (steps 10-12 commented out)
   *
   * This test:
   * - Uses the REAL OOTDApp composable with the full NavHost navigation graph
   * - Simulates actual user interactions across multiple screens
   * - Validates automatic navigation flows between screens
   * - Tests the user lifecycle: sign-in → register → navigation → partial outfit post → sign-out
   * - Uses FakeCredentialManager and mocked Firebase to avoid network calls
   * - Mocks consent as already given to skip the consent screen
   * - Uses bottom navigation tabs for Search and Account navigation (no top-bar back arrows)
   */
  @Test
  fun fullAppFlow_newUser_signInAndCompleteRegistration() {
    runBlocking {
      initializeMocksForTest()
      // STEP 1: Launch the full app
      launchFullAppWaitForLogin()

      // STEP 2-3 : Wait for Authentication screen and press Sign-In button
      // (Since FirebaseAuth.currentUser is null, we should navigate to authentication)
      pressLoginButton()

      // STEP 4-5: Wait for RegistrationScreen to appear
      // (userExists returns false, so the app should navigate to registration)
      waitNavigationRegisterScreen()

      // Step 6: Fill in registration form
      enterUsername(composeTestRule, testUsername)
      enterDateOfBirth(composeTestRule, testDateofBirth)
      enterLocation(testLocation)

      // Steps 7-8: Save registration and navigate to feed
      saveRegistrationAndNavigateToFeed()

      // STEP 9: Navigate to Search screen using bottom navigation tab
      navigateToSearchScreen()

      // STEP 10: Navigate back to Feed screen using bottom navigation tab (instead of back button)
      composeTestRule.waitUntil(timeoutMillis = 5000) {
        composeTestRule
            .onAllNodesWithTag(NavigationTestTags.getTabTestTag(Tab.Feed))
            .fetchSemanticsNodes()
            .isNotEmpty()
      }
      composeTestRule
          .onNodeWithTag(NavigationTestTags.getTabTestTag(Tab.Feed))
          .assertIsDisplayed()
          .performClick()
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

      // STEP 11: Click "Do a Fit Check" button to start posting a new outfit
      composeTestRule.waitUntil(timeoutMillis = 5000) {
        composeTestRule
            .onAllNodesWithTag(FeedScreenTestTags.ADD_POST_FAB)
            .fetchSemanticsNodes()
            .isNotEmpty()
      }

      composeTestRule.onNodeWithTag(FeedScreenTestTags.ADD_POST_FAB).assertIsDisplayed()
      composeTestRule.onNodeWithTag(FeedScreenTestTags.ADD_POST_FAB).performClick()
      composeTestRule.waitForIdle()

      // Wait for FitCheck screen to appear
      composeTestRule.waitUntil(timeoutMillis = 5000) {
        composeTestRule
            .onAllNodesWithTag(FitCheckScreenTestTags.SCREEN)
            .fetchSemanticsNodes()
            .isNotEmpty()
      }

      // Verify we're on the FitCheck screen
      composeTestRule.onNodeWithTag(FitCheckScreenTestTags.SCREEN).assertIsDisplayed()
      composeTestRule
          .onNodeWithTag(FitCheckScreenTestTags.ADD_PHOTO_BUTTON)
          .performScrollTo()
          .assertIsDisplayed()

      // STEP 12: Navigate back to Feed screen from FitCheck
      composeTestRule.onNodeWithTag(FitCheckScreenTestTags.BACK_BUTTON).performClick()
      composeTestRule.waitForIdle()

      // Wait for Feed screen to appear again
      composeTestRule.waitUntil(timeoutMillis = 5000) {
        composeTestRule
            .onAllNodesWithTag(FeedScreenTestTags.SCREEN)
            .fetchSemanticsNodes()
            .isNotEmpty()
      }

      // Verify we're back on the Feed screen
      verifyFeedScreenAppears(composeTestRule)

      // STEP 13: User clicks notification Icon
      // Wait for the Notification Icon to be fully initialized and visible
      composeTestRule.waitUntil(timeoutMillis = 5000) {
        composeTestRule
            .onAllNodesWithTag(NAVIGATE_TO_NOTIFICATIONS_SCREEN, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .isNotEmpty()
      }

      // Click on the notifications icon
      composeTestRule
          .onNodeWithTag(NAVIGATE_TO_NOTIFICATIONS_SCREEN, useUnmergedTree = true)
          .performClick()

      composeTestRule.waitForIdle()

      // STEP 14: Go to account page

      composeTestRule
          .onNodeWithTag(NavigationTestTags.getTabTestTag(Tab.Account))
          .assertIsDisplayed()
          .performClick()

      composeTestRule.waitForIdle()

      // Wait for the account page to finish loading (there's a loading overlay shown first)
      composeTestRule.waitUntil(timeoutMillis = 10000) {
        composeTestRule
            .onAllNodesWithTag(AccountPageTestTags.SETTINGS_BUTTON)
            .fetchSemanticsNodes()
            .isNotEmpty()
      }

      composeTestRule
          .onNodeWithTag(AccountPageTestTags.SETTINGS_BUTTON)
          .assertIsDisplayed()
          .performClick()

      // Scroll to Sign Out button
      composeTestRule
          .onNodeWithTag(UiTestTags.TAG_SIGNOUT_BUTTON)
          .performScrollTo()
          .assertIsDisplayed()

      composeTestRule.waitForIdle()

      // STEP 15: User clicks signout Button
      composeTestRule.onNodeWithTag(UiTestTags.TAG_SIGNOUT_BUTTON).performScrollTo().performClick()

      composeTestRule.waitForIdle()

      // Wait for navigation back to Authentication screen after logout
      composeTestRule.waitUntil(timeoutMillis = 10000) {
        composeTestRule
            .onAllNodesWithTag(SignInScreenTestTags.LOGIN_BUTTON)
            .fetchSemanticsNodes()
            .isNotEmpty()
      }

      // Verify we're back on the Sign-In screen after logout
      verifySignInScreenAppears(composeTestRule)
    }
  }
}
