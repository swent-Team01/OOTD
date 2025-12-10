package com.android.ootd.ui.map

import com.android.ootd.model.account.PublicLocation
import com.android.ootd.model.map.Location
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Unit tests for PublicProfileMarker cluster item. */
class PublicProfileMarkerTest {

  private val testLocation = Location(46.5197, 6.6323, "Lausanne")
  private val testPublicLocation =
      PublicLocation(ownerId = "user123", username = "TestUser", location = testLocation)

  @Test
  fun getPosition_returnsCorrectLatLng() {
    val marker = PublicProfileMarker(testPublicLocation)
    val position = marker.getPosition()
    assertEquals(testLocation.latitude, position.latitude, 0.0001)
    assertEquals(testLocation.longitude, position.longitude, 0.0001)
  }

  @Test
  fun profileMarkerItem_implementationIsCorrect() {
    val marker = PublicProfileMarker(testPublicLocation)
    assertEquals("user123", marker.userId)
    assertEquals("TestUser", marker.displayName)
    assertEquals("user123", marker.markerId)
    assertEquals("TestUser", marker.getTitle())
    assertNull(marker.getSnippet())
  }

  @Test
  fun adjustedLocation_usesProvidedLocation() {
    val adjustedLoc = Location(46.5200, 6.6330, "Adjusted")
    val marker = PublicProfileMarker(testPublicLocation, adjustedLoc)
    val position = marker.getPosition()
    assertEquals(adjustedLoc.latitude, position.latitude, 0.0001)
    assertEquals(adjustedLoc.longitude, position.longitude, 0.0001)
  }
}
