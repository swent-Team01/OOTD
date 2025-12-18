package com.android.ootd.ui.post

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.account.PublicLocation
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.map.Location
import com.android.ootd.model.user.User
import com.android.ootd.utils.InMemoryItem
import com.android.ootd.utils.ItemsTest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SelectInventoryItemScreenTest : ItemsTest by InMemoryItem {
  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockFirebaseAuth: FirebaseAuth
  private lateinit var mockFirebaseUser: FirebaseUser

  @Before
  override fun setUp() {
    super.setUp()
    mockFirebaseAuth = mockk(relaxed = true)
    mockFirebaseUser = mockk(relaxed = true)

    // Mock Firebase auth
    mockkStatic(FirebaseAuth::class)
    every { FirebaseAuth.getInstance() } returns mockFirebaseAuth
    every { mockFirebaseAuth.currentUser } returns mockFirebaseUser
    every { mockFirebaseUser.uid } returns "test_user_123"
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  private fun item(id: String = "1", postUuids: List<String> = emptyList()) =
      Item(
          id,
          postUuids,
          ImageData("i$id", "url$id"),
          "Clothing",
          "Shirt",
          "Brand",
          10.0,
          "CHF",
          emptyList(),
          null,
          "user1")

  private val fakeAccountRepo =
      object : AccountRepository {
        override suspend fun getItemsList(userID: String) = listOf("1", "2", "3")

        override suspend fun createAccount(
            user: User,
            userEmail: String,
            dateOfBirth: String,
            location: Location,
            isPrivate: Boolean
        ) {}

        override suspend fun addAccount(account: Account) {}

        override suspend fun getAccount(userID: String) = Account()

        override suspend fun accountExists(userID: String) = true

        override suspend fun addFriend(userID: String, friendID: String) = true

        override suspend fun removeFriend(userID: String, friendID: String) {}

        override suspend fun isMyFriend(userID: String, friendID: String) = false

        override suspend fun togglePrivacy(userID: String) = false

        override suspend fun deleteAccount(userID: String) {}

        override suspend fun editAccount(
            userID: String,
            username: String,
            birthDay: String,
            picture: String,
            location: Location
        ) {}

        override suspend fun deleteProfilePicture(userID: String) {}

        override suspend fun addItem(itemUid: String) = true

        override suspend fun removeItem(itemUid: String) = true

        override fun observeAccount(userID: String): Flow<Account> = flowOf(Account())

        override suspend fun getStarredItems(userID: String): List<String> = emptyList()

        override suspend fun refreshStarredItems(userID: String): List<String> = emptyList()

        override suspend fun addStarredItem(itemUid: String): Boolean = true

        override suspend fun removeStarredItem(itemUid: String): Boolean = true

        override suspend fun toggleStarredItem(itemUid: String): List<String> = emptyList()

        override suspend fun getPublicLocations(): List<PublicLocation> = emptyList()

        override fun observePublicLocations(): Flow<List<PublicLocation>> = flowOf(emptyList())
      }

  private fun fakeItemsRepo(items: List<Item>, postItems: List<Item> = emptyList()) =
      object : ItemsRepository {
        override fun getNewItemId() = "new"

        override suspend fun getAllItems() = items

        override suspend fun getItemById(uuid: String) = items.first { it.itemUuid == uuid }

        override suspend fun getItemsByIds(uuids: List<String>) =
            items.filter { it.itemUuid in uuids }

        override suspend fun getItemsByIdsAcrossOwners(uuids: List<String>) = getItemsByIds(uuids)

        override suspend fun getAssociatedItems(postUuid: String) = postItems

        override suspend fun addItem(item: Item) {}

        override suspend fun editItem(itemUUID: String, newItem: Item) {}

        override suspend fun deleteItem(uuid: String) {}

        override suspend fun deletePostItems(postUuid: String) {}

        override suspend fun getFriendItemsForPost(
            postUuid: String,
            friendId: String,
            isPublicPost: Boolean
        ): List<Item> {
          return emptyList()
        }
      }

  @Test
  fun screenDisplays_withTitleAndBackButton() {
    composeTestRule.setContent {
      SelectInventoryItemScreen(
          "post1", SelectInventoryItemViewModel(fakeAccountRepo, fakeItemsRepo(emptyList())))
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(SelectInventoryItemScreenTestTags.TITLE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(SelectInventoryItemScreenTestTags.GO_BACK_BUTTON)
        .assertIsDisplayed()
  }

  @Test
  fun emptyInventory_showsEmptyState() {
    composeTestRule.setContent {
      SelectInventoryItemScreen(
          "post1", SelectInventoryItemViewModel(fakeAccountRepo, fakeItemsRepo(emptyList())))
    }

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(SelectInventoryItemScreenTestTags.EMPTY_STATE)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(SelectInventoryItemScreenTestTags.EMPTY_STATE).assertIsDisplayed()
  }

  @Test
  fun itemsAvailable_showsGrid() {
    val items = listOf(item("1"), item("2"), item("3"))
    composeTestRule.setContent {
      SelectInventoryItemScreen(
          "post1", SelectInventoryItemViewModel(fakeAccountRepo, fakeItemsRepo(items)))
    }

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(SelectInventoryItemScreenTestTags.ITEMS_GRID)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(SelectInventoryItemScreenTestTags.ITEMS_GRID).assertIsDisplayed()
  }

  @Test
  fun clickingItem_triggersCallback() {
    val items = listOf(item("1"), item("2"), item("3"))
    var selected = false

    composeTestRule.setContent {
      SelectInventoryItemScreen(
          "post1",
          SelectInventoryItemViewModel(fakeAccountRepo, fakeItemsRepo(items)),
          onItemSelected = { selected = true })
    }

    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule.onAllNodesWithTag("inventoryItemCard_1").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithTag("inventoryItemCard_1").performClick()
    composeTestRule.waitForIdle()
    assert(selected)
  }
}
