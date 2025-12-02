package com.android.ootd.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

/** Utility functions for creating map marker bitmaps. */
object MarkerUtils {
  private const val MARKER_SIZE = 120

  /**
   * Creates a circular bitmap with a border from the provided bitmap.
   *
   * @param bitmap The source bitmap to make circular
   * @return A circular bitmap with a colored border
   */
  fun createCircularBitmap(bitmap: Bitmap): Bitmap {
    val size = MARKER_SIZE
    val output = createBitmap(size, size)
    val canvas = Canvas(output)
    val paint =
        Paint().apply {
          isAntiAlias = true
          color = Primary.toArgb()
        }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    paint.color = Secondary.toArgb()
    canvas.drawCircle(size / 2f, size / 2f, size / 2.2f, paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(bitmap.scale(size, size), 0f, 0f, paint)
    return output
  }

  /**
   * Creates a bitmap with the user's initial letter for use as a map marker.
   *
   * @param username The username to extract the initial from
   * @return A BitmapDescriptor suitable for use as a map marker icon
   */
  fun createInitialsBitmap(username: String): BitmapDescriptor {
    val size = MARKER_SIZE
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    val paint =
        Paint().apply {
          isAntiAlias = true
          color = Primary.toArgb()
        }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    paint.color = Secondary.toArgb()
    canvas.drawCircle(size / 2f, size / 2f, size / 2.2f, paint)
    paint.color = Primary.toArgb()
    canvas.drawCircle(size / 2f, size / 2f, size / 2.5f, paint)

    val textPaint =
        Paint().apply {
          isAntiAlias = true
          color = Secondary.toArgb()
          textSize = 50f
          textAlign = Paint.Align.CENTER
        }
    val initial = username.firstOrNull()?.uppercase() ?: "?"
    canvas.drawText(
        initial,
        size / 2f,
        (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f),
        textPaint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
  }

  /**
   * Creates a bitmap showing the number of clustered markers.*
   *
   * @param count The number of items in the cluster
   * @return A BitmapDescriptor showing the count in a circular badge
   */
  fun createClusterBitmap(count: Int): BitmapDescriptor {
    val size = MARKER_SIZE
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)
    val paint =
        Paint().apply {
          isAntiAlias = true
          color = Secondary.toArgb()
        }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    paint.color = Primary.toArgb()
    canvas.drawCircle(size / 2f, size / 2f, size / 2.5f, paint)

    val textPaint =
        Paint().apply {
          isAntiAlias = true
          color = Secondary.toArgb()
          textSize = if (count < 100) 40f else 30f
          textAlign = Paint.Align.CENTER
          isFakeBoldText = true
        }
    val text = if (count > 999) "999+" else count.toString()
    canvas.drawText(
        text, size / 2f, (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f), textPaint)
    return BitmapDescriptorFactory.fromBitmap(bitmap)
  }
}
