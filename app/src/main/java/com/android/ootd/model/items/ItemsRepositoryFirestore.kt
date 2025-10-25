package com.android.ootd.model.items

import android.util.Log
import androidx.core.net.toUri
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlin.collections.get
import kotlinx.coroutines.tasks.await

const val ITEMS_COLLECTION = "items"

class ItemsRepositoryFirestore(private val db: FirebaseFirestore) : ItemsRepository {

  private val ownerAttributeName = "ownerId"

  override fun getNewItemId(): String {
    return db.collection(ITEMS_COLLECTION).document().id
  }

  override suspend fun getAllItems(): List<Item> {
    val ownerId =
        Firebase.auth.currentUser?.uid
            ?: throw Exception("ToDosRepositoryFirestore: User not logged in.")
    val snapshot =
        db.collection(ITEMS_COLLECTION).whereEqualTo(ownerAttributeName, ownerId).get().await()
    return snapshot.mapNotNull { mapToItem(it) }
  }

  override suspend fun getItemById(uuid: String): Item {
    val doc = db.collection(ITEMS_COLLECTION).document(uuid).get().await()
    return mapToItem(doc) ?: throw Exception("ItemsRepositoryFirestore: Item not found")
  }

  override suspend fun addItem(item: Item) {
    db.collection(ITEMS_COLLECTION).document(item.uuid).set(item).await()
  }

  override suspend fun editItem(itemUUID: String, newItem: Item) {
    val doc = db.collection(ITEMS_COLLECTION).document(itemUUID).get().await()
    if (!doc.exists()) throw Exception("Item $itemUUID not found")
    db.collection(ITEMS_COLLECTION).document(itemUUID).set(newItem).await()
  }

  override suspend fun deleteItem(uuid: String) {
    db.collection(ITEMS_COLLECTION).document(uuid).delete().await()
  }
}

private fun mapToItem(doc: DocumentSnapshot): Item? {
  return try {
    val uuid = doc.getString("uuid") ?: return null
    val image = doc.getString("image") ?: return null
    val imageUri = image.toUri()
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
        uuid = uuid,
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
