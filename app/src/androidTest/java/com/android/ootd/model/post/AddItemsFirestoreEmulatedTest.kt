package com.android.ootd.model.post

import androidx.compose.ui.test.junit4.createComposeRule
import com.android.ootd.model.ItemsRepository
import com.android.ootd.ui.post.AddItemsScreen
import com.android.ootd.ui.post.AddItemsViewModel
import com.android.ootd.utils.FirebaseEmulator
import com.android.ootd.utils.FirestoreItemTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AddItemsFirestoreEmulatedTest : FirestoreItemTest() {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: AddItemsViewModel
  private lateinit var repo: ItemsRepository

  @Before
  override fun setUp() {
    super.setUp()
    FirebaseEmulator
    repo = createInitializedRepository()
    viewModel = AddItemsViewModel(repo)
    composeTestRule.setContent { AddItemsScreen(viewModel) }
  }

  @After
  override fun tearDown() {
    runBlocking {
      if (FirebaseEmulator.isRunning) {
        // This uses FirestoreItemTest’s helper method
        FirebaseEmulator.clearFirestoreEmulator()
      }
    }
    super.tearDown()
  }

  @Test
  fun canStoreNewItemInDatabase() =
      runTest {
        // Fill inputs
        //      composeTestRule.enterAddItemDetails(item1)
        //
        //      composeTestRule.runOnIdle { viewModel.setPhoto(item1.image) }

        // composeTestRule.clickOnSaveForAddItem(true)
        //      val item = ItemsTest.item4
        //      composeTestRule.enterAddItemDetails(item)
        //      composeTestRule.runOnIdle { viewModel.setPhoto(item.image) }
        //
        //      composeTestRule.waitUntil(timeoutMillis = 5_000) {
        //
        // composeTestRule.onAllNodesWithTag(AddItemScreenTestTags.ADD_ITEM_BUTTON).fetchSemanticsNodes().isNotEmpty()
        //      }
        //
        //      composeTestRule.onNodeWithTag(AddItemScreenTestTags.ADD_ITEM_BUTTON)
        //          .assertIsDisplayed()
        //          .performClick()
        //
        //      composeTestRule.waitForIdle()
        //
        //
        //      assertEquals(1, countItems())
        //      val items = repo.getAllItems()
        //      val storedItem = items.first()
        //      item1.isEqual(storedItem)
        //      assertTrue(item1.isEqual(storedItem))
      }

  //  @Test
  //  fun cannotAddItemWithMissingRequiredFields() = runTest {
  //    // Leave out required fields (category, type)
  //    composeTestRule.enterAddItemBrand(item1.brand ?: "")
  //    composeTestRule.enterAddItemPrice(item1.price ?: 0.0)
  //    composeTestRule.enterAddItemLink(item1.link?: "")
  //    composeTestRule.runOnIdle { viewModel.setPhoto(item1.image) }
  //    composeTestRule.clickOnSaveForAddItem()
  //    composeTestRule.waitForIdle()
  //    // No item should be added to the database
  //    assertEquals(0, countItems())
  //  }

  // Will be added when the navigation is imlemented
  //  @Test
  //  fun canAddMultipleItems() = runTest {
  //    composeTestRule.enterAddItemDetails(item1)
  //    composeTestRule.runOnIdle {
  //      viewModel.setPhoto(item1.image)
  //    }
  //    composeTestRule.clickOnSaveForAddItem(true)
  //    // Wait for Firestore write
  //    composeTestRule.waitForIdle()
  //    assertEquals(1, countItems())
  //
  //    // Add second item
  //    composeTestRule.enterAddItemDetails(item2)
  //    composeTestRule.runOnIdle {
  //      viewModel.setPhoto(item2.image)
  //    }
  //    composeTestRule.clickOnSaveForAddItem(true)
  //    // Wait for Firestore write
  //    composeTestRule.waitForIdle()
  //    assertEquals(2, countItems())
  //
  //    composeTestRule.enterAddItemDetails(item4)
  //    composeTestRule.runOnIdle {
  //      viewModel.setPhoto(item4.image)
  //    }
  //    composeTestRule.clickOnSaveForAddItem(true)
  //    // Wait for Firestore write
  //    composeTestRule.waitForIdle()
  //
  //    assertEquals(3, countItems())
  //
  //    val items = repo.getAllItems()
  //    val storedItem1 = items.find { it.uuid == item1.uuid }
  //    val storedItem2 = items.find { it.uuid == item2.uuid }
  //    val storedItem3 = items.find { it.uuid == item4.uuid }
  //    assertTrue(storedItem1 != null && item1.isEqual(storedItem1))
  //    assertTrue(storedItem2 != null && item2.isEqual(storedItem2))
  //    assertTrue(storedItem3 != null && item4.isEqual(storedItem3))
  //  }

}
