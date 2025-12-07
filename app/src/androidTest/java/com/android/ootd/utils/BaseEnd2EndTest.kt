package com.android.ootd.utils

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.credentials.CredentialManager
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.android.ootd.OOTDApp
import com.android.ootd.model.account.AccountRepositoryFirestore
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.consent.Consent
import com.android.ootd.model.consent.ConsentRepository
import com.android.ootd.model.consent.ConsentRepositoryFirestore
import com.android.ootd.model.consent.ConsentRepositoryProvider
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.ItemsRepositoryProvider
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.LocationRepository
import com.android.ootd.model.map.LocationRepositoryProvider
import com.android.ootd.model.notifications.Notification
import com.android.ootd.model.notifications.NotificationRepositoryFirestore
import com.android.ootd.model.notifications.NotificationRepositoryProvider
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepositoryFirestore
import com.android.ootd.model.user.UserRepositoryProvider
import com.android.ootd.ui.authentication.SignInScreenTestTags
import com.android.ootd.ui.feed.FeedScreenTestTags
import com.android.ootd.ui.inventory.InventoryScreenTestTags
import com.android.ootd.ui.map.LocationSelectionTestTags
import com.android.ootd.ui.navigation.NavigationTestTags
import com.android.ootd.ui.navigation.Screen
import com.android.ootd.ui.notifications.NotificationsScreenTestTags
import com.android.ootd.ui.register.RegisterScreenTestTags
import com.android.ootd.ui.search.SearchScreenTestTags
import com.android.ootd.ui.search.UserProfileCardTestTags
import com.android.ootd.ui.search.UserSelectionFieldTestTags
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.firestore
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule

open class BaseEnd2EndTest {
  @get:Rule val composeTestRule = createComposeRule()
  val userId = "greg"
  val testLocation = Location(47.3769, 8.5417, "Zürich, Switzerland")
  val testDateofBirth = "10102010"

  lateinit var context: Context
  lateinit var mockFirebaseAuth: FirebaseAuth
  lateinit var mockFirebaseUser: FirebaseUser
  lateinit var mockAuthResult: AuthResult
  lateinit var mockUserRepository: UserRepositoryFirestore
  lateinit var mockAccountRepository: AccountRepositoryFirestore
  lateinit var mockNotificationRepository: NotificationRepositoryFirestore
  lateinit var mockLocationRepository: LocationRepository
  lateinit var mockConsentRepository: ConsentRepository
  lateinit var testUserId: String
  lateinit var testUsername: String
  lateinit var fakeGoogleIdToken: String
  lateinit var fakeCredentialManager: CredentialManager
  lateinit var testNavController: TestNavHostController
  lateinit var mockItemsRepository: ItemsRepository

  // If you want to add a new repository, you need to make sure to add it here.
  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    mockFirebaseAuth = mockk(relaxed = true)
    mockFirebaseUser = mockk(relaxed = true)
    mockAuthResult = mockk(relaxed = true)
    mockUserRepository = mockk(relaxed = true)
    mockAccountRepository = mockk(relaxed = true)
    mockLocationRepository = mockk(relaxed = true)
    mockConsentRepository = mockk(relaxed = true)
    mockItemsRepository = mockk(relaxed = true)
    mockNotificationRepository = mockk(relaxed = true)
    // Inject mock repositories into the providers so the app uses them instead of real Firestore
    UserRepositoryProvider.repository = mockUserRepository
    AccountRepositoryProvider.repository = mockAccountRepository
    NotificationRepositoryProvider.repository = mockNotificationRepository
    LocationRepositoryProvider.repository = mockLocationRepository
    ConsentRepositoryProvider.repository = mockConsentRepository
    ItemsRepositoryProvider.repository = mockItemsRepository

    // Generate unique identifiers for each test run to avoid conflicts
    val timestamp = System.currentTimeMillis()
    testUserId = "test_user_$timestamp"
    testUsername = "user$timestamp"

    // Pre-populate SharedPreferences with consent data to skip consent screen
    val consentPrefs = context.getSharedPreferences("ootd_beta_consent", Context.MODE_PRIVATE)
    consentPrefs.edit().apply {
      putBoolean("ootd_consent_given", true)
      putLong("ootd_consent_timestamp", timestamp)
      putString("ootd_consent_uuid", "test_consent_uuid_$timestamp")
      apply()
    }
    // Create a fake Google ID token for a new user
    fakeGoogleIdToken = FakeJwtGenerator.createFakeGoogleIdToken(userId, email = "greg@gmail.com")
    fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)
    // Ensure clean state
    FirebaseAuth.getInstance().signOut()
  }

  @After
  fun tearDown() {
    unmockkAll()

    // Clean up SharedPreferences
    val consentPrefs = context.getSharedPreferences("ootd_beta_consent", Context.MODE_PRIVATE)
    consentPrefs.edit().clear().apply()

    // Restore the real repositories after test completes
    UserRepositoryProvider.repository = UserRepositoryFirestore(Firebase.firestore)
    AccountRepositoryProvider.repository = AccountRepositoryFirestore(Firebase.firestore)
    LocationRepositoryProvider.repository =
        com.android.ootd.model.map.NominatimLocationRepository(
            com.android.ootd.HttpClientProvider.client)
    ConsentRepositoryProvider.repository = ConsentRepositoryFirestore(Firebase.firestore)
  }

  fun initializeMocksForTest() {
    // Set up mock Firebase authentication with new unique test user ID
    every { mockFirebaseUser.uid } returns testUserId
    every { mockAuthResult.user } returns mockFirebaseUser

    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockFirebaseAuth
    every { mockFirebaseAuth.currentUser } returns null // Initially not signed in
    every { mockFirebaseAuth.signInWithCredential(any()) } returns Tasks.forResult(mockAuthResult)

    // Mock signOut to update currentUser to null when called
    every { mockFirebaseAuth.signOut() } answers
        {
          every { mockFirebaseAuth.currentUser } returns null
        }

    // New user, so userExists returns false initially (no username set yet)
    // After registration, it will return true
    coEvery { mockUserRepository.userExists(any()) } returns false

    // Mock successful user creation - use any() matchers since we're testing the flow, not exact
    // values
    coEvery { mockUserRepository.createUser(any(), any()) } returns Unit

    // Mock successful account creation
    coEvery { mockAccountRepository.createAccount(any(), any(), any(), any()) } returns Unit
    coEvery { mockAccountRepository.accountExists(any()) } returns false

    // Mock consent as already given (user has consented, so consent screen will be skipped)
    val mockConsent =
        Consent(
            consentUuid = "mock_consent_uuid",
            userId = testUserId,
            timestamp = System.currentTimeMillis(),
            version = "1.0")
    coEvery { mockConsentRepository.getConsentByUserId(any()) } returns mockConsent
  }

  fun initTestNavController() {
    testNavController =
        TestNavHostController(context).apply {
          navigatorProvider.addNavigator(ComposeNavigator())
          navigatorProvider.addNavigator(DialogNavigator())
        }
  }

  fun waitForRoute(route: String, timeoutMillis: Long = 10_000) {
    if (!::testNavController.isInitialized) return
    composeTestRule.waitUntil(timeoutMillis) {
      testNavController.currentDestination?.route == route
    }
  }

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
    initTestNavController()
    composeTestRule.setContent {
      OOTDApp(
          context = context,
          credentialManager = fakeCredentialManager,
          testMode = true,
          testNavController = testNavController)
    }
    waitForRoute(Screen.Authentication.route)
    verifyElementAppearsWithTimer(composeTestRule, SignInScreenTestTags.LOGIN_BUTTON)
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
    // Update mock to return the signed-in user after sign-in
    every { mockFirebaseAuth.currentUser } returns mockFirebaseUser
    clickWithWait(composeTestRule, SignInScreenTestTags.LOGIN_BUTTON, true)
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
    waitForRoute(Screen.RegisterUsername.route)
    verifyElementAppearsWithTimer(composeTestRule, RegisterScreenTestTags.REGISTER_SAVE)
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
    // Update mock behavior BEFORE clicking Save to avoid race conditions
    // After successful registration, userExists should return true

    coEvery { mockUserRepository.userExists(any()) } returns true
    coEvery { mockUserRepository.createUser(any(), any()) } returns Unit

    // Ensure the Save button is visible by scrolling to it if necessary
    clickWithWait(composeTestRule, RegisterScreenTestTags.REGISTER_SAVE, shouldScroll = true)

    waitForRoute(Screen.Feed.route)
    verifyFeedScreenAppears(composeTestRule)
    composeTestRule.waitForIdle()
  }

  fun navigateToSearchScreen() {
    clickWithWait(composeTestRule, NavigationTestTags.SEARCH_TAB)
    waitForRoute(Screen.SearchScreen.route)
    verifyElementAppearsWithTimer(composeTestRule, SearchScreenTestTags.SEARCH_SCREEN)
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
   * Opens the notifications screen from the Feed screen.
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
                  content = "Wants to follow you",
                  senderName = ""))
        }
    clickWithWait(composeTestRule, NavigationTestTags.FEED_TAB)
    clickWithWait(composeTestRule, FeedScreenTestTags.NAVIGATE_TO_NOTIFICATIONS_SCREEN)
    waitForRoute(Screen.NotificationsScreen.route)
    verifyElementAppearsWithTimer(
        composeTestRule, NotificationsScreenTestTags.PUSH_NOTIFICATIONS_INSTRUCTIONS)
    clickWithWait(composeTestRule, NotificationsScreenTestTags.ENABLE_PUSH_NOTIFICATIONS)
    verifyElementAppearsWithTimer(composeTestRule, NotificationsScreenTestTags.NOTIFICATIONS_SCREEN)
    verifyElementAppearsWithTimer(composeTestRule, NotificationsScreenTestTags.NOTIFICATION_ITEM)
  }

  /** Accepts follow notification from the notification screen and checks it dissapeared after. */
  fun acceptFollowNotification() {
    composeTestRule.onAllNodesWithTag(NotificationsScreenTestTags.ACCEPT_BUTTON)[0].performClick()
    verifyElementDoesNotAppearWithTimer(
        composeTestRule, NotificationsScreenTestTags.NOTIFICATION_ITEM)
  }

  /**
   * Goes to the inventory by clicking the corresponding tab button and checks the items button is
   * there.
   */
  fun navigateToInventoryAndCheckAddItemButton() {
    clickWithWait(composeTestRule, NavigationTestTags.INVENTORY_TAB)
    waitForRoute(Screen.InventoryScreen.route)
    verifyInventoryScreenAppears(composeTestRule)
    verifyElementAppearsWithTimer(composeTestRule, InventoryScreenTestTags.EMPTY_STATE)
    verifyElementAppearsWithTimer(composeTestRule, InventoryScreenTestTags.ADD_ITEM_FAB)
  }
}
