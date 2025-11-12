package com.android.ootd.end2end

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.OOTDApp
import com.android.ootd.model.map.Location
import com.android.ootd.model.notifications.Notification
import com.android.ootd.model.user.User
import com.android.ootd.screen.enterDate
import com.android.ootd.screen.enterUsername
import com.android.ootd.ui.Inventory.InventoryScreenTestTags
import com.android.ootd.ui.account.UiTestTags
import com.android.ootd.ui.authentication.SignInScreenTestTags
import com.android.ootd.ui.feed.FeedScreenTestTags
import com.android.ootd.ui.navigation.NavigationTestTags
import com.android.ootd.ui.navigation.Tab
import com.android.ootd.ui.notifications.NotificationsScreenTestTags
import com.android.ootd.ui.post.FitCheckScreenTestTags
import com.android.ootd.ui.register.RegisterScreenTestTags
import com.android.ootd.ui.search.SearchScreenTestTags
import com.android.ootd.ui.search.UserProfileCardTestTags
import com.android.ootd.ui.search.UserSelectionFieldTestTags
import com.android.ootd.utils.BaseEnd2EndTest
import com.android.ootd.utils.verifyFeedScreenAppears
import com.android.ootd.utils.verifyInventoryScreenAppears
import com.android.ootd.utils.verifyRegisterScreenAppears
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
class SecondEnd2EndTest : BaseEnd2EndTest() {

  /**
   * Launches the full OOTD application and waits for the login screen to appear.
   *
   * This function performs the following steps:
   * 1. Sets up the OOTDApp composable with the fake credential manager
   * 2. Waits for UI to stabilize
   * 3. Waits for navigation from Splash screen to Authentication screen
   * 4. Verifies that the Sign-In screen is displayed
   *
   * Note: Since FirebaseAuth.currentUser is null at this point, the app should automatically
   * navigate from Splash to the Authentication screen.
   */
  fun launchFullAppWaitForLogin() {
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
    verifySignInScreenAppears(composeTestRule)
  }

  /**
   * Simulates pressing the Google Sign-In button on the Authentication screen.
   *
   * This function:
   * 1. Updates the mock Firebase authentication to return a signed-in user
   * 2. Scrolls to and clicks the login button
   * 3. Waits for the UI to process the login action
   *
   * After this action, the app should automatically navigate to the Registration screen if the user
   * is new (no username set).
   */
  fun pressLoginButton() {
    // STEP 3: Click the Google Sign-In button
    // Update mock to return the signed-in user after sign-in
    every { mockFirebaseAuth.currentUser } returns mockFirebaseUser

    composeTestRule
        .onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON)
        .performScrollTo()
        .performClick()

    composeTestRule.waitForIdle()
  }

  /**
   * Waits for automatic navigation to the Registration screen after successful sign-in.
   *
   * This function waits until the Register Save button is visible on screen, indicating that the
   * app has successfully navigated to the Registration screen.
   *
   * This navigation occurs automatically when:
   * - User successfully signs in with Google
   * - The user account doesn't exist in the database (userExists returns false)
   */
  fun waitNavigationRegisterScreen() {
    // STEP 4: Wait for automatic navigation to Registration screen
    // (userExists returns false, so the app should navigate to registration)
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(RegisterScreenTestTags.REGISTER_SAVE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  /**
   * Enters a username into the registration form.
   *
   * This function:
   * 1. Scrolls to the username input field
   * 2. Enters the provided username using the helper function
   * 3. Waits for UI to stabilize
   * 4. Verifies that the username was entered correctly
   *
   * @param testUsername The username to be entered in the registration form
   */
  fun enterUsername(testUsername: String) {
    // STEP 6: Fill in the registration form - enter username FIRST
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).performScrollTo()
    composeTestRule.enterUsername(testUsername)
    composeTestRule.waitForIdle()
    // Verify username was entered correctly before moving on
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME)
        .performScrollTo()
        .assertTextContains(testUsername)
  }

  /**
   * Enters a date of birth using the date picker in the registration form.
   *
   * This function:
   * 1. Scrolls to and clicks the date picker icon
   * 2. Waits for the date picker dialog to appear
   * 3. Verifies the date picker is displayed
   * 4. Enters the date using the helper function
   * 5. Waits for the date picker to close
   *
   * @param testDateofBirth The date of birth to be entered (format: "DD/MM/YYYY")
   */
  fun enterDateOfBirth(testDateofBirth: String) {
    // STEP 6: Fill in the registration form - enter date of birth

    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.DATE_PICKER_ICON, useUnmergedTree = true)
        .performScrollTo()
        .performClick()

    composeTestRule.waitForIdle()

    // Verify date picker is displayed
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_DATE_PICKER).assertIsDisplayed()

    // Enter date and confirm
    composeTestRule.enterDate(testDateofBirth)
    composeTestRule.waitForIdle()
  }

  /**
   * Selects a location from the location picker in the registration form.
   *
   * This function:
   * 1. Mocks the location repository to return the provided location when searching
   * 2. Scrolls to and clicks the location input field
   * 3. Types "Zurich" to trigger a location search
   * 4. Waits for location suggestions to appear
   * 5. Clicks the first suggestion to select it
   *
   * @param testLocation The location object to be used as the search result
   */
  fun enterLocation(testLocation: Location) {
    // STEP 6b: Select a location
    // Mock the location repository to return suggestions when user types "Zurich"

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
          .onAllNodesWithTag(com.android.ootd.ui.map.LocationSelectionTestTags.LOCATION_SUGGESTION)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click the first location suggestion to select it
    composeTestRule
        .onAllNodesWithTag(com.android.ootd.ui.map.LocationSelectionTestTags.LOCATION_SUGGESTION)[0]
        .performClick()
    composeTestRule.waitForIdle()
  }

  /**
   * Saves the registration form and waits for navigation to the Feed screen.
   *
   * This function:
   * 1. Updates mock behavior to indicate the user now exists in the database
   * 2. Mocks the user creation operation to succeed
   * 3. Scrolls to and verifies the Save button is enabled
   * 4. Clicks the Save button
   * 5. Waits for automatic navigation to the Feed screen
   * 6. Verifies the Feed screen is displayed
   *
   * Note: Navigation to Feed assumes consent is already given (mocked in test setup). If consent is
   * not given, the app would navigate to the Consent screen instead.
   */
  fun saveRegistrationAndNavigateToFeed() {
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
  }

  /**
   * Navigates from the Feed screen to the Search screen.
   *
   * This function:
   * 1. Clicks the search icon on the Feed screen
   * 2. Waits for the Search screen to appear
   * 3. Verifies the Search screen is displayed
   */
  fun navigateToSearchScreen() {
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
  }

  /**
   * Searches for the user Greg and follows him
   *
   * This function:
   * 1. Inputs the name greg in the search screen
   * 2. Selects the user greg from the dropdown
   * 3. Follows greg by clicking the follow button
   */
  fun searchAndFollowUser() {
    // Steps 10-12: Search and follow user
    // This step doesn't work as we are on a local FireBase
    coEvery { mockUserRepository.getAllUsers() } returns
        runBlocking { listOf(User(username = userId)) }

    // STEP 10: Search for "Greg" in the username field
    composeTestRule.onNodeWithTag(UserSelectionFieldTestTags.INPUT_USERNAME).performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(UserSelectionFieldTestTags.INPUT_USERNAME)
        .performTextInput(userId)
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
  }

  /**
   * Navigates back from the Search screen to the Feed screen.
   *
   * This function:
   * 1. Clicks the back button on the Search screen
   * 2. Waits for the Feed screen to appear
   * 3. Verifies the Feed screen is displayed
   */
  fun navigateBackToFeedFromSearch() {
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
  }

  /**
   * Navigates from the Feed screen to the FitCheck screen to start creating a new outfit post.
   *
   * This function:
   * 1. Waits for the "Add Post" floating action button to appear
   * 2. Verifies the button is displayed
   * 3. Clicks the button to start posting a new outfit
   * 4. Waits for the FitCheck screen to appear
   * 5. Verifies the FitCheck screen and "Add Photo" button are displayed
   *
   * Note: The photo selection functionality cannot be fully tested in automated UI tests because it
   * launches external Android activities (camera/gallery) that break the Compose hierarchy.
   */
  fun navigateToFitCheckScreen() {
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
  }

  /**
   * Navigates back from the FitCheck screen to the Feed screen.
   *
   * This function:
   * 1. Clicks the back button on the FitCheck screen
   * 2. Waits for the Feed screen to appear
   * 3. Verifies the Feed screen is displayed
   *
   * This is used when the user decides not to complete the outfit post.
   */
  fun navigateBackToFeedFromFitCheck() {
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
  }

  /**
   * Opens the notifications screen from the Feed screen.
   *
   * This function:
   * 1. Waits for the notification icon to be initialized and visible
   * 2. Clicks the notification icon to navigate to the notifications screen
   * 3. Waits for the UI to stabilize
   * 4. Tests to see that there is a follow notification for the user on the screen
   */
  fun openNotificationsScreen() {
    // STEP 18: User clicks notification Icon
    // Wait for the Notification Icon to be fully initialized and visible
    coEvery { mockNotificationRepository.getNotificationsForReceiver(any()) } returns
        runBlocking {
          listOf(
              Notification(
                  uid = "notification1",
                  senderId = userId,
                  receiverId = userId,
                  type = "FOLLOW_REQUEST",
                  content = "Wants to follow you"))
        }

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(
              FeedScreenTestTags.NAVIGATE_TO_NOTIFICATIONS_SCREEN, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Click on the notifications icon
    composeTestRule
        .onNodeWithTag(FeedScreenTestTags.NAVIGATE_TO_NOTIFICATIONS_SCREEN, useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onNodeWithTag(NotificationsScreenTestTags.NOTIFICATIONS_SCREEN).isDisplayed()
    }
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(NotificationsScreenTestTags.NOTIFICATION_ITEM)
          .fetchSemanticsNodes()
          .size == 1
    }
  }

  /** Accepts follow notification from the notification screen and checks it dissapeared after. */
  fun acceptFollowNotification() {
    composeTestRule.onAllNodesWithTag(NotificationsScreenTestTags.ACCEPT_BUTTON)[0].performClick()

    // STEP 19: Accept follow notification
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(NotificationsScreenTestTags.NOTIFICATION_ITEM)
          .fetchSemanticsNodes()
          .isEmpty()
    }
  }

  /**
   * Goes to the inventory by clicking the corresponding tab button and checks the items button is
   * there *
   */
  fun navigateToInventoryAndCheckAddItemButton() {
    composeTestRule.onNodeWithTag(NavigationTestTags.INVENTORY_TAB).performClick()

    verifyInventoryScreenAppears(composeTestRule)

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onNodeWithTag(InventoryScreenTestTags.EMPTY_STATE).isDisplayed()
    }
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onNodeWithTag(InventoryScreenTestTags.ADD_ITEM_FAB).isDisplayed()
    }
  }

  /**
   * Navigates to the Account page using the bottom navigation bar.
   *
   * This function:
   * 1. Finds the Account tab in the navigation bar
   * 2. Verifies it is displayed
   * 3. Clicks on it to navigate to the Account screen
   * 4. Scrolls to and verifies the Sign Out button is visible
   * 5. Waits for the UI to stabilize
   */
  fun navigateToAccountPage() {
    // STEP 22: Go to account page

    composeTestRule
        .onNodeWithTag(NavigationTestTags.getTabTestTag(Tab.Account))
        .assertIsDisplayed()
        .performClick()

    // Scroll to Sign Out button
    composeTestRule
        .onNodeWithTag(UiTestTags.TAG_SIGNOUT_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()

    composeTestRule.waitForIdle()
  }

  /**
   * Signs out the user and verifies navigation back to the Sign-In screen.
   *
   * This function:
   * 1. Scrolls to and clicks the Sign Out button
   * 2. Waits for the UI to stabilize
   * 3. Waits for navigation back to the Authentication screen
   * 4. Verifies the Sign-In screen is displayed
   *
   * After sign-out, the user should be returned to the initial authentication state, requiring them
   * to sign in again to access the app.
   */
  fun signOutAndVerifyAuthScreen() {
    // STEP 23-24: User clicks signout Button
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
   * 9. User clicks search icon to navigate to Search screen
   * 10. User searches for "greg" in the username field
   * 11. User selects greg from the suggestions
   * 12. User clicks Follow button on greg's profile
   * 13. User clicks back button to return to Feed screen
   * 14. User clicks "Do a Fit Check" button to start posting a new outfit
   * 15. User reaches FitCheck screen and verifies "Add Fit Photo" button
   * 16. Note: Photo selection with camera/gallery cannot be tested in automated UI tests
   * 17. User navigates back to Feed screen from FitCheck
   * 18. User clicks notification icon
   * 19. User accepts follow notification
   * 20. User goes to inventory checks no items are there
   * 21. User checks the add item button is there
   * 23. User clicks Sign Out button
   * 24. App navigates back to Authentication screen
   *
   * LIMITATIONS:
   * - Camera/Gallery intents cannot be tested in Compose UI tests without mocking
   * - Taking photos launches external activities that break ComposeTestRule
   *
   * This test:
   * - Uses the REAL OOTDApp composable with the full NavHost navigation graph
   * - Simulates actual user interactions across multiple screens
   * - Validates automatic navigation flows between screens
   * - Tests the user lifecycle: sign-in → register → search → partial outfit post → accept
   *   notification → sign-out
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
      enterUsername(testUsername)
      enterDateOfBirth(testDateofBirth)
      enterLocation(testLocation)

      // Steps 7-8: Save registration and navigate to feed
      saveRegistrationAndNavigateToFeed()

      // Step 9: Navigate to search screen
      navigateToSearchScreen()

      // Step 10-12: Searches for Greg and follows him
      searchAndFollowUser()

      // Step 13: Navigate back to feed from search
      navigateBackToFeedFromSearch()

      // Steps 14-15: Navigate to FitCheck screen and verify photo button
      navigateToFitCheckScreen()

      // STEP 16: Skip photo selection for testing purposes
      // In a real-world scenario, the user would select a photo here
      // For automated testing, we'll proceed without it
      // Since the Next button requires a valid photo, we cannot proceed further

      // Step 17: Navigate back to feed from FitCheck
      navigateBackToFeedFromFitCheck()

      // Step 18: Open notifications screen
      openNotificationsScreen()

      // Step 19: Accept follow notification
      acceptFollowNotification()

      // Step 20-21: Navigate to inventory and check add item button exists
      navigateToInventoryAndCheckAddItemButton()

      // Step 22: Navigate to account page
      navigateToAccountPage()

      // Step 23-24: Sign out and verify auth screen
      signOutAndVerifyAuthScreen()
    }
  }
}
