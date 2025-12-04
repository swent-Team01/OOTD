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
    fullRegisterSequence(
        composeTestRule = composeTestRule, username = "user_1", dateOfBirth = testDateofBirth)

    // STEP 2: Add item to inventory
    addItemFromInventory(
        composeTestRule,
        itemsRepository = itemsRepository,
        accountRepository = accountRepository,
        userId = FirebaseEmulator.auth.currentUser?.uid ?: "")

    val firstItemUuid = itemsRepository.getAllItems()[0].itemUuid

    val firstItemCategory = itemsRepository.getItemById(firstItemUuid).category

    // STEP 3: Make sure that the search function works in inventory
    searchItemInInventory(
        composeTestRule, itemCategory = firstItemCategory, itemUuid = firstItemUuid)

    clickWithWait(composeTestRule, NavigationTestTags.FEED_TAB)

    // STEP 4: Create a post with the item that was added in inventory.
    addPostWithOneItem(
        composeTestRule,
        selectFromInventory = false,
        inventoryItemUuid = firstItemUuid) // Test adding item from inventory works as well

    checkPostAppearsInFeed(composeTestRule)

    checkItemAppearsInPost(composeTestRule)
    // STEP 5: Check that the star functionality works as intended
    checkStarFunctionalityForItem(composeTestRule, firstItemUuid)
    // STEP 6: Create second user to interact with the first one
    signOutAndVerifyAuthScreen(composeTestRule, testNavController = testNavController)
    FakeCredentialManager.changeCredential(fakeGoogleIdToken2)

    fullRegisterSequence(
        composeTestRule = composeTestRule,
        username = "user_2",
        dateOfBirth = testDateofBirth,
        acceptBetaScreen = false)

    // STEP 7: Create post for the second user
    addPostWithOneItem(composeTestRule)

    // STEP 8: Make the second user follow the first user
    searchAndFollowUser(composeTestRule, "user_1")

    // STEP 9: Logout from the second user and login into the first user
    signOutAndVerifyAuthScreen(composeTestRule, testNavController = testNavController)
    FakeCredentialManager.changeCredential(fakeGoogleIdToken)

    loginWithoutRegistering(composeTestRule = composeTestRule)

    // STEP 10: Accept the follow request of the second user on the first user account
    openNotificationsScreenAndAcceptNotification(composeTestRule = composeTestRule)

    // STEP 11: Verify that the first user can now see the post of the first user

    checkNumberOfPostsInFeed(composeTestRule = composeTestRule, userRepository.getAllUsers().size)
  }
}
