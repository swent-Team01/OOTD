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
 * Custom cluster renderer that displays:
 * - Profile pictures for individual post markers
 * - Circular badges with counts for clustered markers This renderer handles asynchronous loading of
 *   profile pictures and caches them for performance. Disclaimer: This takes inspiration from
 *   https://developers.google.com/maps/documentation/android-sdk/utility/marker-clustering.
 *   Additionally, AI was used to assist in the development of this class.
 *
 * @param context Android context for resource access
 * @param map GoogleMap instance
 * @param clusterManager ClusterManager instance
 * @param userRepository Repository to fetch user profile pictures
 * @param coroutineScope Coroutine scope for async operations
 */
class PostClusterRenderer(
    private val context: Context,
    map: GoogleMap,
    clusterManager: ClusterManager<PostMarker>,
    private val userRepository: UserRepository,
    private val coroutineScope: CoroutineScope
) : DefaultClusterRenderer<PostMarker>(context, map, clusterManager) {

  private val profilePictureCache = mutableMapOf<String, Bitmap?>()
  private val markerCache = mutableMapOf<String, Marker>()

  init {
    minClusterSize = 2
  }

  override fun shouldRenderAsCluster(cluster: Cluster<PostMarker>) = cluster.size >= minClusterSize

  override fun getColor(clusterSize: Int) = Primary.toArgb()

  override fun onBeforeClusterItemRendered(item: PostMarker, markerOptions: MarkerOptions) {
    val userId = item.post.ownerId
    val username = item.post.name

    if (profilePictureCache.containsKey(userId)) {
      val icon =
          profilePictureCache[userId]?.let {
            BitmapDescriptorFactory.fromBitmap(MarkerUtils.createCircularBitmap(it))
          } ?: MarkerUtils.createInitialsBitmap(username)
      markerOptions.icon(icon).anchor(0.5f, 0.5f)
    } else {
      markerOptions.icon(MarkerUtils.createInitialsBitmap(username)).anchor(0.5f, 0.5f)
      loadProfilePicture(userId, username, item)
    }
  }

  override fun onClusterItemUpdated(item: PostMarker, marker: Marker) {
    markerCache[item.post.postUID] = marker
    val userId = item.post.ownerId

    if (profilePictureCache.containsKey(userId)) {
      val icon =
          profilePictureCache[userId]?.let {
            BitmapDescriptorFactory.fromBitmap(MarkerUtils.createCircularBitmap(it))
          } ?: MarkerUtils.createInitialsBitmap(item.post.name)
      marker.setIcon(icon)
    }
  }

  override fun onBeforeClusterRendered(cluster: Cluster<PostMarker>, markerOptions: MarkerOptions) {
    markerOptions.icon(MarkerUtils.createClusterBitmap(cluster.size)).anchor(0.5f, 0.5f)
  }

  private fun loadProfilePicture(userId: String, username: String, item: PostMarker) {
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
              markerCache[item.post.postUID]?.setIcon(
                  bitmap?.let {
                    BitmapDescriptorFactory.fromBitmap(MarkerUtils.createCircularBitmap(it))
                  } ?: MarkerUtils.createInitialsBitmap(username))
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
