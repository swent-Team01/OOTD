package com.android.ootd.utils

object UsernameValidator {
  private val USERNAME_REGEX = "^[a-zA-Z0-9_]{3,20}$".toRegex()

  fun isValid(username: String): Boolean {
    return USERNAME_REGEX.matches(username)
  }

  fun errorMessage(username: String): String? {
    if (username.isBlank()) return "Username cannot be empty."
    if (username.length < 3) return "Username must be at least 3 characters."
    if (username.length > 20) return "Username must be at most 20 characters."
    if (!USERNAME_REGEX.matches(username))
        return "Username can only contain letters, numbers, and underscores."
    return null
  }
}
