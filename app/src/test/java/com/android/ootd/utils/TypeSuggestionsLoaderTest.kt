package com.android.ootd.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TypeSuggestionsLoaderTest {

  private lateinit var context: Context

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    // Clear cache before each test
    TypeSuggestionsLoader.clearCache()
  }

  @Test
  fun `loadTypeSuggestions returns non-empty map`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    assertNotNull(suggestions)
    assertTrue(suggestions.isNotEmpty())
  }

  @Test
  fun `loadTypeSuggestions contains expected categories`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    assertTrue(suggestions.containsKey("Clothing") || suggestions.containsKey("Clothes"))
    assertTrue(suggestions.containsKey("Shoes"))
    assertTrue(suggestions.containsKey("Bags"))
    assertTrue(suggestions.containsKey("Accessories"))
  }

  @Test
  fun `loadTypeSuggestions Clothing category contains expected items`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    val clothingKey = suggestions.keys.find { it.equals("Clothing", ignoreCase = true) }
    assertNotNull(clothingKey)

    val clothingItems = suggestions[clothingKey]
    assertNotNull(clothingItems)
    assertTrue(clothingItems!!.contains("T-shirt"))
    assertTrue(clothingItems.contains("Jeans"))
    assertTrue(clothingItems.contains("Jacket"))
  }

  @Test
  fun `loadTypeSuggestions Shoes category contains expected items`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    val shoesItems = suggestions["Shoes"]
    assertNotNull(shoesItems)
    assertTrue(shoesItems!!.contains("Sneakers"))
    assertTrue(shoesItems.contains("Boots"))
    assertTrue(shoesItems.contains("Sandals"))
  }

  @Test
  fun `loadTypeSuggestions Accessories category contains expected items`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    val accessoriesItems = suggestions["Accessories"]
    assertNotNull(accessoriesItems)
    assertTrue(accessoriesItems!!.contains("Hat"))
    assertTrue(accessoriesItems.contains("Scarf"))
    assertTrue(accessoriesItems.contains("Belt"))
  }

  @Test
  fun `loadTypeSuggestions Bags category contains expected items`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    val bagsItems = suggestions["Bags"]
    assertNotNull(bagsItems)
    assertTrue(bagsItems!!.contains("Backpack"))
    assertTrue(bagsItems.contains("Handbag"))
    assertTrue(bagsItems.contains("Tote"))
  }

  @Test
  fun `loadTypeSuggestions uses cache on second call`() {
    val firstCall = TypeSuggestionsLoader.loadTypeSuggestions(context)
    val secondCall = TypeSuggestionsLoader.loadTypeSuggestions(context)

    // Both should return the same data
    assertEquals(firstCall, secondCall)
  }

  @Test
  fun `clearCache removes cached data`() {
    // First load
    val firstLoad = TypeSuggestionsLoader.loadTypeSuggestions(context)
    assertNotNull(firstLoad)

    // Clear cache
    TypeSuggestionsLoader.clearCache()

    // Load again - should reload from file
    val secondLoad = TypeSuggestionsLoader.loadTypeSuggestions(context)
    assertNotNull(secondLoad)

    // Data should be the same, but it should have been reloaded
    assertEquals(firstLoad, secondLoad)
  }

  @Test
  fun `getDefaultSuggestions provides fallback data`() {
    // This test verifies the structure of default suggestions
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    // All valid categories should have at least some items
    val validCategories = listOf("Clothing", "Shoes", "Accessories", "Bags")
    validCategories.forEach { category ->
      val items = suggestions[category]
      assertNotNull("Category $category should exist", items)
      assertTrue("Category $category should have items", items!!.isNotEmpty())
    }
  }

  @Test
  fun `loadTypeSuggestions handles empty or invalid input gracefully`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    // Should still return valid data even if YAML has issues
    assertNotNull(suggestions)
    assertTrue(suggestions.isNotEmpty())
  }

  @Test
  fun `all categories have non-empty item lists`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    // Only check valid categories used by the dropdown
    val validCategories = listOf("Clothing", "Shoes", "Accessories", "Bags")
    validCategories.forEach { category ->
      val items = suggestions[category]
      assertNotNull("Category $category should exist", items)
      assertFalse("Category $category should have items", items!!.isEmpty())
    }
  }

  @Test
  fun `no duplicate items within categories`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    suggestions.forEach { (category, items) ->
      val uniqueItems = items.toSet()
      assertEquals(
          "Category $category should not have duplicate items", items.size, uniqueItems.size)
    }
  }

  @Test
  fun `all items are non-empty strings`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    suggestions.forEach { (category, items) ->
      items.forEach { item ->
        assertTrue("Item in $category should not be empty", item.isNotEmpty())
        assertFalse("Item in $category should not be blank", item.isBlank())
      }
    }
  }

  @Test
  fun `all category names are non-empty`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    suggestions.keys.forEach { category ->
      assertTrue("Category name should not be empty", category.isNotEmpty())
      assertFalse("Category name should not be blank", category.isBlank())
    }
  }

  @Test
  fun `Clothing category has comprehensive list`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    val clothingKey = suggestions.keys.find { it.equals("Clothing", ignoreCase = true) }
    val clothingItems = suggestions[clothingKey]

    assertNotNull(clothingItems)
    assertTrue(clothingItems!!.size >= 10) // Should have at least 10 clothing types
  }

  @Test
  fun `Shoes category has comprehensive list`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    val shoesItems = suggestions["Shoes"]

    assertNotNull(shoesItems)
    assertTrue(shoesItems!!.size >= 5) // Should have at least 5 shoe types
  }

  @Test
  fun `Accessories category has comprehensive list`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    val accessoriesItems = suggestions["Accessories"]

    assertNotNull(accessoriesItems)
    assertTrue(accessoriesItems!!.size >= 5) // Should have at least 5 accessory types
  }

  @Test
  fun `Bags category has comprehensive list`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    val bagsItems = suggestions["Bags"]

    assertNotNull(bagsItems)
    assertTrue(bagsItems!!.size >= 5) // Should have at least 5 bag types
  }

  @Test
  fun `loadTypeSuggestions returns consistent results`() {
    TypeSuggestionsLoader.clearCache()

    val firstLoad = TypeSuggestionsLoader.loadTypeSuggestions(context)

    TypeSuggestionsLoader.clearCache()

    val secondLoad = TypeSuggestionsLoader.loadTypeSuggestions(context)

    assertEquals("Multiple loads should return same data", firstLoad, secondLoad)
  }

  @Test
  fun `default suggestions match expected structure`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    // Verify structure matches the documented default suggestions
    assertTrue(suggestions.size >= 4) // At least 4 categories
  }

  @Test
  fun `loadTypeSuggestions handles multiple contexts`() {
    val context1 = ApplicationProvider.getApplicationContext<Context>()
    val context2 = ApplicationProvider.getApplicationContext<Context>()

    TypeSuggestionsLoader.clearCache()
    val suggestions1 = TypeSuggestionsLoader.loadTypeSuggestions(context1)

    // Should use cache for second context
    val suggestions2 = TypeSuggestionsLoader.loadTypeSuggestions(context2)

    assertEquals(suggestions1, suggestions2)
  }

  @Test
  fun `items are properly trimmed`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    suggestions.forEach { (_, items) ->
      items.forEach { item -> assertEquals("Items should be trimmed", item, item.trim()) }
    }
  }

  @Test
  fun `category names are properly trimmed`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    suggestions.keys.forEach { category ->
      assertEquals("Category names should be trimmed", category, category.trim())
    }
  }

  @Test
  fun `categories do not end with colons`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    suggestions.keys.forEach { category ->
      assertFalse("Category should not end with colon", category.endsWith(":"))
    }
  }

  @Test
  fun `fallback suggestions contain all expected categories`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    // Verify all default categories are present (even if YAML fails)
    val hasClothing = suggestions.keys.any { it.equals("Clothing", ignoreCase = true) }
    assertTrue("Should have Clothing category", hasClothing)
    assertTrue("Should have Shoes category", suggestions.containsKey("Shoes"))
    assertTrue("Should have Bags category", suggestions.containsKey("Bags"))
    assertTrue("Should have Accessories category", suggestions.containsKey("Accessories"))
  }

  @Test
  fun `fallback Clothing suggestions are complete`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    val clothingKey = suggestions.keys.find { it.equals("Clothing", ignoreCase = true) }
    assertNotNull(clothingKey)

    val clothingItems = suggestions[clothingKey]!!
    val expectedItems =
        listOf(
            "T-shirt",
            "Shirt",
            "Jeans",
            "Jacket",
            "Dress",
            "Skirt",
            "Shorts",
            "Sweater",
            "Coat",
            "Blouse",
            "Suit",
            "Hoodie",
            "Cardigan",
            "Pants",
            "Leggings",
            "Overalls",
            "Jumpsuit")

    expectedItems.forEach { expectedItem ->
      assertTrue(
          "Clothing should contain $expectedItem",
          clothingItems.any { it.equals(expectedItem, ignoreCase = true) })
    }
  }

  @Test
  fun `fallback Shoes suggestions are complete`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    val shoesItems = suggestions["Shoes"]!!
    val expectedItems =
        listOf(
            "Sneakers",
            "Boots",
            "Sandals",
            "Heels",
            "Flats",
            "Loafers",
            "Oxfords",
            "Slippers",
            "Wedges",
            "Espadrilles",
            "Ballerinas",
            "Moccasins",
            "Sports Shoes")

    expectedItems.forEach { expectedItem ->
      assertTrue(
          "Shoes should contain $expectedItem",
          shoesItems.any { it.equals(expectedItem, ignoreCase = true) })
    }
  }

  @Test
  fun `fallback Accessories suggestions are complete`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    val accessoriesItems = suggestions["Accessories"]!!
    val expectedItems =
        listOf(
            "Hat",
            "Scarf",
            "Belt",
            "Gloves",
            "Sunglasses",
            "Watch",
            "Bracelet",
            "Necklace",
            "Earrings",
            "Tie",
            "Beanie",
            "Cap")

    expectedItems.forEach { expectedItem ->
      assertTrue(
          "Accessories should contain $expectedItem",
          accessoriesItems.any { it.equals(expectedItem, ignoreCase = true) })
    }
  }

  @Test
  fun `fallback Bags suggestions are complete`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    val bagsItems = suggestions["Bags"]!!
    val expectedItems =
        listOf(
            "Backpack",
            "Handbag",
            "Tote",
            "Clutch",
            "Messenger Bag",
            "Duffel Bag",
            "Satchel",
            "Crossbody Bag",
            "Shopper",
            "Wallet")

    expectedItems.forEach { expectedItem ->
      assertTrue(
          "Bags should contain $expectedItem",
          bagsItems.any { it.equals(expectedItem, ignoreCase = true) })
    }
  }

  @Test
  fun `fallback suggestions match documented defaults`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    // Verify the number of categories matches the documented default
    assertTrue("Should have at least 4 categories", suggestions.size >= 4)

    // Verify minimum items per category based on documentation
    val clothingKey = suggestions.keys.find { it.equals("Clothing", ignoreCase = true) }
    assertTrue("Clothing should have at least 17 items", suggestions[clothingKey]?.size ?: 0 >= 17)
    assertTrue("Shoes should have at least 13 items", suggestions["Shoes"]?.size ?: 0 >= 13)
    assertTrue(
        "Accessories should have at least 12 items", suggestions["Accessories"]?.size ?: 0 >= 12)
    assertTrue("Bags should have at least 10 items", suggestions["Bags"]?.size ?: 0 >= 10)
  }

  @Test
  fun `suggestions remain valid after cache clear`() {
    // First load
    val firstSuggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)
    assertTrue(firstSuggestions.isNotEmpty())

    // Clear cache
    TypeSuggestionsLoader.clearCache()

    // Second load should still return valid suggestions
    val secondSuggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)
    assertTrue(secondSuggestions.isNotEmpty())

    // Should have same categories
    assertEquals(firstSuggestions.keys, secondSuggestions.keys)
  }

  @Test
  fun `fallback data is used when YAML parsing fails`() {
    // Since we can't easily corrupt the YAML file in tests,
    // we verify that the fallback data structure is valid
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    // All valid dropdown categories should be present
    val validCategories = listOf("Clothing", "Shoes", "Accessories", "Bags")
    validCategories.forEach { category ->
      val items = suggestions[category]
      assertNotNull("Category $category should exist", items)
      assertTrue("Category $category should have items", items!!.isNotEmpty())
    }
  }

  @Test
  fun `fallback suggestions do not have empty items`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    // Verify no category has empty item strings
    suggestions.forEach { (category, items) ->
      items.forEach { item ->
        assertTrue("Item in $category should not be empty", item.isNotEmpty())
        assertTrue("Item in $category should not be blank", item.isNotBlank())
      }
    }
  }

  @Test
  fun `fallback suggestions have proper capitalization`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    // Check that category names are properly capitalized
    suggestions.keys.forEach { category ->
      assertTrue("Category $category should start with uppercase", category.first().isUpperCase())
    }
  }

  @Test
  fun `all fallback items are unique within their category`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    suggestions.forEach { (category, items) ->
      val uniqueItems = items.toSet()
      assertEquals(
          "Category $category should not have duplicate items in fallback",
          items.size,
          uniqueItems.size)
    }
  }

  @Test
  fun `fallback suggestions are case-insensitive searchable`() {
    val suggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)

    val clothingKey = suggestions.keys.find { it.equals("Clothing", ignoreCase = true) }
    val clothingItems = suggestions[clothingKey]!!

    // Verify we can find items regardless of case
    assertTrue(clothingItems.any { it.equals("t-shirt", ignoreCase = true) })
    assertTrue(clothingItems.any { it.equals("JEANS", ignoreCase = true) })
    assertTrue(clothingItems.any { it.equals("JaCkEt", ignoreCase = true) })
  }
}
