package com.android.ootd.ui.map

import android.content.Context
import com.android.ootd.model.user.UserRepository
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.clustering.ClusterManager
import kotlinx.coroutines.CoroutineScope

/**
 * Custom cluster renderer for public profile markers that displays profile pictures.
 *
 * This is a specialized version of ProfileClusterRenderer for PublicProfileMarker items. See
 * ProfileClusterRenderer for implementation details.
 *
 * @param context Android context for resource access
 * @param map GoogleMap instance
 * @param clusterManager ClusterManager instance
 * @param userRepository Repository to fetch user profile pictures
 * @param coroutineScope Coroutine scope for async operations
 */
class PublicProfileClusterRenderer(
    context: Context,
    map: GoogleMap,
    clusterManager: ClusterManager<PublicProfileMarker>,
    userRepository: UserRepository,
    coroutineScope: CoroutineScope
) :
    ProfileClusterRenderer<PublicProfileMarker>(
        context, map, clusterManager, userRepository, coroutineScope)
