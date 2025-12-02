package com.android.ootd.model.items

import org.junit.Assert.assertEquals
import org.junit.Test

class ItemsMappersTest {

  @Test
  fun toMap_serializesAllFields() {
    val item =
        createItem(
            material =
                listOf(
                    Material(name = "Cotton", percentage = 80.0),
                    Material(name = "Polyester", percentage = 20.0)))

    val map = ItemsMappers.toMap(item)

    val expected =
        mapOf(
            "itemUuid" to "item-1",
            "postUuids" to listOf("post-1"),
            "image" to mapOf("imageId" to "image-1", "imageUrl" to "url-1"),
            "category" to "Tops",
            "type" to "T-Shirt",
            "brand" to "BrandX",
            "price" to 42.0,
            "currency" to "CHF",
            "material" to
                listOf(
                    mapOf("name" to "Cotton", "percentage" to 80.0),
                    mapOf("name" to "Polyester", "percentage" to 20.0)),
            "link" to "https://example.com",
            "ownerId" to "owner-1",
            "condition" to "New",
            "size" to "M",
            "fitType" to "Regular",
            "style" to "Casual",
            "notes" to "Great condition",
            "isPublic" to false)

    assertEquals(expected, map)
  }

  @Test
  fun toMap_includesNullMaterialEntries() {
    val item =
        createItem(
            material = listOf(null, Material(name = "Silk", percentage = 50.0)),
            notes = null,
            style = null)

    val map = ItemsMappers.toMap(item)

    val materials = map["material"] as List<*>
    val expectedMaterial = listOf(null, mapOf("name" to "Silk", "percentage" to 50.0))

    assertEquals(expectedMaterial, materials)
    assertEquals(null, map["notes"])
    assertEquals(null, map["style"])
  }

  private fun createItem(
      material: List<Material?>,
      notes: String? = "Great condition",
      style: String? = "Casual"
  ) =
      Item(
          itemUuid = "item-1",
          postUuids = listOf("post-1"),
          image = ImageData("image-1", "url-1"),
          category = "Tops",
          type = "T-Shirt",
          brand = "BrandX",
          price = 42.0,
          currency = "CHF",
          material = material,
          link = "https://example.com",
          ownerId = "owner-1",
          condition = "New",
          size = "M",
          fitType = "Regular",
          style = style,
          notes = notes)
}
