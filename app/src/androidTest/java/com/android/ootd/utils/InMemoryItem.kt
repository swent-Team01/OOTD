package com.android.ootd.utils

import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.items.ItemsRepositoryProvider

object InMemoryItem : ItemsTest {
  override val repository: ItemsRepository
    get() = ItemsRepositoryProvider.repository

  override fun createInitializedRepository(): ItemsRepository {
    return InMemoryItemsRepository()
  }

  class InMemoryItemsRepository(val itemList: MutableList<Item> = mutableListOf<Item>()) :
      ItemsRepository {

    override suspend fun addItem(item: Item) {
      itemList.add(item)
    }

    override suspend fun editItem(itemUUID: String, newItem: Item) {
      itemList.replaceAll { if (it.itemUuid == itemUUID) newItem else it }
    }

    override suspend fun deleteItem(uuid: String) {
      itemList.removeIf { it.itemUuid == uuid }
    }

    override fun getNewItemId(): String {
      return "${itemList.size}"
    }

    override suspend fun getAllItems(): List<Item> {
      return itemList
    }

    override suspend fun getItemById(uuid: String): Item {
      return itemList.first { it.itemUuid == uuid }
    }
  }
}
