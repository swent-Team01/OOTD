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
   * Gets all items from the local storage that belong to a specific post.
   *
   * @param postUuid The unique identifier of the post to filter by.
   */
  override suspend fun getAssociatedItems(postUuid: String): List<Item> {
    return items.values.filter { it.postUuids.contains(postUuid) }
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
   * Gets multiple items by their unique identifiers.
   *
   * @param uuids The list of unique identifiers of items to retrieve.
   * @return A list of found items (may be smaller than uuids if some don't exist).
   */
  override suspend fun getItemsByIds(uuids: List<String>): List<Item> {
    return uuids.mapNotNull { items[it] }
  }

  override suspend fun getItemsByIdsAcrossOwners(uuids: List<String>): List<Item> {
    return getItemsByIds(uuids)
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

  /**
   * Deletes all items associated with a specific post from the local storage.
   *
   * @param postUuid The unique identifier of the post whose items are to be deleted.
   */
  override suspend fun deletePostItems(postUuid: String) {
    items.values.removeIf { it.postUuids.contains(postUuid) }
  }

  /**
   * Gets items associated with a specific post that belong to a specific friend.
   *
   * @param postUuid The unique identifier of the post to filter by.
   * @param friendId The unique identifier of the friend whose items to retrieve.
   * @return A list of items associated with the post and owned by the specified friend.
   */
  override suspend fun getFriendItemsForPost(postUuid: String, friendId: String): List<Item> {
    return items.values.filter { it.postUuids.contains(postUuid) && it.ownerId == friendId }
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
