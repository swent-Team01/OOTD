package com.android.ootd.utils

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.ui.Inventory.InventoryScreenTestTags
import com.android.ootd.ui.account.AccountPageTestTags
import com.android.ootd.ui.authentication.SignInScreenTestTags
import com.android.ootd.ui.feed.FeedScreenTestTags
import com.android.ootd.ui.feed.OutfitPostCardTestTags
import com.android.ootd.ui.feed.OutfitPostCardTestTags.OUTFIT_POST_CARD
import com.android.ootd.ui.feed.SeeFitScreenTestTags
import com.android.ootd.ui.navigation.NavigationTestTags
import com.android.ootd.ui.post.FitCheckScreenTestTags
import com.android.ootd.ui.post.PreviewItemScreenTestTags
import com.android.ootd.ui.post.items.AddItemScreenTestTags
import com.android.ootd.ui.register.RegisterScreenTestTags
import com.android.ootd.utils.InMemoryItem.ensureVisible
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
fun addPostWithOneItem(composeTestRule: ComposeContentTestRule) {
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
  clickWithWait(composeTestRule, PreviewItemScreenTestTags.CREATE_NEW_ITEM_OPTION)
  clickWithWait(composeTestRule, AddItemScreenTestTags.IMAGE_PICKER)

  clickWithWait(composeTestRule, AddItemScreenTestTags.INPUT_CATEGORY)
  composeTestRule.onAllNodesWithTag(AddItemScreenTestTags.CATEGORY_SUGGESTION)[0].performClick()

  composeTestRule.ensureVisible(AddItemScreenTestTags.ADD_ITEM_BUTTON)
  composeTestRule.waitUntil(timeoutMillis = 5_000) {
    composeTestRule
        .onAllNodesWithTag(AddItemScreenTestTags.ADD_ITEM_BUTTON, useUnmergedTree = true)
        .fetchSemanticsNodes()
        .isNotEmpty()
  }
  clickWithWait(composeTestRule, AddItemScreenTestTags.ADD_ITEM_BUTTON, shouldScroll = true)

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

  composeTestRule.ensureVisible(AddItemScreenTestTags.ADD_ITEM_BUTTON)
  composeTestRule.waitUntil(timeoutMillis = 5_000) {
    composeTestRule
        .onAllNodesWithTag(AddItemScreenTestTags.ADD_ITEM_BUTTON, useUnmergedTree = true)
        .fetchSemanticsNodes()
        .isNotEmpty()
  }
  clickWithWait(composeTestRule, AddItemScreenTestTags.ADD_ITEM_BUTTON, shouldScroll = true)
  val finalItemNumber = itemsRepository.getAllItems().count()
  assert(finalItemNumber == initialItemNumber + 1)
}
