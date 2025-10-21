package com.android.ootd.model.user

/**
 * Represents a public user profile for search and discovery. This is the public-facing user data
 * with relaxed read access.
 *
 * @property uid Firebase user id.
 * @property username Display name of the user.
 */
data class User(val uid: String = "", val username: String = "")
