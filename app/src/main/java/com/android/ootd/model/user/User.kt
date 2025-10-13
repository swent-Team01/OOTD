package com.android.ootd.model.user

import android.net.Uri

/**
 * Represents a friend of a user.
 *
 * @property uid Firebase user id of the friend.
 * @property username Display name of the friend.
 * @property profilePicture Firebase Storage download URI for the friend's profile picture.
 */
data class Friend(
    val uid: String = "",
    val username: String = "",
    val profilePicture: Uri = Uri.EMPTY // Firebase Storage download URL
)

/**
 * Represents an app user.
 *
 * @property uid Firebase user id.
 * @property username Display name of the user.
 * @property profilePicture Firebase Storage download URI for the user's profile picture.
 * @property friendList List of the user's friends.
 */
data class User(
    val uid: String = "",
    val username: String = "",
    val profilePicture: Uri = Uri.EMPTY, // Firebase Storage download URL
    val friendList: List<Friend> = emptyList()
)
