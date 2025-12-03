package com.android.ootd.utils

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.navigation.testing.TestNavHostController
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.screen.enterDate
import com.android.ootd.screen.enterUsername
import com.android.ootd.ui.account.AccountPageTestTags
import com.android.ootd.ui.account.UiTestTags
import com.android.ootd.ui.authentication.SignInScreenTestTags
import com.android.ootd.ui.consent.BetaConsentScreenTestTags
import com.android.ootd.ui.feed.FeedScreenTestTags
import com.android.ootd.ui.feed.OutfitPostCardTestTags
import com.android.ootd.ui.feed.OutfitPostCardTestTags.OUTFIT_POST_CARD
import com.android.ootd.ui.feed.SeeFitScreenTestTags
import com.android.ootd.ui.inventory.InventoryScreenTestTags
import com.android.ootd.ui.map.LocationSelectionTestTags
import com.android.ootd.ui.map.MapScreenTestTags
import com.android.ootd.ui.navigation.NavigationTestTags
import com.android.ootd.ui.navigation.Screen
import com.android.ootd.ui.notifications.NotificationsScreenTestTags
import com.android.ootd.ui.post.FitCheckScreenTestTags
import com.android.ootd.ui.post.PreviewItemScreenTestTags
import com.android.ootd.ui.post.items.AddItemScreenTestTags
import com.android.ootd.ui.register.RegisterScreenTestTags
import com.android.ootd.ui.search.UserProfileCardTestTags
import com.android.ootd.ui.search.UserSelectionFieldTestTags
import com.google.accompanist.permissions.ExperimentalPermissionsApi

fun verifyFeedScreenAppears(composeTestRule: ComposeContentTestRule) {
  // Verify we're on the Feed screen
  composeTestRule.waitUntil(timeoutMillis = 5000) {
    composeTestRule.onNodeWithTag(FeedScreenTestTags.SCREEN).isDisplayed()
  }
  composeTestRule.waitUntil(timeoutMillis = 5000) {
    composeTestRule.onNodeWithTag(FeedScreenTestTags.TOP_BAR).isDisplayed()
  }
}

fun verifySignInScreenAppears(composeTestRule: ComposeContentTestRule) {
  composeTestRule
      .onNodeWithTag(SignInScreenTestTags.LOGIN_BUTTON)
      .performScrollTo()
      .assertIsDisplayed()
  composeTestRule.onNodeWithTag(SignInScreenTestTags.APP_LOGO).assertIsDisplayed()
}

fun verifyRegisterScreenAppears(composeTestRule: ComposeContentTestRule) {
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
}

fun verifyInventoryScreenAppears(composeTestRule: ComposeContentTestRule) {
  composeTestRule.waitUntil(timeoutMillis = 5000) {
    composeTestRule.onNodeWithTag(InventoryScreenTestTags.SCREEN).isDisplayed()
  }
}

fun clickWithWait(
    composeTestRule: ComposeContentTestRule,
    tag: String,
    shouldScroll: Boolean = false,
    useUnmergedTree: Boolean = false
) {
  composeTestRule.waitUntil(timeoutMillis = 5000) {
    if (shouldScroll) {
      composeTestRule
          .onNodeWithTag(tag, useUnmergedTree = useUnmergedTree)
          .performScrollTo()
          .isDisplayed()
    } else {
      composeTestRule.onNodeWithTag(tag, useUnmergedTree = useUnmergedTree).isDisplayed()
    }
  }
  if (shouldScroll) {
    composeTestRule
        .onNodeWithTag(tag, useUnmergedTree = useUnmergedTree)
        .performScrollTo()
        .performClick()
  } else {
    composeTestRule.onNodeWithTag(tag, useUnmergedTree = useUnmergedTree).performClick()
  }
}

fun verifyElementDoesNotAppearWithTimer(composeTestRule: ComposeContentTestRule, tag: String) {
  composeTestRule.waitUntil(timeoutMillis = 5000) {
    composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()
  }
}

fun verifyElementAppearsWithTimer(composeTestRule: ComposeContentTestRule, tag: String) {
  composeTestRule.waitUntil(timeoutMillis = 5000) {
    composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
  }
}

/** Add a post with one item to feed */
@OptIn(ExperimentalPermissionsApi::class)
fun addPostWithOneItem(
    composeTestRule: ComposeContentTestRule,
    selectFromInventory: Boolean = false,
    inventoryItemUuid: String = ""
) {
  verifyElementAppearsWithTimer(composeTestRule, FeedScreenTestTags.ADD_POST_FAB)

  clickWithWait(composeTestRule, FeedScreenTestTags.ADD_POST_FAB, useUnmergedTree = true)
  clickWithWait(composeTestRule, FitCheckScreenTestTags.ADD_PHOTO_BUTTON)
  verifyElementAppearsWithTimer(composeTestRule, FitCheckScreenTestTags.CHOOSE_GALLERY_BUTTON)
  clickWithWait(composeTestRule, FitCheckScreenTestTags.TAKE_PHOTO_BUTTON)
  composeTestRule
      .onNodeWithTag(FitCheckScreenTestTags.DESCRIPTION_INPUT)
      .performTextInput("Sample description")

  clickWithWait(composeTestRule, FitCheckScreenTestTags.NEXT_BUTTON)
  verifyElementAppearsWithTimer(composeTestRule, PreviewItemScreenTestTags.SCREEN_TITLE)

  clickWithWait(composeTestRule, PreviewItemScreenTestTags.CREATE_ITEM_BUTTON)
  if (!selectFromInventory) {
    clickWithWait(composeTestRule, PreviewItemScreenTestTags.CREATE_NEW_ITEM_OPTION)
    clickWithWait(composeTestRule, AddItemScreenTestTags.IMAGE_PICKER)

    clickWithWait(composeTestRule, AddItemScreenTestTags.INPUT_CATEGORY)
    composeTestRule.onAllNodesWithTag(AddItemScreenTestTags.CATEGORY_SUGGESTION)[0].performClick()

    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      composeTestRule
          .onAllNodesWithTag(AddItemScreenTestTags.ADD_ITEM_BUTTON, useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
    clickWithWait(composeTestRule, AddItemScreenTestTags.ADD_ITEM_BUTTON)
  } else {
    clickWithWait(composeTestRule, PreviewItemScreenTestTags.SELECT_FROM_INVENTORY_OPTION)
    clickWithWait(composeTestRule, "${InventoryScreenTestTags.ITEM_CARD}_${inventoryItemUuid}")
  }
  verifyElementAppearsWithTimer(composeTestRule, PreviewItemScreenTestTags.POST_BUTTON)
  clickWithWait(composeTestRule, PreviewItemScreenTestTags.POST_BUTTON)
  verifyFeedScreenAppears(composeTestRule)
}

fun checkPostAppearsInFeed(composeTestRule: ComposeContentTestRule) {
  verifyElementAppearsWithTimer(composeTestRule, OUTFIT_POST_CARD)
}

fun checkItemAppearsInPost(composeTestRule: ComposeContentTestRule) {
  clickWithWait(composeTestRule, OutfitPostCardTestTags.SEE_FIT_BUTTON)
  verifyElementAppearsWithTimer(composeTestRule, SeeFitScreenTestTags.ITEMS_GRID)
  clickWithWait(composeTestRule, SeeFitScreenTestTags.NAVIGATE_TO_FEED_SCREEN)
}

fun checkStarFunctionalityForItem(composeTestRule: ComposeContentTestRule, itemUuid: String) {
  clickWithWait(composeTestRule, NavigationTestTags.INVENTORY_TAB)
  verifyElementAppearsWithTimer(composeTestRule, "${InventoryScreenTestTags.ITEM_CARD}_${itemUuid}")
  clickWithWait(composeTestRule, "${InventoryScreenTestTags.ITEM_STAR_BUTTON}_${itemUuid}")
  clickWithWait(composeTestRule, NavigationTestTags.ACCOUNT_TAB)
  clickWithWait(composeTestRule, AccountPageTestTags.STARRED_TAB, useUnmergedTree = true)
  verifyElementAppearsWithTimer(composeTestRule, "${InventoryScreenTestTags.ITEM_CARD}_${itemUuid}")
}

suspend fun addItemFromInventory(
    composeTestRule: ComposeContentTestRule,
    itemsRepository: ItemsRepository
) {
  val initialItemNumber = itemsRepository.getAllItems().count()
  clickWithWait(composeTestRule, NavigationTestTags.INVENTORY_TAB)
  clickWithWait(composeTestRule, InventoryScreenTestTags.ADD_ITEM_FAB)

  clickWithWait(composeTestRule, AddItemScreenTestTags.IMAGE_PICKER)

  clickWithWait(composeTestRule, AddItemScreenTestTags.INPUT_CATEGORY)
  composeTestRule.onAllNodesWithTag(AddItemScreenTestTags.CATEGORY_SUGGESTION)[0].performClick()

  composeTestRule.waitUntil(timeoutMillis = 5_000) {
    composeTestRule
        .onAllNodesWithTag(AddItemScreenTestTags.ADD_ITEM_BUTTON, useUnmergedTree = true)
        .fetchSemanticsNodes()
        .isNotEmpty()
  }
  clickWithWait(composeTestRule, AddItemScreenTestTags.ADD_ITEM_BUTTON)
  val finalItemNumber = itemsRepository.getAllItems().count()
  assert(finalItemNumber == initialItemNumber + 1)
}

fun searchItemInInventory(
    composeTestRule: ComposeContentTestRule,
    itemCategory: String,
    itemUuid: String
) {
  clickWithWait(composeTestRule, NavigationTestTags.INVENTORY_TAB)
  clickWithWait(composeTestRule, InventoryScreenTestTags.SEARCH_FAB)
  verifyElementAppearsWithTimer(composeTestRule, InventoryScreenTestTags.SEARCH_FIELD)

  composeTestRule.onNodeWithTag(InventoryScreenTestTags.SEARCH_FIELD).performTextInput(itemCategory)

  verifyElementAppearsWithTimer(composeTestRule, "${InventoryScreenTestTags.ITEM_CARD}_${itemUuid}")
}

fun waitForRoute(
    route: String,
    composeTestRule: ComposeContentTestRule,
    testNavController: TestNavHostController,
    timeoutMillis: Long = 10_000
) {
  composeTestRule.waitUntil(timeoutMillis) { testNavController.currentDestination?.route == route }
}

/**
 * Signs out the user and verifies navigation back to the Sign-In screen.
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
fun signOutAndVerifyAuthScreen(
    composeTestRule: ComposeContentTestRule,
    testNavController: TestNavHostController
) {
  clickWithWait(composeTestRule, NavigationTestTags.ACCOUNT_TAB)
  clickWithWait(composeTestRule, AccountPageTestTags.SETTINGS_BUTTON)
  clickWithWait(composeTestRule, UiTestTags.TAG_SIGNOUT_BUTTON, shouldScroll = true)
  waitForRoute(
      route = Screen.Authentication.route,
      composeTestRule = composeTestRule,
      testNavController = testNavController)
  verifyElementAppearsWithTimer(composeTestRule, SignInScreenTestTags.LOGIN_BUTTON)
  verifySignInScreenAppears(composeTestRule)
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
fun enterUsername(composeTestRule: ComposeContentTestRule, testUsername: String) {
  verifyElementAppearsWithTimer(composeTestRule, RegisterScreenTestTags.INPUT_REGISTER_UNAME)
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
fun enterDateOfBirth(composeTestRule: ComposeContentTestRule, testDateofBirth: String) {
  clickWithWait(composeTestRule, RegisterScreenTestTags.DATE_PICKER_ICON, useUnmergedTree = true)
  verifyElementAppearsWithTimer(composeTestRule, RegisterScreenTestTags.REGISTER_DATE_PICKER)
  // Enter date and confirm
  composeTestRule.enterDate(testDateofBirth)
  composeTestRule.waitForIdle()
}

fun fullRegisterSequence(
    composeTestRule: ComposeContentTestRule,
    username: String,
    dateOfBirth: String,
    acceptBetaScreen: Boolean = true
) {
  verifyElementAppearsWithTimer(composeTestRule, SignInScreenTestTags.LOGIN_BUTTON)
  clickWithWait(composeTestRule, SignInScreenTestTags.LOGIN_BUTTON, true)

  // Use default logins
  enterUsername(composeTestRule, username)
  enterDateOfBirth(composeTestRule, dateOfBirth)
  clickWithWait(
      composeTestRule, LocationSelectionTestTags.LOCATION_DEFAULT_EPFL, shouldScroll = true)
  // Finish registration

  clickWithWait(composeTestRule, RegisterScreenTestTags.REGISTER_SAVE, shouldScroll = true)
  // Go through the confirmation screen:
  if (acceptBetaScreen) {
    clickWithWait(composeTestRule, BetaConsentScreenTestTags.CHECKBOX)
    clickWithWait(composeTestRule, BetaConsentScreenTestTags.AGREE_BUTTON)
  }
  verifyFeedScreenAppears(composeTestRule)
}

/**
 * Searches for the user with the given username and follows him
 *
 * This function:
 * 1. Inputs the name greg in the search screen
 * 2. Selects the user greg from the dropdown
 * 3. Follows greg by clicking the follow button
 */
fun searchAndFollowUser(composeTestRule: ComposeContentTestRule, username: String) {
  clickWithWait(composeTestRule, NavigationTestTags.SEARCH_TAB)
  clickWithWait(composeTestRule, UserSelectionFieldTestTags.INPUT_USERNAME)
  composeTestRule
      .onNodeWithTag(UserSelectionFieldTestTags.INPUT_USERNAME)
      .performTextInput(username)

  verifyElementAppearsWithTimer(composeTestRule, UserSelectionFieldTestTags.USERNAME_SUGGESTION)
  // Click on the first suggestion
  composeTestRule
      .onAllNodesWithTag(UserSelectionFieldTestTags.USERNAME_SUGGESTION)[0]
      .performClick()
  clickWithWait(composeTestRule, UserProfileCardTestTags.USER_FOLLOW_BUTTON)
}

fun loginWithoutRegistering(composeTestRule: ComposeContentTestRule) {
  verifyElementAppearsWithTimer(composeTestRule, SignInScreenTestTags.LOGIN_BUTTON)
  clickWithWait(composeTestRule, SignInScreenTestTags.LOGIN_BUTTON, true)
}

fun openNotificationsScreenAndAcceptNotification(composeTestRule: ComposeContentTestRule) {
  clickWithWait(composeTestRule, FeedScreenTestTags.NAVIGATE_TO_NOTIFICATIONS_SCREEN)
  verifyElementAppearsWithTimer(composeTestRule, NotificationsScreenTestTags.NOTIFICATION_ITEM)
  composeTestRule.onAllNodesWithTag(NotificationsScreenTestTags.ACCEPT_BUTTON)[0].performClick()
  verifyElementDoesNotAppearWithTimer(
      composeTestRule, NotificationsScreenTestTags.NOTIFICATION_ITEM)
}

fun checkNumberOfPostsInFeed(composeTestRule: ComposeContentTestRule, lowerThreshold: Int) {
  clickWithWait(composeTestRule, NavigationTestTags.FEED_TAB)

  verifyElementAppearsWithTimer(composeTestRule, OUTFIT_POST_CARD)

  // Scroll to bottom to force all items to compose at least once
  // Tests if it can do it to the number of items we set
  composeTestRule
      .onNodeWithTag(FeedScreenTestTags.FEED_LIST)
      .performScrollToIndex(lowerThreshold - 1)
}

fun checkPostsAppearInAccountTab(composeTestRule: ComposeContentTestRule) {
  clickWithWait(composeTestRule, NavigationTestTags.ACCOUNT_TAB)

  verifyElementAppearsWithTimer(composeTestRule, AccountPageTestTags.AVATAR_LETTER)

  composeTestRule.waitUntil(timeoutMillis = 5000) {
    composeTestRule.onNodeWithTag(AccountPageTestTags.POST_TAG).performScrollTo().isDisplayed()
  }
}

fun checkOutMap(composeTestRule: ComposeContentTestRule) {
  clickWithWait(composeTestRule, NavigationTestTags.MAP_TAB)

  verifyElementAppearsWithTimer(composeTestRule, MapScreenTestTags.TOP_BAR_TITLE)
}
