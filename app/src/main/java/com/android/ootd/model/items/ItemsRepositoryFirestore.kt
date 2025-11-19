package com.android.ootd.model.items

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

const val ITEMS_COLLECTION = "items"
const val OWNER_ATTRIBUTE_NAME = "ownerId"
const val POST_ATTRIBUTE_NAME = "postUuids"
const val NOT_LOGGED_IN_EXCEPTION = "ItemsRepositoryFirestore: User not logged in."

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
    if (uuids.isEmpty()) return emptyList()

    val ownerId = Firebase.auth.currentUser?.uid ?: throw Exception(NOT_LOGGED_IN_EXCEPTION)

    // First check memory cache for all requested items
    val cachedItems = uuids.mapNotNull { itemsCache[it] }
    if (cachedItems.size == uuids.size) {
      Log.d("ItemsRepositoryFirestore", "Returning all ${uuids.size} items from memory cache")
      return cachedItems
    }

    // If some items in cache, still return them even if not all
    if (cachedItems.isNotEmpty()) {
      Log.d(
          "ItemsRepositoryFirestore",
          "Found ${cachedItems.size}/${uuids.size} items in cache, fetching rest")
    }

    // Fetch missing items from Firestore with timeout
    val items = mutableListOf<Item>()
    items.addAll(cachedItems) // Start with cached items

    val missingUuids = uuids.filter { !itemsCache.containsKey(it) }
    if (missingUuids.isNotEmpty()) {
      try {
        kotlinx.coroutines.withTimeoutOrNull(2_000L) {
          missingUuids.chunked(10).forEach { batch ->
            val snapshot =
                db.collection(ITEMS_COLLECTION)
                    .whereIn("itemUuid", batch)
                    .whereEqualTo(OWNER_ATTRIBUTE_NAME, ownerId)
                    .get()
                    .await()
            val fetchedItems = snapshot.mapNotNull { mapToItem(it) }
            // Update cache with fetched items
            fetchedItems.forEach { itemsCache[it.itemUuid] = it }
            items.addAll(fetchedItems)
          }
        }
        Log.d(
            "ItemsRepositoryFirestore",
            "Fetched ${items.size - cachedItems.size} items from Firestore")
      } catch (e: Exception) {
        Log.w("ItemsRepositoryFirestore", "Failed to fetch missing items (offline): ${e.message}")
        // Return whatever we have in cache
      }
    }

    return items
  }

  private fun mapToItem(doc: DocumentSnapshot): Item? {
    return try {
      val data = doc.data ?: return null
      ItemsMappers.parseItem(data)
    } catch (_: Exception) {
      null
    }
  }

  override suspend fun addItem(item: Item) {
    // Optimistically update memory cache immediately
    itemsCache[item.itemUuid] = item
    Log.d("ItemsRepositoryFirestore", "Added item ${item.itemUuid} to memory cache")

    // Queue Firestore update with timeout
    try {
      kotlinx.coroutines.withTimeoutOrNull(2_000L) {
        db.collection(ITEMS_COLLECTION).document(item.itemUuid).set(item).await()
      }
      Log.d("ItemsRepositoryFirestore", "Item added to Firestore (or queued if offline)")
    } catch (e: Exception) {
      Log.w("ItemsRepositoryFirestore", "Firestore add queued (offline): ${e.message}")
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
        Log.w("ItemsRepositoryFirestore", "Existence check timed out/offline: ${e.message}")
      }
    }
    if (!existedInCache && !existedInStore) {
      throw Exception("ItemsRepositoryFirestore: Item not found")
    }

    // Optimistically update memory cache immediately (synchronous, instant)
    itemsCache[itemUUID] = newItem
    Log.d("ItemsRepositoryFirestore", "Updated item $itemUUID in memory cache")

    // Queue Firestore update with timeout (will sync when online, doesn't block)
    try {
      kotlinx.coroutines.withTimeoutOrNull(2_000L) {
        db.collection(ITEMS_COLLECTION).document(itemUUID).set(newItem).await()
      }
      Log.d("ItemsRepositoryFirestore", "Item edited in Firestore (or queued if offline)")
    } catch (e: Exception) {
      Log.w("ItemsRepositoryFirestore", "Firestore edit queued (offline): ${e.message}")
    }
  }

  override suspend fun deleteItem(uuid: String) {
    // Optimistically remove from memory cache immediately
    itemsCache.remove(uuid)
    Log.d("ItemsRepositoryFirestore", "Removed item $uuid from memory cache")

    // Queue Firestore delete with timeout
    try {
      kotlinx.coroutines.withTimeoutOrNull(2_000L) {
        db.collection(ITEMS_COLLECTION).document(uuid).delete().await()
      }
      Log.d("ItemsRepositoryFirestore", "Item deleted from Firestore (or queued if offline)")
    } catch (e: Exception) {
      Log.w("ItemsRepositoryFirestore", "Firestore delete queued (offline): ${e.message}")
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
