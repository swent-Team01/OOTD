package com.android.ootd.ui.post.items

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Material
import com.android.ootd.utils.CategoryNormalizer
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
  fun setType(type: String) = updateState { updateType(it, type) }

  /**
   * Sets the brand in the UI state.
   *
   * @param brand The brand name.
   */
  fun setBrand(brand: String) = updateState { updateBrand(it, brand) }

  /**
   * Sets the link in the UI state.
   *
   * @param link The URL link.
   */
  fun setLink(link: String) = updateState { updateLink(it, link) }

  /**
   * Parses material text input and updates the UI state.
   *
   * @param material The material text (e.g., "Cotton 80%, Wool 20%").
   */
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

  /**
   * Updates the type suggestions based on the current category and input.
   *
   * @param input The input string to filter suggestions.
   */
  fun updateTypeSuggestions(input: String) {
    val currentCategory = getCategory(_uiState.value)
    val allSuggestions = typeSuggestions[currentCategory].orEmpty()

    val filtered =
        if (input.isBlank()) allSuggestions
        else allSuggestions.filter { it.startsWith(input, ignoreCase = true) }

    updateState { updateTypeSuggestionsState(it, filtered) }
  }

  /**
   * Updates category suggestions based on input.
   *
   * @param input The input string to filter suggestions.
   */
  fun updateCategorySuggestions(input: String) {
    val filtered =
        if (input.isBlank()) CategoryNormalizer.VALID_CATEGORIES
        else CategoryNormalizer.VALID_CATEGORIES.filter { it.startsWith(input, ignoreCase = true) }
    updateState { updateCategorySuggestionsState(it, filtered) }
  }

  fun clearErrorMsg() = updateState { setErrorMessage(it, null) }

  /**
   * Sets the error message in the UI state.
   *
   * @param msg The error message to display.
   */
  fun setErrorMsg(msg: String) = updateState { setErrorMessage(it, msg) }

  /**
   * Updates the UI state using a transformation function.
   *
   * @param transform A function that takes the current state and returns the new state.
   */
  protected fun updateState(transform: (T) -> T) {
    _uiState.value = transform(_uiState.value)
  }

  /**
   * Updates the type in the UI state.
   *
   * @param state The current state.
   * @param type The type value.
   * @return The updated state.
   */
  protected abstract fun updateType(state: T, type: String): T

  /**
   * Updates the brand in the UI state.
   *
   * @param state The current state.
   * @param brand The brand value.
   * @return The updated state.
   */
  protected abstract fun updateBrand(state: T, brand: String): T

  /**
   * Updates the link in the UI state.
   *
   * @param state The current state.
   * @param link The link value.
   * @return The updated state.
   */
  protected abstract fun updateLink(state: T, link: String): T

  /**
   * Updates the material in the UI state.
   *
   * @param state The current state.
   * @param materialText The raw material text.
   * @param materials The parsed material list.
   * @return The updated state.
   */
  protected abstract fun updateMaterial(
      state: T,
      materialText: String,
      materials: List<Material>
  ): T

  /**
   * Gets the current category from the UI state.
   *
   * @param state The current state.
   * @return The current category string.
   */
  protected abstract fun getCategory(state: T): String

  /**
   * Sets the error message in the UI state.
   *
   * @param state The current state.
   * @param message The error message (null to clear).
   * @return The updated state.
   */
  protected abstract fun setErrorMessage(state: T, message: String?): T

  /**
   * Updates the type suggestions in the UI state.
   *
   * @param state The current state.
   * @param suggestions The filtered suggestions list.
   * @return The updated state.
   */
  protected abstract fun updateTypeSuggestionsState(state: T, suggestions: List<String>): T

  /**
   * Updates the category suggestions in the UI state.
   *
   * @param state The current state.
   * @param suggestions The filtered category suggestions list.
   * @return The updated state.
   */
  protected abstract fun updateCategorySuggestionsState(state: T, suggestions: List<String>): T

  /**
   * Sets the photo URI in the UI state.
   *
   * @param state The current state.
   * @param uri The URI of the selected photo.
   * @param invalidPhotoMsg The validation message (null if valid).
   * @return The updated state.
   */
  protected abstract fun setPhotoState(
      state: T,
      uri: Uri?,
      image: ImageData,
      invalidPhotoMsg: String?
  ): T

  /**
   * Sets the photo in the UI state.
   *
   * @param uri The URI of the selected photo.
   */
  open fun setPhoto(uri: Uri) {
    if (uri == Uri.EMPTY) {
      updateState { setPhotoState(it, null, ImageData("", ""), "Please select a photo.") }
    } else {
      updateState { setPhotoState(it, uri, ImageData("", ""), null) }
    }
  }
}
