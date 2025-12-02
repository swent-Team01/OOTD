package com.android.ootd.end2end

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.credentials.CredentialManager
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.OOTDApp
import com.android.ootd.model.map.Location
import com.android.ootd.ui.navigation.NavigationTestTags
import com.android.ootd.utils.FakeCredentialManager
import com.android.ootd.utils.FakeJwtGenerator
import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreTest
import com.android.ootd.utils.addItemFromInventory
import com.android.ootd.utils.addPostWithOneItem
import com.android.ootd.utils.checkItemAppearsInPost
import com.android.ootd.utils.checkNumberOfPostsInFeed
import com.android.ootd.utils.checkPostAppearsInFeed
import com.android.ootd.utils.checkStarFunctionalityForItem
import com.android.ootd.utils.clickWithWait
import com.android.ootd.utils.fullRegisterSequence
import com.android.ootd.utils.loginWithoutRegistering
import com.android.ootd.utils.openNotificationsScreenAndAcceptNotification
import com.android.ootd.utils.searchAndFollowUser
import com.android.ootd.utils.searchItemInInventory
import com.android.ootd.utils.signOutAndVerifyAuthScreen
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
class ThirdEnd2EndTest : FirestoreTest() {
  @get:Rule val composeTestRule = createComposeRule()

  val testLocation = Location(47.3769, 8.5417, "Zürich, Switzerland")
  val testDateofBirth = "10102020"
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
   * - Uses the REAL OOTDApp composable with the full NavHost navigation graph.
   * - Skips the authentication step as it was tested in the other 2 end to end tests.
   * - Uses the firebase emulator databases for a closer experience to what the application should
   *   feel like.
   */
  @Test
  fun fullAppFlow_newUser_3() = runBlocking {

    /** Add user before tests start* */
    FirebaseEmulator.auth.signOut()

    initTestNavController()
    composeTestRule.setContent {
      OOTDApp(
          context = context,
          credentialManager = fakeCredentialManager,
          testMode = true,
          testNavController = testNavController)
    }

    fullRegisterSequence(
        composeTestRule = composeTestRule, username = "user_1", dateOfBirth = testDateofBirth)

    addItemFromInventory(composeTestRule, itemsRepository = itemsRepository)

    val firstItemUuid = itemsRepository.getAllItems()[0].itemUuid

    val firstItemCategory = itemsRepository.getItemById(firstItemUuid).category

    searchItemInInventory(
        composeTestRule, itemCategory = firstItemCategory, itemUuid = firstItemUuid)

    clickWithWait(composeTestRule, NavigationTestTags.FEED_TAB)

    addPostWithOneItem(
        composeTestRule,
        selectFromInventory = true,
        inventoryItemUuid = firstItemUuid) // Test adding item from inventory works as well
    checkPostAppearsInFeed(composeTestRule)

    checkItemAppearsInPost(composeTestRule)

    checkStarFunctionalityForItem(composeTestRule, firstItemUuid)

    signOutAndVerifyAuthScreen(composeTestRule, testNavController = testNavController)
    FakeCredentialManager.changeCredential(fakeGoogleIdToken2)

    fullRegisterSequence(
        composeTestRule = composeTestRule,
        username = "user_2",
        dateOfBirth = testDateofBirth,
        acceptBetaScreen = false)

    addPostWithOneItem(composeTestRule)

    searchAndFollowUser(composeTestRule, "user_1")

    signOutAndVerifyAuthScreen(composeTestRule, testNavController = testNavController)
    FakeCredentialManager.changeCredential(fakeGoogleIdToken)

    loginWithoutRegistering(composeTestRule = composeTestRule)

    openNotificationsScreenAndAcceptNotification(composeTestRule = composeTestRule)

    // Each user made a post and the current user is the friend of the other
    // So this user can see both posts.
    checkNumberOfPostsInFeed(composeTestRule = composeTestRule, userRepository.getAllUsers().size)
  }
}
