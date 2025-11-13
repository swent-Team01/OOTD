package com.android.ootd.end2end

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.OOTDApp
import com.android.ootd.model.map.Location
import com.android.ootd.screen.enterDate
import com.android.ootd.screen.enterUsername
import com.android.ootd.ui.account.AccountPageTestTags
import com.android.ootd.ui.account.UiTestTags
import com.android.ootd.ui.authentication.SignInScreenTestTags
import com.android.ootd.ui.feed.FeedScreenTestTags
import com.android.ootd.ui.feed.FeedScreenTestTags.NAVIGATE_TO_NOTIFICATIONS_SCREEN
import com.android.ootd.ui.navigation.NavigationTestTags
import com.android.ootd.ui.navigation.Tab
import com.android.ootd.ui.post.FitCheckScreenTestTags
import com.android.ootd.ui.register.RegisterScreenTestTags
import com.android.ootd.ui.search.SearchScreenTestTags
import com.android.ootd.utils.BaseEnd2EndTest
import com.android.ootd.utils.verifyFeedScreenAppears
import com.android.ootd.utils.verifySignInScreenAppears
import io.mockk.*
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
   * 8. App navigates directly to Feed screen (consent already given via mock)
   * 9. User navigates to Search screen via bottom navigation tab 10-12. (Search/follow steps
   *    skipped - require real Firebase data)
   * 13. User returns to Feed screen via bottom navigation tab
   * 14. User clicks "Do a Fit Check" button to start posting a new outfit
   * 15. User reaches FitCheck screen and verifies "Add Fit Photo" button
   * 16. Note: Photo selection with camera/gallery cannot be tested in automated UI tests because it
   *     launches external Android activities that break the Compose hierarchy
   * 17. User navigates back to Feed screen from FitCheck using back button
   * 18. User clicks notification icon in top bar
   * 19. User navigates to Account page via bottom navigation tab
   * 20. User clicks Sign Out button
   * 21. App navigates back to Authentication screen
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
      composeTestRule
          .onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON)
          .performScrollTo()
          .assertIsDisplayed()
      composeTestRule.onNodeWithTag(SignInScreenTestTags.APP_LOGO).assertIsDisplayed()
      composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_TITLE).assertIsDisplayed()

      // STEP 3: Click the Google Sign-In button
      // Update mock to return the signed-in user after sign-in
      every { mockFirebaseAuth.currentUser } returns mockFirebaseUser

      composeTestRule
          .onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON)
          .performScrollTo()
          .performClick()

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
      composeTestRule
          .onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME)
          .performScrollTo()
          .assertIsDisplayed()
      composeTestRule
          .onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_DATE)
          .performScrollTo()
          .assertIsDisplayed()
      composeTestRule
          .onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE)
          .performScrollTo()
          .assertIsDisplayed()

      composeTestRule.waitForIdle()

      // STEP 6: Fill in the registration form - enter username FIRST
      // Use unique username for each test run to avoid "username already exists" errors
      composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).performScrollTo()
      composeTestRule.enterUsername(testUsername)
      composeTestRule.waitForIdle()
      // Verify username was entered correctly before moving on
      composeTestRule
          .onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME)
          .performScrollTo()
          .assertTextContains(testUsername)

      // STEP 6: Fill in the registration form - enter date of birth
      composeTestRule
          .onNodeWithTag(RegisterScreenTestTags.DATE_PICKER_ICON, useUnmergedTree = true)
          .performScrollTo()
          .performClick()

      composeTestRule.waitForIdle()

      // Verify date picker is displayed
      composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_DATE_PICKER).assertIsDisplayed()

      // Enter date and confirm
      composeTestRule.enterDate("10102020")
      composeTestRule.waitForIdle()

      // STEP 6b: Select a location
      // Mock the location repository to return suggestions when user types "Zurich"
      val testLocation = Location(47.3769, 8.5417, "Zürich, Switzerland")
      coEvery { mockLocationRepository.search(any()) } returns listOf(testLocation)

      // Enter location text in the input field
      composeTestRule
          .onNodeWithTag(com.android.ootd.ui.map.LocationSelectionTestTags.INPUT_LOCATION)
          .performScrollTo()
          .performClick()
      composeTestRule.waitForIdle()

      // Type "Zurich" to trigger location search
      composeTestRule
          .onNodeWithTag(com.android.ootd.ui.map.LocationSelectionTestTags.INPUT_LOCATION)
          .performTextInput("Zurich")
      composeTestRule.waitForIdle()

      // Wait for location suggestions to appear (now from mocked repository)
      composeTestRule.waitUntil(timeoutMillis = 5000) {
        composeTestRule
            .onAllNodesWithTag(
                com.android.ootd.ui.map.LocationSelectionTestTags.LOCATION_SUGGESTION)
            .fetchSemanticsNodes()
            .isNotEmpty()
      }

      // Click the first location suggestion to select it
      composeTestRule
          .onAllNodesWithTag(com.android.ootd.ui.map.LocationSelectionTestTags.LOCATION_SUGGESTION)[
              0]
          .performClick()
      composeTestRule.waitForIdle()

      // STEP 7: Save the registration
      // Update mock behavior BEFORE clicking Save to avoid race conditions
      // After successful registration, userExists should return true
      coEvery { mockUserRepository.userExists(any()) } returns true
      coEvery { mockUserRepository.createUser(any(), any()) } returns Unit

      composeTestRule.waitForIdle()

      // Ensure the Save button is visible by scrolling to it if necessary
      composeTestRule
          .onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE)
          .performScrollTo()
          .assertIsEnabled()
      composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE).performClick()

      // STEP 8: App automatically navigates to Feed screen (consent is mocked as already given)
      composeTestRule.waitForIdle()

      // More robust waiting with better error handling
      composeTestRule.waitUntil(timeoutMillis = 10000) {
        try {
          composeTestRule
              .onAllNodesWithTag(FeedScreenTestTags.SCREEN)
              .fetchSemanticsNodes()
              .isNotEmpty()
        } catch (_: Exception) {
          // Return false to continue waiting if there's an exception
          false
        }
      }

      verifyFeedScreenAppears(composeTestRule)

      composeTestRule.waitForIdle()

      // STEP 9: Navigate to Search screen using bottom navigation tab
      composeTestRule.waitUntil(timeoutMillis = 5000) {
        composeTestRule
            .onAllNodesWithTag(NavigationTestTags.getTabTestTag(Tab.Search))
            .fetchSemanticsNodes()
            .isNotEmpty()
      }
      composeTestRule
          .onNodeWithTag(NavigationTestTags.getTabTestTag(Tab.Search))
          .assertIsDisplayed()
          .performClick()
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

      // This step doesn't work as we are on a local FireBase
      //    // STEP 10: Search for "Greg" in the username field
      //    composeTestRule.onNodeWithTag(UserSelectionFieldTestTags.INPUT_USERNAME).performClick()
      //    composeTestRule.waitForIdle()
      //    composeTestRule
      //        .onNodeWithTag(UserSelectionFieldTestTags.INPUT_USERNAME)
      //        .performTextInput("Greg")
      //    composeTestRule.waitForIdle()
      //
      //    // STEP 11: Wait for and click on the username suggestion
      //    composeTestRule.waitUntil(timeoutMillis = 5000) {
      //      composeTestRule
      //          .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)
      //          .fetchSemanticsNodes()
      //          .isNotEmpty()
      //    }
      //
      //    // Click on the first suggestion (Greg)
      //    composeTestRule
      //        .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)[0]
      //        .performClick()
      //
      //    composeTestRule.waitForIdle()
      //
      //    // STEP 12: Wait for the profile card to appear and click Follow button
      //    composeTestRule.waitUntil(timeoutMillis = 5000) {
      //      composeTestRule
      //          .onAllNodesWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON)
      //          .fetchSemanticsNodes()
      //          .isNotEmpty()
      //    }
      //
      //
      // composeTestRule.onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON).assertIsDisplayed()
      //    composeTestRule.onNodeWithTag(UserProfileCardTestTags.USER_FOLLOW_BUTTON).performClick()
      //    composeTestRule.waitForIdle()

      // STEP 13: Navigate back to Feed screen using bottom navigation tab (instead of back button)
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

      // STEP 14: Click "Do a Fit Check" button to start posting a new outfit
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

      // STEP 15: Verify the "Add Fit Photo" button is available
      // Note: We cannot actually take a photo with the camera in an automated test because:
      // - Clicking "Take Photo" launches the native Android camera app as a separate activity
      // - This causes the Compose hierarchy to be lost (app goes to background)
      // - ComposeTestRule can only interact with Compose UI in the foreground
      //
      //
      // STEP 16: Skip photo selection for testing purposes
      // In a real-world scenario, the user would select a photo here
      // For automated testing, we'll proceed without it
      // (Need to ask the coaches what can we do)

      // Since the Next button requires a valid photo, we cannot proceed further

      // STEP 17: Navigate back to Feed screen from FitCheck
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

      // STEP 18: User clicks notification Icon
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

      // STEP 19: Go to account page

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

      // STEP 20: User clicks signout Button
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
