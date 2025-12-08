package com.android.ootd.ui.map

import com.google.maps.android.clustering.ClusterItem

/**
 * Interface for cluster items that display profile pictures.
 *
 * Provides the necessary information for rendering profile-based markers on the map with clustering
 * support.
 */
interface ProfileMarkerItem : ClusterItem {
  /** The user ID of the profile owner (used for caching profile pictures) */
  val userId: String

  /** The display name/username (used for initials fallback) */
  val displayName: String

  /** A unique identifier for this specific marker (used for marker cache) */
  val markerId: String
}
