package com.android.ootd.model.items

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlin.collections.get
import kotlinx.coroutines.tasks.await

const val ITEMS_COLLECTION = "items"
const val OWNER_ATTRIBUTE_NAME = "ownerId"
const val POST_ATTRIBUTE_NAME = "postUuid"

class ItemsRepositoryFirestore(private val db: FirebaseFirestore) : ItemsRepository {

  override fun getNewItemId(): String {
    return db.collection(ITEMS_COLLECTION).document().id
  }

  override suspend fun getAllItems(): List<Item> {
    val ownerId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("ItemsRepositoryFirestore: User not logged in.")
    val snapshot =
        db.collection(ITEMS_COLLECTION).whereEqualTo(OWNER_ATTRIBUTE_NAME, ownerId).get().await()
    return snapshot.mapNotNull { mapToItem(it) }
  }

  override suspend fun getAssociatedItems(postUuid: String): List<Item> {
    val ownerId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("ItemsRepositoryFirestore: User not logged in.")

    val snapshot =
        db.collection(ITEMS_COLLECTION)
            .whereEqualTo(POST_ATTRIBUTE_NAME, postUuid)
            .whereEqualTo(OWNER_ATTRIBUTE_NAME, ownerId)
            .get()
            .await()

    return snapshot.mapNotNull { mapToItem(it) }
  }

  override suspend fun getItemById(uuid: String): Item {
    val doc = db.collection(ITEMS_COLLECTION).document(uuid).get().await()
    return mapToItem(doc) ?: throw Exception("ItemsRepositoryFirestore: Item not found")
  }

  override suspend fun addItem(item: Item) {
    db.collection(ITEMS_COLLECTION).document(item.itemUuid).set(item).await()
  }

  override suspend fun editItem(itemUUID: String, newItem: Item) {
    val doc = db.collection(ITEMS_COLLECTION).document(itemUUID).get().await()
    if (!doc.exists()) throw Exception("Item $itemUUID not found")
    db.collection(ITEMS_COLLECTION).document(itemUUID).set(newItem).await()
  }

  override suspend fun deleteItem(uuid: String) {
    db.collection(ITEMS_COLLECTION).document(uuid).delete().await()
  }

  override suspend fun deletePostItems(postUuid: String) {
    val ownerId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("ItemsRepositoryFirestore: User not logged in.")

    val snapshot =
        db.collection(ITEMS_COLLECTION)
            .whereEqualTo(POST_ATTRIBUTE_NAME, postUuid)
            .whereEqualTo(OWNER_ATTRIBUTE_NAME, ownerId)
            .get()
            .await()

    for (doc in snapshot.documents) {
      doc.reference.delete().await()
    }
  }
}

private fun mapToItem(doc: DocumentSnapshot): Item? {
  return try {
    val uuid = doc.getString("itemUuid") ?: return null
    val postUuid = doc.getString("postUuid") ?: return null
    val imageMap = doc["image"] as? Map<*, *> ?: return null
    val imageUri =
        ImageData(
            imageId = imageMap["imageId"] as? String ?: "",
            imageUrl = imageMap["imageUrl"] as? String ?: "",
        )
    val category = doc.getString("category") ?: return null
    val type = doc.getString("type") ?: return null
    val brand = doc.getString("brand") ?: return null
    val price = doc.getDouble("price") ?: return null
    val link = doc.getString("link") ?: return null
    val ownerId = doc.getString("ownerId") ?: return null
    val materialList = doc.get("material") as? List<*>
    val material =
        materialList?.mapNotNull { item ->
          (item as? Map<*, *>)?.let {
            Material(
                name = it["name"] as? String ?: "",
                percentage = (it["percentage"] as? Number)?.toDouble() ?: 0.0)
          }
        } ?: emptyList()

    Item(
        itemUuid = uuid,
        postUuid = postUuid,
        image = imageUri,
        category = category,
        type = type,
        brand = brand,
        price = price,
        material = material,
        link = link,
        ownerId = ownerId)
  } catch (e: Exception) {
    Log.e("ItemsRepositoryFirestore", "Error converting document ${doc.id} to Item", e)
    null
  }
}
