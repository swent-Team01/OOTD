package com.android.ootd.model.items

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.collections.get
import kotlinx.coroutines.tasks.await

const val ITEMS_COLLECTION = "items"

class ItemsRepositoryFirestore(private val db: FirebaseFirestore) : ItemsRepository {
  override fun getNewItemId(): String {
    return db.collection(ITEMS_COLLECTION).document().id
  }

  override suspend fun getAllItems(): List<Item> {
    val snapshot = db.collection(ITEMS_COLLECTION).get().await()
    return snapshot.mapNotNull { mapToItem(it) }
  }

  override suspend fun getItemById(uuid: String): Item {
    val doc = db.collection(ITEMS_COLLECTION).document(uuid).get().await()
    return mapToItem(doc) ?: throw Exception("ItemsRepositoryFirestore: Item not found")
  }

  override suspend fun addItem(item: Item) {
    Log.d("AddItemsVM", "Repository adding item: $item")
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
}

private fun mapToItem(doc: DocumentSnapshot): Item? {
  return try {
    val uuid = doc.getString("itemUuid") ?: return null
    val imageMap = doc.get("image") as? Map<*, *> ?: return null
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
        image = imageUri,
        category = category,
        type = type,
        brand = brand,
        price = price,
        material = material,
        link = link,
    )
  } catch (e: Exception) {
    Log.e("ItemsRepositoryFirestore", "Error converting document ${doc.id} to Item", e)
    null
  }
}
