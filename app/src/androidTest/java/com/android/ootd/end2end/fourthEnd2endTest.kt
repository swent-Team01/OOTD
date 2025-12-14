package com.android.ootd.end2end

import android.content.Context
import android.util.Log
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.credentials.CredentialManager
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.OOTDApp
import com.android.ootd.model.map.Location
import com.android.ootd.ui.theme.OOTDTheme
import com.android.ootd.utils.FakeCredentialManager
import com.android.ootd.utils.FakeJwtGenerator
import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import com.android.ootd.utils.addPostWithOneItem
import com.android.ootd.utils.checkNumberOfPostsInFeed
import com.android.ootd.utils.checkOutMap
import com.android.ootd.utils.checkOutfitView
import com.android.ootd.utils.checkPostAppearsInFeed
import com.android.ootd.utils.checkPostsAppearInAccountTab
import com.android.ootd.utils.fullRegisterSequence
import com.android.ootd.utils.loginWithoutRegistering
import com.android.ootd.utils.openNotificationsScreenAndAcceptNotification
import com.android.ootd.utils.searchAndFollowUser
import com.android.ootd.utils.signOutAndVerifyAuthScreen
import com.android.ootd.utils.verifyPressingLocationGoesToMap
import kotlinx.coroutines.runBlocking
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
class FourthEnd2EndTest : FirestoreTest() {
  @get:Rule val composeTestRule = createComposeRule()

  companion object {
    private const val TAG = "FourthEnd2EndTest"
  }

  val testLocation = Location(47.3769, 8.5417, "Zürich, Switzerland")
  val testDateofBirth = "10102010"
  val testUsername = "testTestUser"
  val fakeGoogleIdToken2 =
      FakeJwtGenerator.createFakeGoogleIdToken("brbrbrbrbrbr", email = "greg_2@gmail.com")

  lateinit var testNavController: TestNavHostController
  lateinit var context: Context
  lateinit var fakeCredentialManager: CredentialManager
  lateinit var fakeGoogleIdToken: String

  @Before
  override fun setUp() {
    super.setUp()
    context = ApplicationProvider.getApplicationContext()
    fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken(currentUser.uid, email = "greg@gmail.com")
    fakeCredentialManager = FakeCredentialManager.create(fakeGoogleIdToken)
  }

  fun initTestNavController() {
    testNavController =
        TestNavHostController(context).apply {
          navigatorProvider.addNavigator(ComposeNavigator())
          navigatorProvider.addNavigator(DialogNavigator())
        }
  }

  /**
   * End-to-end test: Complete user journey from sign-in through registration, search, follow,
   * attempting to post an outfit, adding an item to the inventory and then logging out.
   *
   * This test validates the FULL application flow using the actual navigation system:
   * 1. Login as the first user and go through the whole sign-in process
   * 2. Add a post in the feed of the first user
   * 3. Make sure the post appears in the feed
   * 4. Logout and login as second user
   * 5. Create a post with one item for the second user
   * 6. Follow the first user from the second user account
   * 7. Logout from second user and login as first user
   * 8. Accept follow request from second user from first user's account
   * 9. Check that both posts of the users appear in the first user's feed
   * 10. Check that you can see your posts in the account tab. functionality
   * 11. Checks that the map appears and that pressing on the post location leads to the map
   * 12. Check that pressing the image and liking it in the image view works LIMITATIONS:
   * - Taking photos launches external activities that break ComposeTestRule
   * - The map marker functionality cannot be tested as they are not composables so they cannot be
   *   clicked. This test:
   * - Uses the REAL OOTDApp composable with the full NavHost navigation graph.
   * - Skips the authentication step as it was tested in the other 2 end to end tests.
   * - Uses the firebase emulator databases for a closer experience to what the application should
   *   feel like.
   */
  @Test
  fun fullAppFlow_newUser_4() = runBlocking {

    /** Add user before tests start* */
    FirebaseEmulator.auth.signOut()

    initTestNavController()
    Log.d(TAG, "Setting content and launching OOTDApp")
    composeTestRule.setContent {
      OOTDTheme {
        OOTDApp(
            context = context,
            credentialManager = fakeCredentialManager,
            testMode = true,
            testNavController = testNavController)
      }
    }

    // STEP 1: Login as the first user and go through the whole sign-in process
    Log.d(TAG, "STEP1: fullRegisterSequence for user_1")
    fullRegisterSequence(
        composeTestRule = composeTestRule, username = "user_1", dateOfBirth = testDateofBirth)

    // STEP 2: Add a post in the feed of the first user
    Log.d(TAG, "STEP2: add post with location for user_1")
    addPostWithOneItem(
        composeTestRule, addLocation = true) // Test adding item from inventory works as well

    // STEP 3: Make sure the post appears in the feed
    Log.d(TAG, "STEP3: verify post appears in feed for user_1")
    checkPostAppearsInFeed(composeTestRule)

    // STEP 4: Logout and login as second user
    Log.d(TAG, "STEP4: sign out user_1 and switch credentials to user_2")
    signOutAndVerifyAuthScreen(composeTestRule, testNavController = testNavController)
    FakeCredentialManager.changeCredential(fakeGoogleIdToken2)

    Log.d(TAG, "STEP4b: fullRegisterSequence for user_2 (skip onboarding flag)")
    fullRegisterSequence(
        composeTestRule = composeTestRule,
        username = "user_2",
        dateOfBirth = testDateofBirth,
        acceptBetaScreen = false)
    val user2UID = FirebaseEmulator.auth.currentUser?.uid ?: ""
    // STEP 5: Create a post with one item for the second user
    Log.d(TAG, "STEP5: add post with location for user_2")
    addPostWithOneItem(composeTestRule, addLocation = true)

    // STEP 6: Follow the first user from the second user account
    Log.d(TAG, "STEP6: user_2 follows user_1")
    searchAndFollowUser(composeTestRule, "user_1")

    // STEP 7: Logout from second user and login as first user
    Log.d(TAG, "STEP7: sign out user_2 and switch back to user_1")
    signOutAndVerifyAuthScreen(composeTestRule, testNavController = testNavController)
    FakeCredentialManager.changeCredential(fakeGoogleIdToken)

    Log.d(TAG, "STEP7b: loginWithoutRegistering for user_1")
    loginWithoutRegistering(composeTestRule = composeTestRule)

    // STEP 8: Accept follow request from second user from first user's account
    Log.d(TAG, "STEP8: accept follow request in notifications")
    openNotificationsScreenAndAcceptNotification(composeTestRule = composeTestRule)

    val user1 = userRepository.getUser(FirebaseEmulator.auth.currentUser?.uid ?: "")
    assert(user1.friendCount == 1)

    val user2 = userRepository.getUser(user2UID)
    assert(user2.friendCount == 1)

    // STEP 9: Check that both posts of the users appear in the first user's feed
    Log.d(TAG, "STEP9: check number of posts in feed for user_1")
    checkNumberOfPostsInFeed(composeTestRule = composeTestRule, userRepository.getAllUsers().size)

    // STEP 10: Check out whether you can see posts on your profile
    Log.d(TAG, "STEP10: check posts appear in account tab")
    checkPostsAppearInAccountTab(composeTestRule = composeTestRule)

    // STEP 11 - Checks that the map appears and that pressing on the post location leads to the map
    Log.d(TAG, "STEP11: verify map navigation from post location")
    checkOutMap(composeTestRule = composeTestRule)
    verifyPressingLocationGoesToMap(composeTestRule = composeTestRule)

    // STEP 12 - Check that pressing the image and liking it in the image view works
    checkOutfitView(composeTestRule = composeTestRule)
  }
}
