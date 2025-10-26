package com.android.ootd.ui.post

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * Creates a [NestedScrollConnection] that handles image resizing based on scroll gestures.
 *
 * @param currentImageSize The mutable state holding the current image size
 * @param imageScale The mutable state holding the current image scale factor
 * @param minImageSize The minimum allowed image size
 * @param maxImageSize The maximum allowed image size
 * @return A [NestedScrollConnection] that updates image size and scale based on scroll input
 */

// Code inspired from the official google doc :
// https://developer.android.com/develop/ui/compose/touch-input/pointer-input/scroll#result

@Composable
fun rememberImageResizeScrollConnection(
    currentImageSize: MutableState<Dp>,
    imageScale: MutableState<Float>,
    minImageSize: Dp,
    maxImageSize: Dp
): NestedScrollConnection {
  val density = LocalDensity.current
  return remember(density, minImageSize, maxImageSize) {
    object : NestedScrollConnection {
      override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val deltaDp = with(density) { available.y.toDp() }

        val previousImageSize = currentImageSize.value
        val newImageSize = (previousImageSize + deltaDp).coerceIn(minImageSize, maxImageSize)
        val consumedDp = newImageSize - previousImageSize

        currentImageSize.value = newImageSize
        imageScale.value = currentImageSize.value / maxImageSize

        val consumedPx = with(density) { consumedDp.toPx() }
        return Offset(0f, consumedPx)
      }
    }
  }
}
