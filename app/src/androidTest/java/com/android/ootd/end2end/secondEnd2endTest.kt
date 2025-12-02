package com.android.ootd.end2end

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.screen.enterUsername
import com.android.ootd.utils.BaseEnd2EndTest
import com.android.ootd.utils.addPostWithOneItem
import com.android.ootd.utils.enterDateOfBirth
import com.android.ootd.utils.signOutAndVerifyAuthScreen
import com.android.ootd.utils.verifyRegisterScreenAppears
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
class SecondEnd2EndTest : BaseEnd2EndTest() {

  /**
   * End-to-end test: Complete user journey from sign-in through registration, search, follow,
   * attempting to post an outfit, adding an item to the inventory and then logging out.
   *
   * This test validates the FULL application flow using the actual navigation system:
   * 1. App starts at Splash screen
   * 2. Navigates to Authentication screen
   * 3. User clicks Google Sign-In button
   * 4. Authentication succeeds with a new user
   * 5. App automatically navigates to Registration screen
   * 6. User enters username, date of birth and location
   * 7. User clicks Save button
   * 8. App navigates directly to Feed screen (consent already given via mock)
   * 9. User adds post with one item attached to it and check it is displayed in feed
   * 10. User searches for "greg" in the username field
   * 11. User selects greg from the suggestions
   * 12. User clicks Follow button on greg's profile
   * 13. User clicks notification icon and goes to notification screen.
   * 14. User accepts follow notification
   * 15. User goes to inventory checks no items are there
   * 16. User checks the add item button is there
   * 17. User clicks Sign Out button
   * 18. App navigates back to Authentication screen
   *
   * LIMITATIONS:
   * - Taking photos launches external activities that break ComposeTestRule
   *
   * This test:
   * - Uses the REAL OOTDApp composable with the full NavHost navigation graph
   * - Simulates actual user interactions across multiple screens
   * - Validates automatic navigation flows between screens
   * - Tests the user lifecycle: sign-in → register → outfit post → search → accept notification →
   *   sign-out
   * - Uses FakeCredentialManager and mocked Firebase to avoid network calls
   * - Mocks consent as already given to skip the consent screen
   */
  @Test
  fun fullAppFlow_newUser_2() {
    runBlocking {
      initializeMocksForTest()

      // Steps 1-2: Launch app and wait for login screen
      launchFullAppWaitForLogin()

      // Step 3: Click login button
      pressLoginButton()

      // Step 4: Wait for navigation to register screen
      waitNavigationRegisterScreen()

      // Step 5: Verify we're on the register screen
      verifyRegisterScreenAppears(composeTestRule)

      // Step 6: Fill in registration form
      composeTestRule.enterUsername(testUsername)
      enterDateOfBirth(composeTestRule, testDateofBirth)
      enterLocation(testLocation)

      // Steps 7-8: Save registration and navigate to feed
      saveRegistrationAndNavigateToFeed()

      // Step 9: Add post with one item attached to it and check it is displayed in feed
      addPostWithOneItem(composeTestRule)

      // Step 10: Navigate to search screen
      navigateToSearchScreen()

      // Step 11-12: Searches for Greg and follows him
      searchAndFollowUser()

      // Step 13: Open notifications screen
      openNotificationsScreen()

      // Step 14: Accept follow notification
      acceptFollowNotification()

      // Step 15-16: Navigate to inventory and check add item button exists
      navigateToInventoryAndCheckAddItemButton()

      // Step 17-18: Sign out and verify auth screen
      signOutAndVerifyAuthScreen(
          composeTestRule = composeTestRule, testNavController = testNavController)
    }
  }
}
