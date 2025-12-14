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
import com.android.ootd.ui.navigation.NavigationTestTags
import com.android.ootd.ui.theme.OOTDTheme
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

  companion object {
    private const val TAG = "ThirdEnd2EndTest"
  }

  val testLocation = Location(47.3769, 8.5417, "Zürich, Switzerland")
  val testDateofBirth = "10102010"
  val fakeGoogleIdToken2 =
      FakeJwtGenerator.createFakeGoogleIdToken("test3_user_2", email = "test_3@gmail.com")

  lateinit var testNavController: TestNavHostController
  lateinit var context: Context
  lateinit var fakeCredentialManager: CredentialManager
  lateinit var fakeGoogleIdToken: String

  @Before
  override fun setUp() {
    super.setUp()
    context = ApplicationProvider.getApplicationContext()
    fakeGoogleIdToken =
        FakeJwtGenerator.createFakeGoogleIdToken("test_3_user_1", email = "test_3_2@gmail.com")
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
   * 1. Create user and go through the whole sign-in process
   * 2. Add item to inventory
   * 3. Make sure that the search function works in inventory
   * 4. Create a post with the item that was added in inventory.
   * 5. Check that the star functionality works as intended
   * 6. Create second user to interact with the first one
   * 7. Create post for the second user
   * 8. Make the second user follow the first user
   * 9. Logout from the second user and login into the first user
   * 10. Accept the follow request of the second user on the first user account
   * 11. Verify that the first user can now see the post of the first user
   *
   * LIMITATIONS:
   * - Taking photos launches external activities that break ComposeTestRule, therefore we hardcode
   *   an image uri for testing.
   *
   * This test:
   * - Uses the REAL OOTDApp composable with the full NavHost navigation graph.
   * - Skips the authentication step as it was tested in the other 2 end to end tests.
   * - Uses the firebase emulator databases for a closer experience to what the application should
   *   feel like.
   * - Creates multiple users to test interactions between them
   */
  @Test
  fun fullAppFlow_newUser_3() = runBlocking {

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

    // STEP 1: Create user and go through the whole sign-in process
    Log.d(TAG, "STEP1: fullRegisterSequence for user_1")
    fullRegisterSequence(
        composeTestRule = composeTestRule, username = "user_1", dateOfBirth = testDateofBirth)

    // STEP 2: Add item to inventory
    Log.d(TAG, "STEP2: add item to inventory for user_1")
    addItemFromInventory(
        composeTestRule,
        itemsRepository = itemsRepository,
        accountRepository = accountRepository,
        userId = FirebaseEmulator.auth.currentUser?.uid ?: "")

    val firstItemUuid = itemsRepository.getAllItems()[0].itemUuid

    val firstItemCategory = itemsRepository.getItemById(firstItemUuid).category

    // STEP 3: Make sure that the search function works in inventory
    Log.d(TAG, "STEP3: search inventory for newly added item")
    searchItemInInventory(
        composeTestRule, itemCategory = firstItemCategory, itemUuid = firstItemUuid)

    clickWithWait(composeTestRule, NavigationTestTags.FEED_TAB)

    // STEP 4: Create a post with the item that was added in inventory.
    Log.d(TAG, "STEP4: create post using inventory item")
    addPostWithOneItem(
        composeTestRule,
        selectFromInventory = true,
        inventoryItemUuid = firstItemUuid) // Test adding item from inventory works as well

    Log.d(TAG, "STEP4b: verify post and item details")
    checkPostAppearsInFeed(composeTestRule)

    // Uncomment later when the issue fixed
     checkItemAppearsInPost(composeTestRule)

    // STEP 5: Check that the star functionality works as intended
    Log.d(TAG, "STEP5: toggle star on item in inventory")
    checkStarFunctionalityForItem(composeTestRule, firstItemUuid)
    // STEP 6: Create second user to interact with the first one
    Log.d(TAG, "STEP6: sign out user_1 and register user_2")
    signOutAndVerifyAuthScreen(composeTestRule, testNavController = testNavController)
    FakeCredentialManager.changeCredential(fakeGoogleIdToken2)

    fullRegisterSequence(
        composeTestRule = composeTestRule,
        username = "user_2",
        dateOfBirth = testDateofBirth,
        acceptBetaScreen = false)

    // STEP 7: Create post for the second user
    Log.d(TAG, "STEP7: user_2 creates a post")
    addPostWithOneItem(composeTestRule)

    // STEP 8: Make the second user follow the first user
    Log.d(TAG, "STEP8: user_2 follows user_1")
    searchAndFollowUser(composeTestRule, "user_1")

    // STEP 9: Logout from the second user and login into the first user
    Log.d(TAG, "STEP9: switch back to user_1 credentials")
    signOutAndVerifyAuthScreen(composeTestRule, testNavController = testNavController)
    FakeCredentialManager.changeCredential(fakeGoogleIdToken)

    Log.d(TAG, "STEP9b: loginWithoutRegistering for user_1")
    loginWithoutRegistering(composeTestRule = composeTestRule)

    // STEP 10: Accept the follow request of the second user on the first user account
    Log.d(TAG, "STEP10: accept follow request in notifications")
    openNotificationsScreenAndAcceptNotification(composeTestRule = composeTestRule)

    // STEP 11: Verify that the first user can now see the post of the first user
    Log.d(TAG, "STEP11: verify feed contains posts from followed users")
    checkNumberOfPostsInFeed(composeTestRule = composeTestRule, userRepository.getAllUsers().size)
  }
}
