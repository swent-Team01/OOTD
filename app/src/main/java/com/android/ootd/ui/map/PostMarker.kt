package com.android.ootd.ui.map

import com.android.ootd.model.map.Location
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.utils.LocationUtils.toLatLng
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

/**
 * Cluster item wrapper for OutfitPost to enable marker clustering on the map.
 *
 * @property post The outfit post to be displayed as a marker
 * @property adjustedLocation The adjusted location to use (defaults to post's original location)
 */
data class PostMarker(val post: OutfitPost, val adjustedLocation: Location = post.location) :
    ClusterItem {
  override fun getPosition(): LatLng = adjustedLocation.toLatLng()

  override fun getTitle(): String = post.name

  override fun getSnippet(): String? = null

  override fun getZIndex(): Float = 0f
}
