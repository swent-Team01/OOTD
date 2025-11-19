package com.android.ootd.ui.items

import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.map.Location
import com.android.ootd.model.user.User

class FakeItemsRepository : ItemsRepository {
  var lastAddedItem: Item? = null

  override fun getNewItemId(): String = "fake-item"

  override suspend fun getAllItems(): List<Item> = emptyList()

  override suspend fun getItemById(uuid: String): Item {
    throw UnsupportedOperationException("Not needed for unit tests")
  }

  override suspend fun getItemsByIds(uuids: List<String>): List<Item> = emptyList()

  override suspend fun getAssociatedItems(postUuid: String): List<Item> = emptyList()

  override suspend fun addItem(item: Item) {
    lastAddedItem = item
  }

  override suspend fun editItem(itemUUID: String, newItem: Item) {
    // no-op
  }

  override suspend fun deleteItem(uuid: String) {
    // no-op
  }

  override suspend fun deletePostItems(postUuid: String) {
    // no-op
  }

  override suspend fun getFriendItemsForPost(postUuid: String, friendId: String): List<Item> =
      emptyList()
}

class FakeAccountRepository : AccountRepository {
  override suspend fun createAccount(
      user: User,
      userEmail: String,
      dateOfBirth: String,
      location: Location
  ) {
    // no-op
  }

  override suspend fun addAccount(account: Account) {
    // no-op
  }

  override suspend fun getAccount(userID: String): Account {
    throw UnsupportedOperationException("Not needed for unit tests")
  }

  override suspend fun accountExists(userID: String): Boolean = false

  override suspend fun addFriend(userID: String, friendID: String): Boolean = false

  override suspend fun removeFriend(userID: String, friendID: String) {
    // no-op
  }

  override suspend fun isMyFriend(userID: String, friendID: String): Boolean = false

  override suspend fun togglePrivacy(userID: String): Boolean = false

  override suspend fun deleteAccount(userID: String) {
    // no-op
  }

  override suspend fun editAccount(
      userID: String,
      username: String,
      birthDay: String,
      picture: String,
      location: Location
  ) {
    // no-op
  }

  override suspend fun deleteProfilePicture(userID: String) {
    // no-op
  }

  override suspend fun getItemsList(userID: String): List<String> = emptyList()

  override suspend fun addItem(itemUid: String): Boolean = true

  override suspend fun removeItem(itemUid: String): Boolean = true
}
