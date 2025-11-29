package com.android.ootd.model.items

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

const val ITEMS_COLLECTION = "items"
const val OWNER_ATTRIBUTE_NAME = "ownerId"
const val POST_ATTRIBUTE_NAME = "postUuids"
const val NOT_LOGGED_IN_EXCEPTION = "ItemsRepositoryFirestore: User not logged in."
private const val TAG = "ItemsRepositoryFirestore"

class ItemsRepositoryFirestore(private val db: FirebaseFirestore) : ItemsRepository {

  // In-memory cache for items - updated optimistically for offline support
  // ConcurrentHashMap ensures thread-safe reads/writes across coroutines
  private val itemsCache = java.util.concurrent.ConcurrentHashMap<String, Item>()

  override fun getNewItemId(): String {
    return db.collection(ITEMS_COLLECTION).document().id
  }

  override suspend fun getAllItems(): List<Item> {
    val ownerId = Firebase.auth.currentUser?.uid ?: throw Exception(NOT_LOGGED_IN_EXCEPTION)
    val snapshot =
        db.collection(ITEMS_COLLECTION).whereEqualTo(OWNER_ATTRIBUTE_NAME, ownerId).get().await()
    return snapshot.mapNotNull { mapToItem(it) }
  }

  override suspend fun getAssociatedItems(postUuid: String): List<Item> {
    val ownerId = Firebase.auth.currentUser?.uid ?: throw Exception(NOT_LOGGED_IN_EXCEPTION)

    val snapshot =
        db.collection(ITEMS_COLLECTION)
            .whereArrayContains(POST_ATTRIBUTE_NAME, postUuid)
            .whereEqualTo(OWNER_ATTRIBUTE_NAME, ownerId)
            .get()
            .await()

    return snapshot.mapNotNull { mapToItem(it) }
  }

  override suspend fun getItemById(uuid: String): Item {
    // Check memory cache first
    itemsCache[uuid]?.let {
      Log.d("ItemsRepositoryFirestore", "Returning item $uuid from memory cache")
      return it
    }

    val doc = db.collection(ITEMS_COLLECTION).document(uuid).get().await()
    val item = mapToItem(doc) ?: throw Exception("ItemsRepositoryFirestore: Item not found")
    // Update cache
    itemsCache[uuid] = item
    return item
  }

  override suspend fun getItemsByIds(uuids: List<String>): List<Item> {
    val ownerId = Firebase.auth.currentUser?.uid ?: throw Exception(NOT_LOGGED_IN_EXCEPTION)
    return fetchItemsByIds(uuids, ownerId)
  }

  override suspend fun getItemsByIdsAcrossOwners(uuids: List<String>): List<Item> {
    return fetchItemsByIds(uuids, ownerFilter = null)
  }

  private fun mapToItem(doc: DocumentSnapshot): Item? {
    return try {
      val data = doc.data ?: return null
      ItemsMappers.parseItem(data)
    } catch (_: Exception) {
      null
    }
  }

  /**
   * Shared implementation for both owner-filtered and cross-owner item lookups.
   *
   * When [ownerFilter] is non-null we must respect Firestore security rules, so each document is
   * fetched individually and skipped when it belongs to a different owner. When null, we can batch
   * the get via `whereIn`. The cache is always respected first to avoid duplicate reads.
   */
  private suspend fun fetchItemsByIds(uuids: List<String>, ownerFilter: String?): List<Item> {
    if (uuids.isEmpty()) return emptyList()

    val cachedItems = uuids.mapNotNull { itemsCache[it] }
    if (cachedItems.size == uuids.size) {
      Log.d(TAG, "Returning all ${uuids.size} items from memory cache")
      return cachedItems
    }

    if (cachedItems.isNotEmpty()) {
      Log.d(TAG, "Found ${cachedItems.size}/${uuids.size} items in cache, fetching rest")
    }

    val missingUuids = uuids.filterNot { itemsCache.containsKey(it) }
    val fetchedItems = fetchMissingItems(missingUuids, ownerFilter)
    return cachedItems + fetchedItems
  }

  private suspend fun fetchMissingItems(
      missingUuids: List<String>,
      ownerFilter: String?
  ): List<Item> {
    if (missingUuids.isEmpty()) return emptyList()

    val fetched = mutableListOf<Item>()
    try {
      kotlinx.coroutines.withTimeoutOrNull(2_000L) {
        missingUuids.chunked(10).forEach { batch -> fetched += fetchBatch(batch, ownerFilter) }
      }
      Log.d(TAG, "Fetched ${fetched.size} items from Firestore (missing=${missingUuids.size})")
    } catch (e: Exception) {
      Log.w(TAG, "Failed to fetch missing items (offline): ${e.message}")
    }
    return fetched
  }

  private suspend fun fetchBatch(batch: List<String>, ownerFilter: String?): List<Item> {
    return if (ownerFilter == null) fetchCrossOwnerBatch(batch)
    else fetchSingleOwnerBatch(batch, ownerFilter)
  }

  private suspend fun fetchCrossOwnerBatch(batch: List<String>): List<Item> {
    val snapshot =
        db.collection(ITEMS_COLLECTION).whereIn(FieldPath.documentId(), batch).get().await()
    val fetchedItems = snapshot.mapNotNull { mapToItem(it) }
    fetchedItems.forEach { itemsCache[it.itemUuid] = it }
    return fetchedItems
  }

  private suspend fun fetchSingleOwnerBatch(batch: List<String>, ownerFilter: String): List<Item> {
    val matches = mutableListOf<Item>()
    batch.forEach { id ->
      val doc = db.collection(ITEMS_COLLECTION).document(id).get().await()
      val owner = doc.getString(OWNER_ATTRIBUTE_NAME)
      if (doc.exists() && owner == ownerFilter) {
        mapToItem(doc)?.let {
          itemsCache[it.itemUuid] = it
          matches.add(it)
        }
      }
    }
    return matches
  }

  override suspend fun addItem(item: Item) {
    // Optimistically update memory cache immediately
    itemsCache[item.itemUuid] = item
    Log.d(TAG, "Added item ${item.itemUuid} to memory cache")

    // Queue Firestore update with timeout
    try {
      kotlinx.coroutines.withTimeoutOrNull(2_000L) {
        db.collection(ITEMS_COLLECTION).document(item.itemUuid).set(item).await()
      }
      Log.d(TAG, "Item added to Firestore (or queued if offline)")
    } catch (e: Exception) {
      Log.w(TAG, "Firestore add queued (offline): ${e.message}")
    }
  }

  override suspend fun editItem(itemUUID: String, newItem: Item) {
    // Determine if item exists (cache fast-path, then Firestore with timeout)
    val existedInCache = itemsCache.containsKey(itemUUID)
    var existedInStore = false
    if (!existedInCache) {
      try {
        existedInStore =
            kotlinx.coroutines.withTimeoutOrNull(1_000L) {
              db.collection(ITEMS_COLLECTION).document(itemUUID).get().await().exists()
            } ?: false
      } catch (e: Exception) {
        // Offline / timeout: treat as not found in store for contract purposes
        existedInStore = false
        Log.w(TAG, "Existence check timed out/offline: ${e.message}")
      }
    }
    if (!existedInCache && !existedInStore) {
      throw Exception("ItemsRepositoryFirestore: Item not found")
    }

    // Optimistically update memory cache immediately (synchronous, instant)
    itemsCache[itemUUID] = newItem
    Log.d(TAG, "Updated item $itemUUID in memory cache")

    // Queue Firestore update with timeout (will sync when online, doesn't block)
    try {
      kotlinx.coroutines.withTimeoutOrNull(2_000L) {
        db.collection(ITEMS_COLLECTION).document(itemUUID).set(newItem).await()
      }
      Log.d(TAG, "Item edited in Firestore (or queued if offline)")
    } catch (e: Exception) {
      Log.w(TAG, "Firestore edit queued (offline): ${e.message}")
    }
  }

  override suspend fun deleteItem(uuid: String) {
    // Optimistically remove from memory cache immediately
    itemsCache.remove(uuid)
    Log.d(TAG, "Removed item $uuid from memory cache")

    // Queue Firestore delete with timeout
    try {
      kotlinx.coroutines.withTimeoutOrNull(2_000L) {
        db.collection(ITEMS_COLLECTION).document(uuid).delete().await()
      }
      Log.d(TAG, "Item deleted from Firestore (or queued if offline)")
    } catch (e: Exception) {
      Log.w(TAG, "Firestore delete queued (offline): ${e.message}")
    }
  }

  override suspend fun deletePostItems(postUuid: String) {
    val ownerId = Firebase.auth.currentUser?.uid ?: throw Exception(NOT_LOGGED_IN_EXCEPTION)

    val snapshot =
        db.collection(ITEMS_COLLECTION)
            .whereArrayContains(POST_ATTRIBUTE_NAME, postUuid)
            .whereEqualTo(OWNER_ATTRIBUTE_NAME, ownerId)
            .get()
            .await()

    for (doc in snapshot.documents) {
      doc.reference.delete().await()
    }
  }

  override suspend fun getFriendItemsForPost(postUuid: String, friendId: String): List<Item> {
    val snapshot =
        db.collection(ITEMS_COLLECTION)
            .whereArrayContains(POST_ATTRIBUTE_NAME, postUuid)
            .whereEqualTo(OWNER_ATTRIBUTE_NAME, friendId)
            .get()
            .await()

    return snapshot.mapNotNull { mapToItem(it) }
  }
}
