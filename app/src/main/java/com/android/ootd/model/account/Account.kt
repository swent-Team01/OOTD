package com.android.ootd.model.account

import com.android.ootd.model.user.User

/**
 * Account model representing a user's account data.
 *
 * @property user the user profile information
 * @property ownerId unique identifier of the account owner
 * @property username display name (may be blank)
 * @property birthday user's birthday as an ISO string (may be blank)
 * @property googleAccountEmail linked Google account email (may be blank)
 * @property profilePicture URI to the profile picture (Firebase Storage URL or Uri.EMPTY)
 * @property friendUids list of friend user IDs
 */
data class Account(
    val user: User = User(),
    val ownerId: String = "",
    val username: String = "",
    val birthday: String = "",
    val googleAccountEmail: String = "",
    val profilePicture: String = "",
    val friendUids: List<String> = emptyList()
)
