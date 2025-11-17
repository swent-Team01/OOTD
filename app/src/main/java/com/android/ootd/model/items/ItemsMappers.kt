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
      val itemUuid = data["itemUuid"] as? String ?: return null

      val imageMap = data["image"] as? Map<*, *> ?: return null
      val imageId = imageMap["imageId"] as? String ?: ""
      val imageUrl = imageMap["imageUrl"] as? String ?: ""
      val image = ImageData(imageId, imageUrl)

      val category = data["category"] as? String ?: return null
      val type = data["type"] as? String ?: return null
      val brand = data["brand"] as? String ?: return null
      val price = (data["price"] as? Number)?.toDouble() ?: return null
      val currency = data["currency"] as? String
      val link = data["link"] as? String ?: return null
      val ownerId = data["ownerId"] as? String ?: return null

      val materialList =
          (data["material"] as? List<*>)?.mapNotNull { entry ->
            (entry as? Map<*, *>)?.let { map ->
              val name = map["name"] as? String ?: return@let null
              val pct = (map["percentage"] as? Number)?.toDouble() ?: return@let null
              Material(name, pct)
            }
          } ?: emptyList()

      val condition = data["condition"] as? String
      val size = data["size"] as? String
      val fitType = data["fitType"] as? String
      val style = data["style"] as? String
      val notes = data["notes"] as? String

      Item(
          itemUuid = itemUuid,
          postUuids = (data["postUuids"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
          image = image,
          category = category,
          type = type,
          brand = brand,
          price = price,
          currency = currency,
          material = materialList,
          link = link,
          ownerId = ownerId,
          condition = condition,
          size = size,
          fitType = fitType,
          style = style,
          notes = notes)
    } catch (e: Exception) {
      null
    }
  }

  fun toMap(item: Item): Map<String, Any?> =
      mapOf(
          "itemUuid" to item.itemUuid,
          "postUuids" to item.postUuids,
          "image" to mapOf("imageId" to item.image.imageId, "imageUrl" to item.image.imageUrl),
          "category" to item.category,
          "type" to item.type,
          "brand" to item.brand,
          "price" to item.price,
          "currency" to item.currency,
          "material" to
              item.material.map {
                it?.let { m -> mapOf("name" to m.name, "percentage" to m.percentage) }
              },
          "link" to item.link,
          "ownerId" to item.ownerId,
          "condition" to item.condition,
          "size" to item.size,
          "fitType" to item.fitType,
          "style" to item.style,
          "notes" to item.notes,
      )
}
