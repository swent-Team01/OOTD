package com.android.ootd.ui.map

import com.android.ootd.model.map.Location
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.utils.LocationUtils.toLatLng
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

/**
 * Cluster item wrapper for OutfitPost to enable marker clustering on the map.
 *
 * Implements ClusterItem interface to provide position, title, and snippet for each post marker.
 * Supports adjusted locations to prevent overlapping markers at the same position.
 *
 * @property post The outfit post to be displayed as a marker
 * @property adjustedLocation The adjusted location to use (defaults to post's original location)
 */
data class PostMarker(val post: OutfitPost, val adjustedLocation: Location = post.location) :
    ClusterItem {
  /**
   * The geographic position where this cluster item (marker) should be placed on the map.
   *
   * @return a [LatLng] representing the adjusted location on the map.
   */
  override fun getPosition(): LatLng = adjustedLocation.toLatLng()

  /**
   * The marker title. Not currently used since info windows are not displayed.
   *
   * @return the post name as a [String].
   */
  override fun getTitle(): String = post.name

  /**
   * The marker snippet. Not currently used since info windows are not displayed.
   *
   * @return null as snippets are not used in this implementation.
   */
  override fun getSnippet(): String? = null

  /**
   * The drawing z-index for the marker. Higher values are drawn above lower ones.
   *
   * @return the default z-index of 0f.
   */
  override fun getZIndex(): Float = 0f
}
