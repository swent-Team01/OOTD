package com.android.ootd.model

/** Repository class that handles data operations for items. */
interface ItemsRepository {

  /** Generates and returns a new unique identifier for a Clothing item. */
  fun getNewItemId(): String

  /**
   * Gets the all the items from the repository
   *
   * @return A list of all items.
   */
  suspend fun getAllItems(): List<Item>

  /**
   * Gets a specific item by its unique identifier.
   *
   * @param id The unique identifier of the item to retrieve.
   * @return The item with the specified identifier, or null if not found.
   */
  suspend fun getItemById(id: Long): Item?

  /**
   * Adds a new item to the repository.
   *
   * @param item The item to add.
   * @return The unique identifier of the newly added item.
   */
  suspend fun addItem(item: Item): Long

  /**
   * Edits an existing item in the repository.
   *
   * @param itemID The unique identifier of the item to edit.
   * @param newItem The item with updated information.
   * @throws Exception if the item is not found.
   */
  suspend fun edit(itemID: String, newItem: Item)

  /**
   * Deletes an item from the repository by its unique identifier.
   *
   * @param id The unique identifier of the item to delete.
   * @throws Exception if the Clothing item is not found.
   */
  suspend fun deleteItem(id: String)
}
