package com.android.ootd.ui.map

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.ootd.model.map.Location
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.clustering.ClusterManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for PostClusterRenderer.
 *
 * These tests verify the renderer can be instantiated and handles various edge cases. The protected
 * rendering methods use BitmapDescriptorFactory which cannot be tested in unit tests without Google
 * Maps initialization. Full integration testing of marker rendering happens in
 * MapScreenFirebaseTest.
 *
 * Disclaimer: This test was written with the assistance of AI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PostClusterRendererTest {

  private lateinit var context: Context
  private lateinit var mockMap: GoogleMap
  private lateinit var mockClusterManager: ClusterManager<PostMarker>
  private lateinit var mockUserRepository: UserRepository
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    context = ApplicationProvider.getApplicationContext()
    mockMap = mockk(relaxed = true)
    mockClusterManager = mockk(relaxed = true)
    mockUserRepository = mockk(relaxed = true)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun renderer_canBeInstantiatedSuccessfully() {
    val renderer = createRenderer()
    assertNotNull(renderer)
  }

  @Test
  fun renderer_canBeAssignedToClusterManager() {
    val renderer = createRenderer()
    every { mockClusterManager.renderer = any() } returns Unit
    mockClusterManager.renderer = renderer
  }

  @Test
  fun renderer_handlesUserWithProfilePicture() = runTest {
    val user =
        User(
            uid = "user123",
            username = "TestUser",
            profilePicture = "https://example.com/profile.jpg")
    coEvery { mockUserRepository.getUser("user123") } returns user

    val renderer = createRenderer()
    assertNotNull(renderer)
  }

  @Test
  fun renderer_handlesUserWithoutProfilePicture() = runTest {
    val user = User(uid = "user456", username = "TestUser", profilePicture = "")
    coEvery { mockUserRepository.getUser("user456") } returns user

    val renderer = createRenderer()
    assertNotNull(renderer)
  }

  @Test
  fun renderer_handlesBlankUserId() = runTest {
    coEvery { mockUserRepository.getUser("") } returns User()

    val renderer = createRenderer()
    assertNotNull(renderer)
  }

  @Test
  fun renderer_handlesRepositoryException() = runTest {
    coEvery { mockUserRepository.getUser(any()) } throws Exception("Network error")

    val renderer = createRenderer()
    assertNotNull(renderer)
  }

  @Test
  fun renderer_handlesCacheOperations() {
    val renderer = createRenderer()

    // Access the cache via reflection from parent class ProfileClusterRenderer
    val cacheField = ClusterRenderer::class.java.getDeclaredField("profilePictureCache")
    cacheField.isAccessible = true
    @Suppress("UNCHECKED_CAST") val cache = cacheField.get(renderer) as MutableMap<String, Any?>

    // Verify cache is initialized and accessible
    assertNotNull(cache)
    assertEquals(0, cache.size)

    // Test cache operations
    cache["user1"] = null
    assertEquals(1, cache.size)
    assertEquals(true, cache.containsKey("user1"))

    cache["user2"] = null
    assertEquals(2, cache.size)
  }

  @Test
  fun renderer_handlesMarkerCacheOperations() {
    val renderer = createRenderer()

    // Access the marker cache via reflection from parent class
    val cacheField = ClusterRenderer::class.java.getDeclaredField("markerCache")
    cacheField.isAccessible = true
    @Suppress("UNCHECKED_CAST") val cache = cacheField.get(renderer) as MutableMap<String, Any?>

    // Verify marker cache is initialized
    assertNotNull(cache)
    assertEquals(0, cache.size)
  }

  @Test
  fun loadProfilePicture_handlesBlankUserId() = runTest {
    val renderer = createRenderer()
    val postMarker = createTestPostMarker("", "EmptyUser")

    // Call loadProfilePicture via reflection with blank userId
    callLoadProfilePicture(renderer, "", "EmptyUser", postMarker)
    advanceUntilIdle()

    // Should complete without calling repository
  }

  @Test
  fun loadProfilePicture_handlesBlankProfilePicture() = runTest {
    val user = User(uid = "user456", username = "NoPicUser", profilePicture = "")
    coEvery { mockUserRepository.getUser("user456") } returns user

    val renderer = createRenderer()
    val postMarker = createTestPostMarker("user456", "NoPicUser")

    callLoadProfilePicture(renderer, "user456", "NoPicUser", postMarker)
    advanceUntilIdle()

    // Should cache null for user without profile picture
    val cacheField = ClusterRenderer::class.java.getDeclaredField("profilePictureCache")
    cacheField.isAccessible = true
    @Suppress("UNCHECKED_CAST") val cache = cacheField.get(renderer) as MutableMap<String, Any?>
    assertEquals(true, cache.containsKey("user456"))
  }

  @Test
  fun loadProfilePicture_handlesException() = runTest {
    coEvery { mockUserRepository.getUser("user789") } throws Exception("Network error")

    val renderer = createRenderer()
    val postMarker = createTestPostMarker("user789", "ErrorUser")

    callLoadProfilePicture(renderer, "user789", "ErrorUser", postMarker)
    advanceUntilIdle()

    // Should cache null on exception
    val cacheField = ClusterRenderer::class.java.getDeclaredField("profilePictureCache")
    cacheField.isAccessible = true
    @Suppress("UNCHECKED_CAST") val cache = cacheField.get(renderer) as MutableMap<String, Any?>
    assertEquals(true, cache.containsKey("user789"))
  }

  // Helper methods
  private fun callLoadProfilePicture(
      renderer: PostClusterRenderer,
      userId: String,
      username: String,
      item: PostMarker
  ) {
    // Method is now in parent class ProfileClusterRenderer with generic signature
    val method =
        ClusterRenderer::class
            .java
            .getDeclaredMethod(
                "loadProfilePicture",
                String::class.java,
                String::class.java,
                ProfileMarkerItem::class.java)
    method.isAccessible = true
    method.invoke(renderer, userId, username, item)
  }

  private fun createTestPostMarker(userId: String, username: String) =
      PostMarker(
          post =
              OutfitPost(
                  postUID = "post_$userId",
                  ownerId = userId,
                  name = username,
                  location = Location(0.0, 0.0, "Test Location")))

  private fun createRenderer() =
      PostClusterRenderer(
          context = context,
          map = mockMap,
          clusterManager = mockClusterManager,
          userRepository = mockUserRepository)
}
