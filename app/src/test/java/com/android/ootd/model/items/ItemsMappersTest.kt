package com.android.ootd.model.items

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import org.junit.Test

class ItemsMappersTest {

  private fun baseMap(overrides: Map<String, Any?> = emptyMap()): Map<String, Any?> {
    val map =
        mutableMapOf<String, Any?>(
            "itemUuid" to "completeItem",
            "image" to mapOf("imageId" to "img123", "imageUrl" to "https://example.com/img.jpg"),
            "category" to "clothes",
            "type" to "jacket",
            "brand" to "Nike",
            "price" to 99.99,
            "link" to "https://example.com/item",
            "material" to
                listOf(
                    mapOf("name" to "Cotton", "percentage" to 70.0),
                    mapOf("name" to "Polyester", "percentage" to 30.0)),
            "ownerId" to "owner")
    map.putAll(overrides)
    return map
  }

  @Test
  fun parseCompleteValidDataReturnsItem() {
    val item = requireNotNull(ItemsMappers.parseItem(baseMap()))
    assertEquals("completeItem", item.itemUuid)
    assertEquals("img123", item.image.imageId)
    assertEquals("https://example.com/img.jpg", item.image.imageUrl)
    assertEquals("clothes", item.category)
    assertEquals("jacket", item.type)
    assertEquals("Nike", item.brand)
    assertEquals(99.99, item.price)
    assertEquals("https://example.com/item", item.link)
    assertEquals(2, item.material.size)
  }

  @Test
  fun missingRequiredFieldsOrInvalidTypesReturnNull() {
    val keysToRemove =
        listOf("itemUuid", "image", "category", "type", "brand", "price", "link", "ownerId")

    // Each required field missing -> null
    keysToRemove.forEach { missing ->
      val map = baseMap().toMutableMap()
      map.remove(missing)
      val parsed = ItemsMappers.parseItem(map)
      assertTrue("Expected null when missing $missing", parsed == null)
    }

    // Invalid price type
    val invalidPrice = baseMap(mapOf("price" to "notANumber"))
    assertNull(ItemsMappers.parseItem(invalidPrice))

    // Exception scenario: image is list instead of map
    val badImage = baseMap(mapOf("image" to listOf("notAMap")))
    assertNull(ItemsMappers.parseItem(badImage))
  }

  @Test
  fun missingMaterialDefaultsToEmptyAndNullsFiltered() {
    // Missing material -> empty list
    val noMaterial = baseMap().toMutableMap().apply { remove("material") }
    val itemNoMat = requireNotNull(ItemsMappers.parseItem(noMaterial))
    assertEquals(0, itemNoMat.material.size)

    // Material with null entries -> nulls filtered
    val withNulls =
        baseMap(
            mapOf(
                "material" to
                    listOf(
                        mapOf("name" to "Cotton", "percentage" to 70.0),
                        null,
                        mapOf("name" to "Polyester", "percentage" to 30.0))))
    val itemFiltered = requireNotNull(ItemsMappers.parseItem(withNulls))
    assertEquals(2, itemFiltered.material.size)
    assertEquals("Cotton", itemFiltered.material[0]?.name)
    assertEquals("Polyester", itemFiltered.material[1]?.name)
  }

  @Test
  fun nonStringImageFieldsAreSanitized() {
    val invalidImageId = baseMap(mapOf("image" to mapOf("imageId" to 123, "imageUrl" to "url")))
    val parsed1 = requireNotNull(ItemsMappers.parseItem(invalidImageId))
    assertEquals("", parsed1.image.imageId)
    assertEquals("url", parsed1.image.imageUrl)

    val invalidImageUrl = baseMap(mapOf("image" to mapOf("imageId" to "id", "imageUrl" to false)))
    val parsed2 = requireNotNull(ItemsMappers.parseItem(invalidImageUrl))
    assertEquals("id", parsed2.image.imageId)
    assertEquals("", parsed2.image.imageUrl)
  }

  @Test
  fun parseIncludesCurrencyWhenPresent() {
    val withCurrency = baseMap(mapOf("currency" to "EUR"))
    val item = requireNotNull(ItemsMappers.parseItem(withCurrency))
    assertEquals("EUR", item.currency)

    val withoutCurrency = baseMap().toMutableMap().apply { remove("currency") }
    val item2 = requireNotNull(ItemsMappers.parseItem(withoutCurrency))
    assertNull(item2.currency)
  }
}
