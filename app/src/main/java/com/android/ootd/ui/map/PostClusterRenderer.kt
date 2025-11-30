package com.android.ootd.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.toArgb
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.android.ootd.model.user.UserRepository
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
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
 * - Circular badges with counts for clustered markers
 *
 * This renderer handles asynchronous loading of profile pictures and caches them for performance.
 *
 * Disclaimer: This takes inspiration from
 * https://developers.google.com/maps/documentation/android-sdk/utility/marker-clustering.
 * Additionally, AI was used to assist in the development of this class.
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

  /**
   * Called before the post marker is rendered. Loads and displays user profile picture
   * asynchronously.
   */
  override fun onBeforeClusterItemRendered(item: PostMarker, markerOptions: MarkerOptions) {
    val userId = item.post.ownerId
    val username = item.post.name

    // Check cache first
    if (profilePictureCache.containsKey(userId)) {
      val bitmap = profilePictureCache[userId]
      val icon =
          if (bitmap != null) {
            BitmapDescriptorFactory.fromBitmap(createCircularBitmap(bitmap))
          } else {
            createInitialsBitmap(username)
          }
      markerOptions.icon(icon).anchor(0.5f, 0.5f)
    } else {
      // Set initials as placeholder
      markerOptions.icon(createInitialsBitmap(username)).anchor(0.5f, 0.5f)

      // Load profile picture asynchronously
      loadProfilePicture(userId, username, item)
    }
  }

  /**
   * Called when updating an existing cluster item marker. Updates the marker icon if a profile
   * picture has been loaded.
   */
  override fun onClusterItemUpdated(item: PostMarker, marker: Marker) {
    markerCache[item.post.postUID] = marker

    val userId = item.post.ownerId
    val username = item.post.name

    if (profilePictureCache.containsKey(userId)) {
      val bitmap = profilePictureCache[userId]
      val icon =
          if (bitmap != null) {
            BitmapDescriptorFactory.fromBitmap(createCircularBitmap(bitmap))
          } else {
            createInitialsBitmap(username)
          }
      marker.setIcon(icon)
    }
  }

  /**
   * Called before the cluster (group of markers) is rendered. Creates a circular badge with the
   * count of posts in the cluster.
   */
  override fun onBeforeClusterRendered(cluster: Cluster<PostMarker>, markerOptions: MarkerOptions) {
    val icon = createClusterBitmap(cluster.size)
    markerOptions.icon(icon).anchor(0.5f, 0.5f)
  }

  /** Loads profile picture asynchronously and updates the marker when loaded. */
  private fun loadProfilePicture(userId: String, username: String, item: PostMarker) {
    coroutineScope.launch {
      try {
        if (userId.isNotBlank()) {
          val user = userRepository.getUser(userId)

          if (user.profilePicture.isNotBlank()) {
            val bitmap =
                withContext(Dispatchers.IO) {
                  val imageLoader = ImageLoader(context)
                  val request =
                      ImageRequest.Builder(context)
                          .data(user.profilePicture)
                          .size(120)
                          .allowHardware(false)
                          .build()

                  val result = imageLoader.execute(request)
                  if (result is SuccessResult) {
                    (result.drawable as? BitmapDrawable)?.bitmap
                  } else null
                }

            profilePictureCache[userId] = bitmap

            // Update marker if it exists
            withContext(Dispatchers.Main) {
              markerCache[item.post.postUID]?.setIcon(
                  if (bitmap != null) {
                    BitmapDescriptorFactory.fromBitmap(createCircularBitmap(bitmap))
                  } else {
                    createInitialsBitmap(username)
                  })
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

  /** Creates a circular bitmap from a square bitmap for profile pictures. */
  private fun createCircularBitmap(bitmap: Bitmap): Bitmap {
    val size = 120
    val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)

    val paint =
        Paint().apply {
          isAntiAlias = true
          color = Secondary.toArgb()
        }

    val rect = Rect(0, 0, size, size)
    val radius = size / 2f

    canvas.drawCircle(radius, radius, radius, paint)

    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true)
    canvas.drawBitmap(scaledBitmap, rect, rect, paint)

    return output
  }

  /** Creates a bitmap with user initials for markers without profile pictures. */
  private fun createInitialsBitmap(username: String): BitmapDescriptor {
    val size = 120
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Draw circle background
    val paint =
        Paint().apply {
          isAntiAlias = true
          color = Primary.toArgb()
        }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

    // Draw initial letter
    val textPaint =
        Paint().apply {
          isAntiAlias = true
          color = Secondary.toArgb()
          textSize = 50f
          textAlign = Paint.Align.CENTER
        }

    val initial = username.firstOrNull()?.uppercase() ?: "?"
    val xPos = size / 2f
    val yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)

    canvas.drawText(initial, xPos, yPos, textPaint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
  }

  /** Creates a bitmap for cluster markers showing the count of posts. */
  private fun createClusterBitmap(count: Int): BitmapDescriptor {
    val size = 120
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Draw circle background
    val paint =
        Paint().apply {
          isAntiAlias = true
          color = Primary.toArgb()
        }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

    // Draw secondary (light) inner circle
    paint.color = Secondary.toArgb()
    canvas.drawCircle(size / 2f, size / 2f, size / 2.5f, paint)

    // Draw primary color for the count background
    paint.color = Primary.toArgb()
    canvas.drawCircle(size / 2f, size / 2f, size / 3f, paint)

    // Draw count text
    val textPaint =
        Paint().apply {
          isAntiAlias = true
          color = Secondary.toArgb()
          textSize = if (count < 100) 40f else 30f
          textAlign = Paint.Align.CENTER
          isFakeBoldText = true
        }

    val text = if (count > 999) "999+" else count.toString()
    val xPos = size / 2f
    val yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)

    canvas.drawText(text, xPos, yPos, textPaint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
  }
}
