package com.android.ootd.end2end

import androidx.compose.ui.test.assertTextContains
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
import com.android.ootd.ui.account.AccountPageTestTags
import com.android.ootd.ui.account.UiTestTags
import com.android.ootd.ui.authentication.SignInScreenTestTags
import com.android.ootd.ui.feed.FeedScreenTestTags
import com.android.ootd.ui.map.LocationSelectionTestTags
import com.android.ootd.ui.navigation.NavigationTestTags
import com.android.ootd.ui.notifications.NotificationsScreenTestTags
import com.android.ootd.ui.post.FitCheckScreenTestTags
import com.android.ootd.ui.post.PreviewItemScreenTestTags
import com.android.ootd.ui.post.items.AddItemScreenTestTags
import com.android.ootd.ui.register.RegisterScreenTestTags
import com.android.ootd.ui.search.SearchScreenTestTags
import com.android.ootd.ui.search.UserProfileCardTestTags
import com.android.ootd.ui.search.UserSelectionFieldTestTags
import com.android.ootd.utils.BaseEnd2EndTest
import com.android.ootd.utils.InMemoryItem.ensureVisible
import com.android.ootd.utils.clickWithWait
import com.android.ootd.utils.verifyElementAppearsWithTimer
import com.android.ootd.utils.verifyElementDoesNotAppearWithTimer
import com.android.ootd.utils.verifyFeedScreenAppears
import com.android.ootd.utils.verifyInventoryScreenAppears
import com.android.ootd.utils.verifyRegisterScreenAppears
import com.android.ootd.utils.verifySignInScreenAppears
import com.google.accompanist.permissions.ExperimentalPermissionsApi
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
   * STEP 1-2: Launches the full OOTD application and waits for the login screen to appear.
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
    composeTestRule.setContent {
      OOTDApp(context = context, credentialManager = fakeCredentialManager, testMode = true)
    }
    verifyElementAppearsWithTimer(composeTestRule, SignInScreenTestTags.LOGIN_BUTTON)
    verifySignInScreenAppears(composeTestRule)
  }

  /**
   * STEP 3: Simulates pressing the Google Sign-In button on the Authentication screen.
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
    // Update mock to return the signed-in user after sign-in
    every { mockFirebaseAuth.currentUser } returns mockFirebaseUser
    clickWithWait(composeTestRule, SignInScreenTestTags.LOGIN_BUTTON, true)
  }

  /**
   * STEP 4: Waits for automatic navigation to the Registration screen after successful sign-in.
   *
   * This function waits until the Register Save button is visible on screen, indicating that the
   * app has successfully navigated to the Registration screen.
   *
   * This navigation occurs automatically when:
   * - User successfully signs in with Google
   * - The user account doesn't exist in the database (userExists returns false)
   */
  fun waitNavigationRegisterScreen() {
    verifyElementAppearsWithTimer(composeTestRule, RegisterScreenTestTags.REGISTER_SAVE)
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
   * STEP 6: Enters a date of birth using the date picker in the registration form.
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
    clickWithWait(composeTestRule, RegisterScreenTestTags.DATE_PICKER_ICON, useUnmergedTree = true)
    verifyElementAppearsWithTimer(composeTestRule, RegisterScreenTestTags.REGISTER_DATE_PICKER)
    // Enter date and confirm
    composeTestRule.enterDate(testDateofBirth)
    composeTestRule.waitForIdle()
  }

  /**
   * STEP 6b: Selects a location from the location picker in the registration form.
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
    // Mock the location repository to return suggestions when user types "Zurich"

    coEvery { mockLocationRepository.search(any()) } returns listOf(testLocation)
    // Enter location text in the input field
    clickWithWait(composeTestRule, LocationSelectionTestTags.INPUT_LOCATION, true)
    composeTestRule.waitForIdle()

    // Type "Zurich" to trigger location search
    composeTestRule
        .onNodeWithTag(com.android.ootd.ui.map.LocationSelectionTestTags.INPUT_LOCATION)
        .performTextInput("Zurich")
    composeTestRule.waitForIdle()

    verifyElementAppearsWithTimer(composeTestRule, LocationSelectionTestTags.LOCATION_SUGGESTION)
    composeTestRule
        .onAllNodesWithTag(com.android.ootd.ui.map.LocationSelectionTestTags.LOCATION_SUGGESTION)[0]
        .performClick()
    composeTestRule.waitForIdle()
  }

  /**
   * STEP 7-8: Saves the registration form and waits for navigation to the Feed screen.
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
    // Update mock behavior BEFORE clicking Save to avoid race conditions
    // After successful registration, userExists should return true

    coEvery { mockUserRepository.userExists(any()) } returns true
    coEvery { mockUserRepository.createUser(any(), any()) } returns Unit

    // Ensure the Save button is visible by scrolling to it if necessary
    clickWithWait(composeTestRule, RegisterScreenTestTags.REGISTER_SAVE, shouldScroll = true)

    // More robust waiting with better error handling
    composeTestRule.waitUntil(timeoutMillis = 10000) {
      composeTestRule
          .onAllNodesWithTag(FeedScreenTestTags.SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    verifyFeedScreenAppears(composeTestRule)
    composeTestRule.waitForIdle()
  }

  /** STEP 9 */
  @OptIn(ExperimentalPermissionsApi::class)
  fun addPostWithOneItem() {

    clickWithWait(composeTestRule, FeedScreenTestTags.ADD_POST_FAB)
    clickWithWait(composeTestRule, FitCheckScreenTestTags.ADD_PHOTO_BUTTON)
    verifyElementAppearsWithTimer(composeTestRule, FitCheckScreenTestTags.CHOOSE_GALLERY_BUTTON)
    clickWithWait(composeTestRule, FitCheckScreenTestTags.TAKE_PHOTO_BUTTON)
    composeTestRule
        .onNodeWithTag(FitCheckScreenTestTags.DESCRIPTION_INPUT)
        .performTextInput("Sample description")

    clickWithWait(composeTestRule, FitCheckScreenTestTags.NEXT_BUTTON)
    verifyElementAppearsWithTimer(composeTestRule, PreviewItemScreenTestTags.SCREEN_TITLE)

    clickWithWait(composeTestRule, PreviewItemScreenTestTags.CREATE_ITEM_BUTTON)
    clickWithWait(composeTestRule, PreviewItemScreenTestTags.CREATE_NEW_ITEM_OPTION)
    clickWithWait(composeTestRule, AddItemScreenTestTags.IMAGE_PICKER)

    clickWithWait(composeTestRule, AddItemScreenTestTags.INPUT_CATEGORY)
    composeTestRule.onAllNodesWithTag(AddItemScreenTestTags.CATEGORY_SUGGESTION)[0].performClick()

    composeTestRule.ensureVisible(AddItemScreenTestTags.ADD_ITEM_BUTTON)
    clickWithWait(composeTestRule, AddItemScreenTestTags.ADD_ITEM_BUTTON)
    clickWithWait(composeTestRule, PreviewItemScreenTestTags.POST_BUTTON)

    verifyFeedScreenAppears(composeTestRule)
  }

  /**
   * STEP 10: Navigates from the Feed screen to the Search screen.
   *
   * This function:
   * 1. Clicks the search icon on the Feed screen
   * 2. Waits for the Search screen to appear
   * 3. Verifies the Search screen is displayed
   */
  fun navigateToSearchScreen() {
    clickWithWait(composeTestRule, NavigationTestTags.SEARCH_TAB)
    verifyElementAppearsWithTimer(composeTestRule, SearchScreenTestTags.SEARCH_SCREEN)
  }

  /**
   * STEP 10-12: Searches for the user Greg and follows him
   *
   * This function:
   * 1. Inputs the name greg in the search screen
   * 2. Selects the user greg from the dropdown
   * 3. Follows greg by clicking the follow button
   */
  fun searchAndFollowUser() {
    coEvery { mockUserRepository.getAllUsers() } returns
        runBlocking { listOf(User(username = userId)) }

    composeTestRule.onNodeWithTag(UserSelectionFieldTestTags.INPUT_USERNAME).performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(UserSelectionFieldTestTags.INPUT_USERNAME)
        .performTextInput(userId)
    composeTestRule.waitForIdle()

    verifyElementAppearsWithTimer(composeTestRule, UserSelectionFieldTestTags.USERNAME_SUGGESTION)

    // Click on the first suggestion (Greg)
    composeTestRule
        .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)[0]
        .performClick()
    clickWithWait(composeTestRule, UserProfileCardTestTags.USER_FOLLOW_BUTTON)
  }

  /**
   * STEP 13: Opens the notifications screen from the Feed screen.
   *
   * This function:
   * 1. Waits for the notification icon to be initialized and visible
   * 2. Clicks the notification icon to navigate to the notifications screen
   * 3. Waits for the UI to stabilize
   * 4. Enables push notifications
   * 5. Tests to see that there is a follow notification for the user on the screen
   */
  fun openNotificationsScreen() {
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
    clickWithWait(composeTestRule, NavigationTestTags.FEED_TAB)
    clickWithWait(composeTestRule, FeedScreenTestTags.NAVIGATE_TO_NOTIFICATIONS_SCREEN)
    verifyElementAppearsWithTimer(
        composeTestRule, NotificationsScreenTestTags.PUSH_NOTIFICATIONS_INSTRUCTIONS)
    clickWithWait(composeTestRule, NotificationsScreenTestTags.ENABLE_PUSH_NOTIFICATIONS)
    verifyElementAppearsWithTimer(composeTestRule, NotificationsScreenTestTags.NOTIFICATIONS_SCREEN)
    verifyElementAppearsWithTimer(composeTestRule, NotificationsScreenTestTags.NOTIFICATION_ITEM)
  }

  /**
   * STEP 14: Accepts follow notification from the notification screen and checks it dissapeared
   * after.
   */
  fun acceptFollowNotification() {
    composeTestRule.onAllNodesWithTag(NotificationsScreenTestTags.ACCEPT_BUTTON)[0].performClick()
    verifyElementDoesNotAppearWithTimer(
        composeTestRule, NotificationsScreenTestTags.NOTIFICATION_ITEM)
  }

  /**
   * STEPS 15-16: Goes to the inventory by clicking the corresponding tab button and checks the
   * items button is there.
   */
  fun navigateToInventoryAndCheckAddItemButton() {
    clickWithWait(composeTestRule, NavigationTestTags.INVENTORY_TAB)
    verifyInventoryScreenAppears(composeTestRule)
    verifyElementAppearsWithTimer(composeTestRule, InventoryScreenTestTags.EMPTY_STATE)
    verifyElementAppearsWithTimer(composeTestRule, InventoryScreenTestTags.ADD_ITEM_FAB)
  }

  /**
   * STEP 17-18: Signs out the user and verifies navigation back to the Sign-In screen.
   *
   * This function:
   * 1. Goes to account page
   * 2. Goes to the account settings
   * 3. Clicks the Sign Out button
   * 4. Waits for the UI to stabilize
   * 5. Waits for navigation back to the Authentication screen
   * 6. Verifies the Sign-In screen is displayed
   *
   * After sign-out, the user should be returned to the initial authentication state, requiring them
   * to sign in again to access the app.
   */
  fun signOutAndVerifyAuthScreen() {
    clickWithWait(composeTestRule, NavigationTestTags.ACCOUNT_TAB)
    clickWithWait(composeTestRule, AccountPageTestTags.SETTINGS_BUTTON)
    clickWithWait(composeTestRule, UiTestTags.TAG_SIGNOUT_BUTTON, shouldScroll = true)
    verifyElementAppearsWithTimer(composeTestRule, SignInScreenTestTags.LOGIN_BUTTON)
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
      enterUsername(testUsername)
      enterDateOfBirth(testDateofBirth)
      enterLocation(testLocation)

      // Steps 7-8: Save registration and navigate to feed
      saveRegistrationAndNavigateToFeed()

      // Step 9: Add post with one item attached to it and check it is displayed in feed
      addPostWithOneItem()

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
      signOutAndVerifyAuthScreen()
    }
  }
}
