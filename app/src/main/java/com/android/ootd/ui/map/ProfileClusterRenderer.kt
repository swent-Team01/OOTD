package com.android.ootd.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.toArgb
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.android.ootd.model.user.UserRepository
import com.android.ootd.ui.theme.Primary
import com.android.ootd.utils.MarkerUtils
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Generic cluster renderer that displays profile pictures for individual markers and circular
 * badges with counts for clustered markers. This renderer handles asynchronous loading of profile
 * pictures and caches them for performance.
 *
 * Disclaimer: This takes inspiration from
 * https://developers.google.com/maps/documentation/android-sdk/utility/marker-clustering.
 * Additionally, AI was used to assist in the development of this class.
 *
 * @param T The type of cluster item, must implement ProfileMarkerItem
 * @param context Android context for resource access
 * @param map GoogleMap instance
 * @param clusterManager ClusterManager instance
 * @param userRepository Repository to fetch user profile pictures
 * @param coroutineScope Coroutine scope for async operations
 */
open class ProfileClusterRenderer<T : ProfileMarkerItem>(
    private val context: Context,
    map: GoogleMap,
    clusterManager: ClusterManager<T>,
    private val userRepository: UserRepository,
    private val coroutineScope: CoroutineScope
) : DefaultClusterRenderer<T>(context, map, clusterManager) {

  private val profilePictureCache = mutableMapOf<String, Bitmap?>()
  private val markerCache = mutableMapOf<String, Marker>()

  init {
    minClusterSize = 2
  }

  override fun shouldRenderAsCluster(cluster: Cluster<T>) = cluster.size >= minClusterSize

  override fun getColor(clusterSize: Int) = Primary.toArgb()

  override fun onBeforeClusterItemRendered(item: T, markerOptions: MarkerOptions) {
    val userId = item.userId
    val displayName = item.displayName

    if (profilePictureCache.containsKey(userId)) {
      val icon =
          profilePictureCache[userId]?.let {
            BitmapDescriptorFactory.fromBitmap(MarkerUtils.createCircularBitmap(it))
          } ?: MarkerUtils.createInitialsBitmap(displayName)
      markerOptions.icon(icon).anchor(0.5f, 0.5f)
    } else {
      markerOptions.icon(MarkerUtils.createInitialsBitmap(displayName)).anchor(0.5f, 0.5f)
      loadProfilePicture(userId, displayName, item)
    }
  }

  override fun onClusterItemUpdated(item: T, marker: Marker) {
    markerCache[item.markerId] = marker
    val userId = item.userId

    if (profilePictureCache.containsKey(userId)) {
      val icon =
          profilePictureCache[userId]?.let {
            BitmapDescriptorFactory.fromBitmap(MarkerUtils.createCircularBitmap(it))
          } ?: MarkerUtils.createInitialsBitmap(item.displayName)
      marker.setIcon(icon)
    }
  }

  override fun onBeforeClusterRendered(cluster: Cluster<T>, markerOptions: MarkerOptions) {
    markerOptions.icon(MarkerUtils.createClusterBitmap(cluster.size)).anchor(0.5f, 0.5f)
  }

  private fun loadProfilePicture(userId: String, displayName: String, item: T) {
    coroutineScope.launch {
      try {
        if (userId.isNotBlank()) {
          val user = userRepository.getUser(userId)
          if (user.profilePicture.isNotBlank()) {
            val bitmap =
                withContext(Dispatchers.IO) {
                  val request =
                      ImageRequest.Builder(context)
                          .data(user.profilePicture)
                          .size(120)
                          .allowHardware(false)
                          .build()
                  val result = ImageLoader(context).execute(request)
                  (result as? SuccessResult)?.drawable?.let { (it as? BitmapDrawable)?.bitmap }
                }
            profilePictureCache[userId] = bitmap
            withContext(Dispatchers.Main) {
              markerCache[item.markerId]?.setIcon(
                  bitmap?.let {
                    BitmapDescriptorFactory.fromBitmap(MarkerUtils.createCircularBitmap(it))
                  } ?: MarkerUtils.createInitialsBitmap(displayName))
            }
          } else {
            profilePictureCache[userId] = null
          }
        }
      } catch (e: Exception) {
        profilePictureCache[userId] = null
      }
    }
  }
}
