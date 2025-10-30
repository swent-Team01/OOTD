package com.android.ootd.ui.post

import android.content.Context
import androidx.lifecycle.ViewModel
import com.android.ootd.model.items.Material
import com.android.ootd.utils.TypeSuggestionsLoader

/**
 * Base ViewModel for item-related screens.
 *
 * Contains shared functionality for managing item data, including type suggestions, error messages,
 * and field setters.
 */
abstract class BaseItemViewModel : ViewModel() {

  protected var typeSuggestions: Map<String, List<String>> = emptyMap()

  /**
   * Initializes the type suggestions from a YAML file.
   *
   * Should be called from the composable with the context.
   *
   * @param context The Android context used to load suggestions.
   */
  fun initTypeSuggestions(context: Context) {
    typeSuggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)
  }

  /**
   * Sets the type in the UI state.
   *
   * @param type The type name.
   */
  fun setType(type: String) {
    updateType(type)
  }

  /**
   * Sets the brand in the UI state.
   *
   * @param brand The brand name.
   */
  fun setBrand(brand: String) {
    updateBrand(brand)
  }

  /**
   * Sets the link in the UI state.
   *
   * @param link The URL link.
   */
  fun setLink(link: String) {
    updateLink(link)
  }

  /**
   * Parses material text input and updates the UI state.
   *
   * @param material The material text (e.g., "Cotton 80%, Wool 20%").
   */
  fun setMaterial(material: String) {
    // Parse text like "Cotton 80%, Wool 20%"
    val materials =
        material.split(",").mapNotNull { entry ->
          val parts = entry.trim().split(" ")
          if (parts.size == 2 && parts[1].endsWith("%")) {
            val name = parts[0]
            val percentage = parts[1].removeSuffix("%").toDoubleOrNull()
            if (percentage != null) Material(name, percentage) else null
          } else null
        }
    updateMaterial(material, materials)
  }

  /**
   * Updates the type suggestions based on the current category and input.
   *
   * @param input The input string to filter suggestions.
   */
  fun updateTypeSuggestions(input: String) {
    val currentCategory = getCurrentCategory()

    val normalizedCategory =
        when (currentCategory.trim().lowercase()) {
          "clothes",
          "clothing" -> "Clothing"
          "shoe",
          "shoes" -> "Shoes"
          "bag",
          "bags" -> "Bags"
          "accessory",
          "accessories" -> "Accessories"
          else -> currentCategory
        }

    val allSuggestions = typeSuggestions[normalizedCategory] ?: emptyList()

    val filtered =
        if (input.isBlank()) {
          allSuggestions
        } else {
          allSuggestions.filter { it.startsWith(input, ignoreCase = true) }
        }

    updateSuggestions(filtered)
  }

  /**
   * Updates category suggestions based on input.
   *
   * @param input The input string to filter suggestions.
   */
  fun updateCategorySuggestions(input: String) {
    val categories = typeSuggestions.keys.toList()
    val filtered =
        if (input.isBlank()) {
          categories
        } else {
          categories.filter { it.startsWith(input, ignoreCase = true) }
        }

    updateCategorySuggestionsState(filtered)
  }

  /** Clears the error message in the UI state. */
  abstract fun clearErrorMsg()

  /**
   * Sets the error message in the UI state.
   *
   * @param msg The error message to display.
   */
  abstract fun setErrorMsg(msg: String)

  /**
   * Updates the type in the UI state.
   *
   * @param type The type value.
   */
  protected abstract fun updateType(type: String)

  /**
   * Updates the brand in the UI state.
   *
   * @param brand The brand value.
   */
  protected abstract fun updateBrand(brand: String)

  /**
   * Updates the link in the UI state.
   *
   * @param link The link value.
   */
  protected abstract fun updateLink(link: String)

  /**
   * Updates the material in the UI state.
   *
   * @param materialText The raw material text.
   * @param materials The parsed material list.
   */
  protected abstract fun updateMaterial(materialText: String, materials: List<Material>)

  /**
   * Gets the current category from the UI state.
   *
   * @return The current category string.
   */
  protected abstract fun getCurrentCategory(): String

  /**
   * Updates the type suggestions in the UI state.
   *
   * @param suggestions The filtered suggestions list.
   */
  protected abstract fun updateSuggestions(suggestions: List<String>)

  /**
   * Updates the category suggestions in the UI state.
   *
   * @param suggestions The filtered category suggestions list.
   */
  protected abstract fun updateCategorySuggestionsState(suggestions: List<String>)
}

/** Common state properties interface for item-related UI states. */
interface ItemUIState {
  val type: String
  val brand: String
  val link: String
  val material: List<Material>
  val materialText: String
  val category: String

  fun copy(
      type: String = this.type,
      brand: String = this.brand,
      link: String = this.link,
      material: List<Material> = this.material,
      materialText: String = this.materialText
  ): ItemUIState
}
