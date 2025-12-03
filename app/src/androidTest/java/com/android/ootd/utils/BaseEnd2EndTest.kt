package com.android.ootd.utils

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.credentials.CredentialManager
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.android.ootd.model.account.AccountRepositoryFirestore
import com.android.ootd.model.account.AccountRepositoryProvider
import com.android.ootd.model.consent.Consent
import com.android.ootd.model.consent.ConsentRepository
import com.android.ootd.model.consent.ConsentRepositoryFirestore
import com.android.ootd.model.consent.ConsentRepositoryProvider
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.LocationRepository
import com.android.ootd.model.map.LocationRepositoryProvider
import com.android.ootd.model.notifications.NotificationRepositoryFirestore
import com.android.ootd.model.notifications.NotificationRepositoryProvider
import com.android.ootd.model.user.UserRepositoryFirestore
import com.android.ootd.model.user.UserRepositoryProvider
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
import org.junit.After
import org.junit.Before
import org.junit.Rule

open class BaseEnd2EndTest {
  @get:Rule val composeTestRule = createComposeRule()
  val userId = "greg"
  val testLocation = Location(47.3769, 8.5417, "Zürich, Switzerland")
  val testDateofBirth = "10102020"

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

    mockNotificationRepository = mockk(relaxed = true)
    // Inject mock repositories into the providers so the app uses them instead of real Firestore
    UserRepositoryProvider.repository = mockUserRepository
    AccountRepositoryProvider.repository = mockAccountRepository
    NotificationRepositoryProvider.repository = mockNotificationRepository
    LocationRepositoryProvider.repository = mockLocationRepository
    ConsentRepositoryProvider.repository = mockConsentRepository
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
    coEvery { mockAccountRepository.createAccount(any(), any(), any(), any(), any()) } returns Unit
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
}
