package com.android.ootd.model.items

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

const val ITEMS_COLLECTION = "items"
const val OWNER_ATTRIBUTE_NAME = "ownerId"
const val POST_ATTRIBUTE_NAME = "postUuids"
const val NOT_LOGGED_IN_EXCEPTION = "ItemsRepositoryFirestore: User not logged in."

class ItemsRepositoryFirestore(private val db: FirebaseFirestore) : ItemsRepository {

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
    val doc = db.collection(ITEMS_COLLECTION).document(uuid).get().await()
    return mapToItem(doc) ?: throw Exception("ItemsRepositoryFirestore: Item not found")
  }

  override suspend fun getItemsByIds(uuids: List<String>): List<Item> {
    if (uuids.isEmpty()) return emptyList()

    val ownerId = Firebase.auth.currentUser?.uid ?: throw Exception(NOT_LOGGED_IN_EXCEPTION)
    val items = mutableListOf<Item>()

    // Fetch items from Firestore in chunks
    uuids.chunked(10).forEach { batch ->
      try {
        val snapshot =
            db.collection(ITEMS_COLLECTION)
                .whereIn("itemUuid", batch)
                .whereEqualTo(OWNER_ATTRIBUTE_NAME, ownerId)
                .get()
                .await()
        items.addAll(snapshot.mapNotNull { mapToItem(it) })
      } catch (e: Exception) {}
    }

    return items
  }

  override suspend fun getItemsByIdsAcrossOwners(uuids: List<String>): List<Item> = coroutineScope {
    if (uuids.isEmpty()) return@coroutineScope emptyList()

    uuids
        .map { uuid ->
          async {
            try {
              val doc = db.collection(ITEMS_COLLECTION).document(uuid).get().await()
              mapToItem(doc)
            } catch (e: Exception) {
              null
            }
          }
        }
        .awaitAll()
        .filterNotNull()
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
    try {
      withTimeout(2000L) {
        db.collection(ITEMS_COLLECTION).document(item.itemUuid).set(item).await()
      }
    } catch (e: TimeoutCancellationException) {} catch (e: Exception) {
      Log.e("ItemsRepositoryFirestore", "Error adding item: ${e.message}", e)
      throw e
    }
  }

  override suspend fun editItem(itemUUID: String, newItem: Item) {
    try {
      // Check existence first (optional, but good for consistency)
      // We use a short timeout for existence check
      try {
        withTimeout(1000L) {
          val doc = db.collection(ITEMS_COLLECTION).document(itemUUID).get().await()
          if (!doc.exists()) {
            throw Exception("ItemsRepositoryFirestore: Item not found")
          }
        }
      } catch (e: TimeoutCancellationException) {}

      withTimeout(2000L) { db.collection(ITEMS_COLLECTION).document(itemUUID).set(newItem).await() }
    } catch (e: TimeoutCancellationException) {} catch (e: Exception) {
      Log.e("ItemsRepositoryFirestore", "Error editing item: ${e.message}", e)
      throw e
    }
  }

  override suspend fun deleteItem(uuid: String) {
    try {
      withTimeout(2000L) { db.collection(ITEMS_COLLECTION).document(uuid).delete().await() }
    } catch (e: TimeoutCancellationException) {} catch (e: Exception) {
      Log.e("ItemsRepositoryFirestore", "Error deleting item: ${e.message}", e)
      throw e
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
