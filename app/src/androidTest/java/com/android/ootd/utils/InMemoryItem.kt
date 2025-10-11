package com.android.ootd.utils

import com.android.ootd.model.Item
import com.android.ootd.model.ItemsRepository

object InMemoryItem : ItemsTest {
  override fun createInitializedRepository(): ItemsRepository {
    return InMemoryItemsRepository()
  }

  class InMemoryItemsRepository(val itemList: MutableList<Item> = mutableListOf<Item>()) :
      ItemsRepository {

    override suspend fun addItem(item: com.android.ootd.model.Item) {
      itemList.add(item)
    }

    override suspend fun editItem(itemUUID: String, newItem: com.android.ootd.model.Item) {
      itemList.replaceAll { if (it.uuid == itemUUID) newItem else it }
    }

    override suspend fun deleteItem(uuid: String) {
      itemList.removeIf { it.uuid == uuid }
    }

    override fun getNewItemId(): String {
      return "${itemList.size}"
    }

    override suspend fun getAllItems(): List<com.android.ootd.model.Item> {
      return itemList
    }

    override suspend fun getItemById(uuid: String): com.android.ootd.model.Item {
      return itemList.first { it.uuid == uuid }
    }
  }
}
