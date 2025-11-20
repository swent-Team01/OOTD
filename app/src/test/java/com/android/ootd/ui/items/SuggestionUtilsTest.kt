package com.android.ootd.ui.items

import com.android.ootd.ui.post.items.filterDropdownSuggestions
import kotlin.test.assertEquals
import org.junit.Test

class SuggestionUtilsTest {

  private val suggestions = listOf("Slim", "Regular", "Relaxed", "Streetwear")

  @Test
  fun filterDropdownSuggestions_returnsAllWhenInputBlank() {
    val result = filterDropdownSuggestions("", suggestions)
    assertEquals(suggestions, result)
  }

  @Test
  fun filterDropdownSuggestions_matchesPrefixIgnoringCase() {
    val result = filterDropdownSuggestions("re", suggestions)
    assertEquals(listOf("Regular", "Relaxed"), result)

    val noMatch = filterDropdownSuggestions("xyz", suggestions)
    assertEquals(emptyList(), noMatch)
  }
}
