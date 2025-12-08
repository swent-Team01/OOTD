package com.android.ootd.model.account

import androidx.annotation.Keep
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.emptyLocation

/**
 * PublicLocation model representing publicly visible location data for accounts. This is stored
 * separately from Account to allow authenticated users to see locations of public accounts without
 * exposing other account details.
 *
 * @property ownerId unique identifier matching the account owner's ID
 * @property username display name of the account owner
 * @property location user's current location
 */
@Keep
data class PublicLocation(
    val ownerId: String = "",
    val username: String = "",
    val location: Location = emptyLocation
)
