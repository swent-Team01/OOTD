package com.android.ootd.ui.post

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Material
import com.android.ootd.utils.TypeSuggestionsLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Base ViewModel for item-related screens.
 *
 * Contains shared functionality for managing item data, including type suggestions, error messages,
 * and field setters. Uses a generic type parameter to work with different UI state types.
 *
 * @param T The type of UI state this ViewModel manages.
 */
abstract class BaseItemViewModel<T : Any> : ViewModel() {

  protected var typeSuggestions: Map<String, List<String>> = emptyMap()

  /** Subclasses provide the initial UI state. */
  protected abstract fun initialState(): T

  /** Mutable flow is internal; NOT exposed. */
  protected val _uiState: MutableStateFlow<T> by lazy { MutableStateFlow(initialState()) }

  /** Public read-only state. */
  val uiState: StateFlow<T> = _uiState.asStateFlow()

  fun initTypeSuggestions(context: Context) {
    typeSuggestions = TypeSuggestionsLoader.loadTypeSuggestions(context)
  }

  fun setType(type: String) = updateState { updateType(it, type) }

  fun setBrand(brand: String) = updateState { updateBrand(it, brand) }

  fun setLink(link: String) = updateState { updateLink(it, link) }

  fun setMaterial(material: String) {
    val materials =
        material.split(",").mapNotNull { entry ->
          val parts = entry.trim().split(" ")
          if (parts.size == 2 && parts[1].endsWith("%")) {
            parts[1].removeSuffix("%").toDoubleOrNull()?.let { pct -> Material(parts[0], pct) }
          } else null
        }
    updateState { updateMaterial(it, material, materials) }
  }

  fun updateTypeSuggestions(input: String) {
    val currentCategory = getCategory(_uiState.value)
    val normalizedCategory = normalizeCategory(currentCategory).trim()
    val allSuggestions =
        typeSuggestions.entries
            .firstOrNull { it.key.equals(normalizedCategory, ignoreCase = true) }
            ?.value
            .orEmpty()

    val filtered =
        if (input.isBlank()) allSuggestions
        else allSuggestions.filter { it.startsWith(input, ignoreCase = true) }

    updateState { updateTypeSuggestionsState(it, filtered) }
  }

  fun updateCategorySuggestions(input: String) {
    val categories = typeSuggestions.keys.toList()
    val filtered =
        if (input.isBlank()) categories
        else categories.filter { it.startsWith(input, ignoreCase = true) }
    updateState { updateCategorySuggestionsState(it, filtered) }
  }

  protected fun normalizeCategory(category: String): String =
      when (category.trim().lowercase()) {
        "clothes",
        "clothing" -> "Clothing"
        "shoe",
        "shoes" -> "Shoes"
        "bag",
        "bags" -> "Bags"
        "accessory",
        "accessories" -> "Accessories"
        else -> category
      }

  fun clearErrorMsg() = updateState { setErrorMessage(it, null) }

  fun setErrorMsg(msg: String) = updateState { setErrorMessage(it, msg) }

  protected fun updateState(transform: (T) -> T) {
    _uiState.value = transform(_uiState.value)
  }

  protected abstract fun updateType(state: T, type: String): T

  protected abstract fun updateBrand(state: T, brand: String): T

  protected abstract fun updateLink(state: T, link: String): T

  protected abstract fun updateMaterial(
      state: T,
      materialText: String,
      materials: List<Material>
  ): T

  protected abstract fun getCategory(state: T): String

  protected abstract fun setErrorMessage(state: T, message: String?): T

  protected abstract fun updateTypeSuggestionsState(state: T, suggestions: List<String>): T

  protected abstract fun updateCategorySuggestionsState(state: T, suggestions: List<String>): T

  protected abstract fun setPhotoState(
      state: T,
      uri: Uri?,
      image: ImageData,
      invalidPhotoMsg: String?
  ): T

  open fun setPhoto(uri: Uri) {
    if (uri == Uri.EMPTY) {
      updateState { setPhotoState(it, null, ImageData("", ""), "Please select a photo.") }
    } else {
      updateState { setPhotoState(it, uri, ImageData("", ""), null) }
    }
  }
}
