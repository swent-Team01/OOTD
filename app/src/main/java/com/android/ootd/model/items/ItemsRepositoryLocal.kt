package com.android.ootd.model.items

import java.util.UUID

/**
 * Local in-device implementation of ItemsRepository for testing purposes.
 *
 * This repository stores items in a mutable map and provides the same interface as
 * ItemsRepositoryFirestore but without any network calls or Firebase dependencies. All data is
 * stored in memory and will be lost when the emulator or app is closed.
 */
class ItemsRepositoryLocal : ItemsRepository {

  private val items = mutableMapOf<String, Item>()

  /**
   * Generates a new unique identifier for an item using UUID.
   *
   * @return A new unique identifier string.
   */
  override fun getNewItemId(): String {
    return UUID.randomUUID().toString()
  }

  /**
   * Gets all items from the local storage.
   *
   * @return A list of all items currently stored.
   */
  override suspend fun getAllItems(): List<Item> {
    return items.values.toList()
  }

  /**
   * Gets a specific item by its unique identifier.
   *
   * @param uuid The unique identifier of the item to retrieve.
   * @return The item with the specified identifier.
   * @throws Exception if the item is not found.
   */
  override suspend fun getItemById(uuid: String): Item {
    return items[uuid] ?: throw Exception("ItemsRepositoryLocal: Item not found with id: $uuid")
  }

  /**
   * Adds a new item to the local storage.
   *
   * @param item The item to add.
   */
  override suspend fun addItem(item: Item) {
    items[item.itemUuid] = item
  }

  /**
   * Edits an existing item in the local storage.
   *
   * @param itemUUID The unique identifier of the item to edit.
   * @param newItem The item with updated information.
   * @throws Exception if the item is not found.
   */
  override suspend fun editItem(itemUUID: String, newItem: Item) {
    if (!items.containsKey(itemUUID)) {
      throw Exception("ItemsRepositoryLocal: Item not found with id: $itemUUID")
    }
    items[itemUUID] = newItem
  }

  /**
   * Deletes an item from the local storage by its unique identifier.
   *
   * @param uuid The unique identifier of the item to delete.
   * @throws Exception if the item is not found.
   */
  override suspend fun deleteItem(uuid: String) {
    if (!items.containsKey(uuid)) {
      throw Exception("ItemsRepositoryLocal: Item not found with id: $uuid")
    }
    items.remove(uuid)
  }
  /** Clears all items from the local storage. Useful for resetting state between tests. */
  fun clearAll() {
    items.clear()
  }

  /**
   * Gets the current count of items in the repository. Useful for testing.
   *
   * @return The number of items currently stored.
   */
  fun getItemCount(): Int {
    return items.size
  }

  /**
   * Checks if an item exists in the repository. Useful for testing.
   *
   * @param uuid The unique identifier of the item to check.
   * @return true if the item exists, false otherwise.
   */
  fun hasItem(uuid: String): Boolean {
    return items.containsKey(uuid)
  }
}
