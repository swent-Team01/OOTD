package com.android.ootd.model.account

import com.android.ootd.model.map.Location
import com.android.ootd.model.map.emptyLocation

/**
 * Account model representing a user's account data.
 *
 * @property uid unique identifier of the account owner
 * @property ownerId unique identifier of the account owner for consistency across the app
 * @property username display name (may be blank)
 * @property birthday user's birthday as an ISO string (may be blank)
 * @property googleAccountEmail linked Google account email (may be blank)
 * @property profilePicture URL to the profile picture (Firebase Storage URL or empty string)
 * @property friendUids list of friend user IDs
 * @property isPrivate whether the account is private or not
 * @property location user's location
 */
data class Account(
    val uid: String = "",
    val ownerId: String = "",
    val username: String = "",
    val birthday: String = "",
    val googleAccountEmail: String = "",
    val profilePicture: String = "",
    val friendUids: List<String> = emptyList(),
    val isPrivate: Boolean = false,
    val location: Location = emptyLocation
)

/** Exception thrown when a required location is missing. */
class MissingLocationException : Exception("Location must be selected")
