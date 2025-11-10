package com.android.ootd.model.items

/** Repository class that handles data operations for items. */
interface ItemsRepository {

  /** Generates and returns a new unique identifier for a Clothing item. */
  fun getNewItemId(): String

  /**
   * Gets all the items from the repository
   *
   * @return A list of all items.
   */
  suspend fun getAllItems(): List<Item>

  /**
   * Gets a specific item by its unique identifier.
   *
   * @param uuid The unique identifier of the item to retrieve.
   * @return The item with the specified identifier, or null if not found.
   * @throws Exception if the item is not found.
   */
  suspend fun getItemById(uuid: String): Item

  /**
   * Gets multiple items by their unique identifiers in a single query.
   *
   * @param uuids The list of unique identifiers of items to retrieve.
   * @return A list of found items (may be smaller than uuids if some don't exist).
   */
  suspend fun getItemsByIds(uuids: List<String>): List<Item>

  /**
   * Gets all the items associated with a specific post.
   *
   * @param postUuid The unique identifier of the post.
   */
  suspend fun getAssociatedItems(postUuid: String): List<Item>

  /**
   * Adds a new item to the repository.
   *
   * @param item The item to add.
   */
  suspend fun addItem(item: Item)

  /**
   * Edits an existing item in the repository.
   *
   * @param itemUUID The unique identifier of the item to edit.
   * @param newItem The item with updated information.
   * @throws Exception if the item is not found.
   */
  suspend fun editItem(itemUUID: String, newItem: Item)

  /**
   * Deletes an item from the repository by its unique identifier.
   *
   * @param uuid The unique identifier of the item to delete.
   * @throws Exception if the Clothing item is not found.
   */
  suspend fun deleteItem(uuid: String)

  /**
   * Deletes all items associated with a specific post.
   *
   * @param postUuid The unique identifier of the post.
   */
  suspend fun deletePostItems(postUuid: String)
}
