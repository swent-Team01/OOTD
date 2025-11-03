package com.android.ootd.model.items

/** Pure mapping utilities for Item to enable fast JVM unit testing. */
object ItemsMappers {
  /**
   * Parse an Item from a generic Map (e.g., Firestore document data). Required fields: itemUuid,
   * image (map), category, type, brand, price (number), link, ownerId.
   * - image.imageId and image.imageUrl are sanitized to empty strings if not strings.
   * - material list is optional; entries not maps are skipped; fields sanitized.
   */
  fun parseItem(data: Map<String, Any?>): Item? {
    return try {
      val uuid = data["itemUuid"] as? String ?: return null

      val imageMap = data["image"] as? Map<*, *> ?: return null
      val image =
          ImageData(
              imageId = imageMap["imageId"] as? String ?: "",
              imageUrl = imageMap["imageUrl"] as? String ?: "")

      val category = data["category"] as? String ?: return null
      val type = data["type"] as? String ?: return null
      val brand = data["brand"] as? String ?: return null
      val price = (data["price"] as? Number)?.toDouble() ?: return null
      val link = data["link"] as? String ?: return null
      val ownerId = data["ownerId"] as? String ?: return null
      val postUuids = (data["postUuids"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

      val materialList = data["material"] as? List<*>
      val material =
          materialList?.mapNotNull { entry ->
            (entry as? Map<*, *>)?.let {
              Material(
                  name = it["name"] as? String ?: "",
                  percentage = (it["percentage"] as? Number)?.toDouble() ?: 0.0)
            }
          } ?: emptyList()

      Item(
          itemUuid = uuid,
          postUuids = postUuids,
          image = image,
          category = category,
          type = type,
          brand = brand,
          price = price,
          material = material,
          link = link,
          ownerId = ownerId)
    } catch (_: Exception) {
      null
    }
  }
}
