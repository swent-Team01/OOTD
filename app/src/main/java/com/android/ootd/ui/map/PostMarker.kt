package com.android.ootd.ui.map

import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.utils.LocationUtils.toLatLng
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

/**
 * Cluster item wrapper for OutfitPost to enable marker clustering on the map.
 *
 * Implements ClusterItem interface to provide position, title, and snippet for each post marker.
 *
 * @property post The outfit post to be displayed as a marker
 */
data class PostMarker(val post: OutfitPost) : ClusterItem {
  /**
   * The geographic position where this cluster item (marker) should be placed on the map.
   *
   * @return a [LatLng] representing the post's location on the map.
   */
  override fun getPosition(): LatLng = post.location.toLatLng()

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
