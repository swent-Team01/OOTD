package com.android.ootd.ui.map

import android.content.Context
import com.android.ootd.model.user.UserRepository
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.clustering.ClusterManager

/**
 * Custom cluster renderer for public profile markers that displays profile pictures.
 *
 * @param context Android context for resource access
 * @param map GoogleMap instance
 * @param clusterManager ClusterManager instance
 * @param userRepository Repository to fetch user profile pictures
 */
class PublicProfileClusterRenderer(
    context: Context,
    map: GoogleMap,
    clusterManager: ClusterManager<PublicProfileMarker>,
    userRepository: UserRepository
) : ClusterRenderer<PublicProfileMarker>(context, map, clusterManager, userRepository)
