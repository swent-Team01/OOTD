package com.android.ootd.utils

/**
 * Utility object for category management throughout the app.
 *
 * Provides a single source of truth for valid categories. Since categories are now selected via
 * dropdown menus, all category values in the database will be consistent and match exactly.
 */
object CategoryNormalizer {

  /**
   * List of valid categories in their canonical form.
   *
   * This is the definitive list used for:
   * - Dropdown menu options in Add/Edit screens
   * - Category sorting order in inventory
   * - Category grouping in inventory display
   */
  val VALID_CATEGORIES =
      listOf(
          "Clothing",
          "Shoes",
          "Accessories",
          "Jewelry",
          "Bags",
          "Sportswear",
          "Underwear",
          "Others")

  /**
   * Checks if a category name is valid.
   *
   * @param category The category name to check
   * @return true if the category matches one of the valid categories
   */
  fun isValid(category: String): Boolean {
    return category in VALID_CATEGORIES
  }

  /**
   * Gets the sort order index for a category. Used for sorting items by category in the inventory.
   *
   * Categories are displayed in this order: Clothing → Shoes → Accessories → Bags
   *
   * @param category The category name
   * @return The sort index (0-3 for valid categories, Int.MAX_VALUE for unknown)
   */
  fun getSortOrder(category: String): Int {
    return VALID_CATEGORIES.indexOf(category).takeIf { it >= 0 } ?: Int.MAX_VALUE
  }
}
