package com.android.ootd.model.items

import junit.framework.TestCase.assertEquals
import org.junit.Test

class ImageFilenameSanitizerTest {

  @Test
  fun trimsAndReplacesWhitespaceWithUnderscore() {
    assertEquals("hello_world", ImageFilenameSanitizer.sanitize("  hello   world  "))
  }

  @Test
  fun replacesSpecialCharactersWithUnderscore() {
    assertEquals(
        "file_name______chars______", ImageFilenameSanitizer.sanitize("file name !@#% chars ^&*()"))
  }

  @Test
  fun preservesSafeCharacters() {
    assertEquals(
        "file.name-with_underscores", ImageFilenameSanitizer.sanitize("file.name-with_underscores"))
  }

  @Test
  fun sanitizesPathLikeStrings() {
    assertEquals(
        "folder_subfolder_image", ImageFilenameSanitizer.sanitize("folder/subfolder/image"))
  }

  @Test
  fun numericAndUuidRemainMostlyUntouched() {
    val numeric = "1234567890"
    assertEquals(numeric, ImageFilenameSanitizer.sanitize(numeric))

    val uuid = "550e8400-e29b-41d4-a716-446655440000"
    assertEquals(uuid, ImageFilenameSanitizer.sanitize(uuid))
  }
}
