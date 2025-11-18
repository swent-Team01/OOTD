package com.android.ootd.model.items

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

    // Batch of 10 item for each querries
    val items = mutableListOf<Item>()
    uuids.chunked(10).forEach { batch ->
      val snapshot =
          db.collection(ITEMS_COLLECTION)
              .whereIn("itemUuid", batch)
              .whereEqualTo(OWNER_ATTRIBUTE_NAME, ownerId)
              .get()
              .await()
      items.addAll(snapshot.mapNotNull { mapToItem(it) })
    }
    return items
  }

  private fun mapToItem(doc: DocumentSnapshot): Item? {
    return try {
      val data = doc.data ?: return null
      ItemsMappers.parseItem(data)
    } catch (e: Exception) {
      null
    }
  }

  override suspend fun addItem(item: Item) {
    val map = ItemsMappers.toMap(item)
    db.collection(ITEMS_COLLECTION).document(item.itemUuid).set(map).await()
  }

  override suspend fun editItem(itemUUID: String, newItem: Item) {
    val docRef = db.collection(ITEMS_COLLECTION).document(itemUUID)
    val existing = docRef.get().await()
    if (!existing.exists()) throw Exception("ItemsRepositoryFirestore: Item not found")
    val map = ItemsMappers.toMap(newItem)
    docRef.set(map).await()
  }

  override suspend fun deleteItem(uuid: String) {
    db.collection(ITEMS_COLLECTION).document(uuid).delete().await()
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
