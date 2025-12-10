package com.android.ootd.ui.map

import com.android.ootd.model.account.PublicLocation
import com.android.ootd.model.map.Location
import com.android.ootd.utils.LocationUtils.toLatLng
import com.google.android.gms.maps.model.LatLng

/**
 * Cluster item wrapper for PublicLocation to enable marker clustering on the map.
 *
 * @property publicLocation The public location to be displayed as a marker
 * @property adjustedLocation The adjusted location to use (defaults to publicLocation's original
 *   location)
 */
data class PublicProfileMarker(
    val publicLocation: PublicLocation,
    val adjustedLocation: Location = publicLocation.location
) : ProfileMarkerItem {
  override fun getPosition(): LatLng = adjustedLocation.toLatLng()

  override fun getTitle(): String = publicLocation.username

  override fun getSnippet(): String? = null

  override fun getZIndex(): Float = 0f

  override val userId: String
    get() = publicLocation.ownerId

  override val displayName: String
    get() = publicLocation.username

  override val markerId: String
    get() = publicLocation.ownerId
}
