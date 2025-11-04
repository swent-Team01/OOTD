package com.android.ootd.model.items

object ImageFilenameSanitizer {
  /**
   * Sanitizes a file name for Firebase Storage:
   * - trims
   * - replaces whitespace with underscore
   * - replaces any char not in [A-Za-z0-9_.-] with underscore
   */
  fun sanitize(original: String): String {
    return original.trim().replace("\\s+".toRegex(), "_").replace("[^A-Za-z0-9_.-]".toRegex(), "_")
  }
}
