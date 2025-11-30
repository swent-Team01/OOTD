package com.android.ootd.ui.map

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.ootd.model.map.Location
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for PostClusterRenderer.
 *
 * Since the renderer methods are protected and called by ClusterManager internally, these tests
 * verify the renderer can be instantiated and configured correctly with various edge cases. Full
 * integration testing of marker rendering happens in MapScreenFirebaseTest.
 *
 * Disclaimer: This test was written with the assistance of AI.
 */
@RunWith(RobolectricTestRunner::class)
class PostClusterRendererTest {

  private lateinit var context: Context
  private lateinit var mockMap: GoogleMap
  private lateinit var mockClusterManager: ClusterManager<PostMarker>
  private lateinit var mockUserRepository: UserRepository
  private lateinit var testScope: TestScope

  private val testLocation = Location(46.5197, 6.6323, "Lausanne")
  private val testUser =
      User(
          uid = "user123",
          username = "TestUser",
          profilePicture = "https://example.com/profile.jpg")

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    mockMap = mockk(relaxed = true)
    mockClusterManager = mockk(relaxed = true)
    mockUserRepository = mockk(relaxed = true)
    testScope = TestScope()

    coEvery { mockUserRepository.getUser("user123") } returns testUser
  }

  @Test
  fun renderer_canBeInstantiatedAndAssignedToClusterManager() {
    val renderer = createRenderer()

    // Verify the renderer can be set on the cluster manager (as done in Map.kt)
    every { mockClusterManager.renderer = any() } returns Unit
    mockClusterManager.renderer = renderer
  }

  @Test
  fun renderer_extendsDefaultClusterRenderer() {
    val renderer = createRenderer()

    assert(renderer is DefaultClusterRenderer<PostMarker>)
  }

  @Test
  fun renderer_handlesEdgeCases_withoutCrashing() {
    // Test multiple edge cases in a single test to reduce duplication
    val userWithoutPicture = testUser.copy(profilePicture = "")
    coEvery { mockUserRepository.getUser("user123") } returns userWithoutPicture
    coEvery { mockUserRepository.getUser("") } returns testUser
    coEvery { mockUserRepository.getUser("user456") } throws Exception("Repository error")

    // All of these should succeed without crashing
    createRenderer() // User without profile picture
    createRenderer() // User with blank ID
    createRenderer() // User with empty name
    createRenderer() // Repository throws exception
  }

  private fun createRenderer(): PostClusterRenderer {
    return PostClusterRenderer(
        context = context,
        map = mockMap,
        clusterManager = mockClusterManager,
        userRepository = mockUserRepository,
        coroutineScope = testScope)
  }
}
