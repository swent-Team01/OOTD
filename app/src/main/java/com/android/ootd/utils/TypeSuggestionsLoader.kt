package com.android.ootd.utils

import android.content.Context
import com.android.ootd.R

/**
 * Utility object to load type suggestions from .yml configuration file. To be used in the Add &
 * Edit Screen.
 *
 * The suggestions are loaded from res/raw/type_suggestions.yml and cached for performance.
 */
object TypeSuggestionsLoader {

  private var cachedSuggestions: Map<String, List<String>>? = null

  /**
   * Loads type suggestions from the .yml file.
   *
   * @param context Android context to access resources
   * @return Map of category names to their type suggestions
   */
  fun loadTypeSuggestions(context: Context): Map<String, List<String>> {

    // Return cached version if it exists
    cachedSuggestions?.let {
      return it
    }

    val suggestions = mutableMapOf<String, List<String>>()

    try {
      val inputStream = context.resources.openRawResource(R.raw.type_suggestions)
      val yamlContent = inputStream.bufferedReader().use { it.readText() }

      var currentCategory: String? = null
      val currentItems = mutableListOf<String>()

      yamlContent.lines().forEach { line ->
        val trimmedLine = line.trim()

        when {
          // Empty line or comment -> skip
          trimmedLine.isEmpty() || trimmedLine.startsWith("#") -> {}

          // Category line
          trimmedLine.endsWith(":") -> {
            // Save previous category if exists
            currentCategory?.let { category ->
              suggestions[category] = currentItems.toList()
              currentItems.clear()
            }
            // Start new category
            currentCategory = trimmedLine.removeSuffix(":").trim()
          }

          // Item line
          trimmedLine.startsWith("-") -> {
            val item = trimmedLine.removePrefix("-").trim()
            if (item.isNotEmpty()) {
              currentItems.add(item)
            }
          }
        }
      }

      // Save last category
      currentCategory?.let { category -> suggestions[category] = currentItems.toList() }

      cachedSuggestions = suggestions
    } catch (e: Exception) {
      // If .yml loading fails, return default suggestions
      return getDefaultSuggestions()
    }

    return suggestions
  }

  /**
   * Fallback method providing default suggestions if .yml loading fails. Same lists as the .yml as
   * of the 07 october 2025
   */
  private fun getDefaultSuggestions(): Map<String, List<String>> {
    return mapOf(
        "Clothing" to
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
                "Jumpsuit"),
        "Shoes" to
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
                "Sports Shoes"),
        "Accessories" to
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
                "Cap"),
        "Bags" to
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
                "Wallet"))
  }

  /** Clears the cached suggestions. */
  fun clearCache() {
    cachedSuggestions = null
  }
}
