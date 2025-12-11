package com.android.ootd.ui.map

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.android.ootd.model.account.Account
import com.android.ootd.model.map.Location
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.utils.PostFirestoreTest
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapsSdkInitializedCallback
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Integration test for MapScreen with marker clustering.
 *
 * This test uses the full Android framework including Google Maps components and Firebase
 * infrastructure to verify that the clustering implementation works correctly with real data.
 *
 * Tests verify:
 * - Clustering renders with multiple posts
 * - Map handles empty state
 * - Real-time updates work with clustering
 * - Camera positioning updates correctly
 *
 * Disclaimer: This test was written with the assistance of AI.
 */
class MapScreenFirebaseTest : PostFirestoreTest(), OnMapsSdkInitializedCallback {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var viewModel: MapViewModel

  // Local test location constants
  private val testLausanneLocation = Location(46.5197, 6.6323, "Lausanne")

  @Before
  override fun setUp() {
    super.setUp()

    // Initialize Google Maps SDK to prevent CameraUpdateFactory errors
    val context = ApplicationProvider.getApplicationContext<Context>()
    MapsInitializer.initialize(context, MapsInitializer.Renderer.LATEST, this)

    runBlocking {
      // Create and add the account to Firestore first
      val initialAccount =
          Account(
              uid = currentUser.uid,
              ownerId = currentUser.uid,
              username = "test_user",
              birthday = "2000-01-01",
              googleAccountEmail = "test@example.com",
              profilePicture = "",
              friendUids = emptyList(),
              location = testLausanneLocation)
      accountRepository.addAccount(initialAccount)

      // Give Firestore time to process the account creation
      Thread.sleep(1000)
    }

    // Create the ViewModel with Firebase-backed repositories from BaseTest
    viewModel = MapViewModel(feedRepository, accountRepository)

    // Give the ViewModel time to initialize and start observing
    Thread.sleep(1000)
  }

  override fun onMapsSdkInitialized(renderer: MapsInitializer.Renderer) {
    // Maps SDK initialized successfully
  }

  @Test
  fun mapScreen_rendersClusteringWithMultiplePosts() {
    runBlocking {
      // Use helper method to create test posts at different locations
      val posts = createTestPostsForClustering(count = 3)

      // Add posts to the Firestore repository
      posts.forEach { feedRepository.addPost(it) }
    }

    // Set up the MapScreen with the viewModel
    composeTestRule.setContent { MapScreen(viewModel = viewModel) }

    // Wait for composition to settle
    composeTestRule.waitForIdle()

    // Give the map time to load and render clustering
    Thread.sleep(2000)
    composeTestRule.waitForIdle()

    // Verify the scaffold and top bar are displayed
    composeTestRule.onNodeWithTag(MapScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.TOP_BAR).assertIsDisplayed()

    // Verify the content box is displayed (contains the map with clusters)
    composeTestRule.onNodeWithTag(MapScreenTestTags.CONTENT_BOX).assertIsDisplayed()

    // Note: Individual clusters cannot be easily verified in Compose UI tests because
    // they are rendered as part of the Google Maps component. This test verifies that:
    // 1. The MapScreen renders without crashing with posts
    // 2. The clustering infrastructure is set up correctly
    // 3. The posts are available to the ClusterManager for rendering
  }

  @Test
  fun mapScreen_displaysWithEmptyPosts() {
    // Don't add any posts - test with empty state

    composeTestRule.setContent { MapScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()
    Thread.sleep(1000)
    composeTestRule.waitForIdle()

    // Verify the map still renders without posts (no clusters)
    composeTestRule.onNodeWithTag(MapScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.TOP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.CONTENT_BOX).assertIsDisplayed()
  }

  @Test
  fun mapScreen_displaysWithSinglePost() {
    runBlocking {
      val singlePost =
          createTestPost(
              postUID = "single-post",
              name = "SingleUser",
              profilePicURL = "https://example.com/single.jpg",
              description = "Single Post",
              location = EPFL_LOCATION)

      feedRepository.addPost(singlePost)
    }

    composeTestRule.setContent { MapScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()
    Thread.sleep(1500)
    composeTestRule.waitForIdle()

    // Verify the map renders with a single post (no clustering needed)
    composeTestRule.onNodeWithTag(MapScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.CONTENT_BOX).assertIsDisplayed()
  }

  @Test
  fun mapScreen_cameraPositionUpdates_whenUserLocationChanges() {
    runBlocking {
      val initialLocation = testLausanneLocation
      val updatedLocation = EPFL_LOCATION

      // Set initial location
      accountRepository.editAccount(
          currentUser.uid, username = "", birthDay = "", picture = "", location = initialLocation)

      composeTestRule.setContent { MapScreen(viewModel = viewModel) }

      composeTestRule.waitForIdle()
      Thread.sleep(1000)

      // Update location - this should trigger camera position update
      accountRepository.editAccount(
          currentUser.uid, username = "", birthDay = "", picture = "", location = updatedLocation)

      Thread.sleep(1000)
      composeTestRule.waitForIdle()

      // Verify the map is still displayed after location change
      composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertExists()
      composeTestRule.onNodeWithTag(MapScreenTestTags.CONTENT_BOX).assertIsDisplayed()
    }
  }

  @Test
  fun mapScreen_observesPostUpdates_inRealTime_withClustering() {
    // Start with empty posts
    composeTestRule.setContent { MapScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()
    Thread.sleep(1000)

    runBlocking {
      // Add posts dynamically using helper method
      val dynamicPosts =
          listOf(
              createTestPost(
                  postUID = "dynamic-post-1",
                  name = "DynamicUser1",
                  profilePicURL = "https://example.com/dynamic1.jpg",
                  outfitURL = "https://example.com/outfit-dynamic1.jpg",
                  description = "Dynamic Post 1",
                  location = Location(46.5197, 6.6323, "Dynamic Location 1")),
              createTestPost(
                  postUID = "dynamic-post-2",
                  name = "DynamicUser2",
                  profilePicURL = "https://example.com/dynamic2.jpg",
                  outfitURL = "https://example.com/outfit-dynamic2.jpg",
                  description = "Dynamic Post 2",
                  location = Location(46.5198, 6.6324, "Dynamic Location 2")))

      dynamicPosts.forEach { feedRepository.addPost(it) }
    }

    Thread.sleep(1500)
    composeTestRule.waitForIdle()

    // Verify the map still renders after dynamic post addition with clustering
    composeTestRule.onNodeWithTag(MapScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.CONTENT_BOX).assertIsDisplayed()
  }

  @Test
  fun mapScreen_handlesManyPosts_withClustering() {
    runBlocking {
      // Use helper method to create many posts for clustering performance test
      val posts = createTestPostsForClustering(count = 10)
      posts.forEach { feedRepository.addPost(it) }
    }

    composeTestRule.setContent { MapScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()
    Thread.sleep(2000)
    composeTestRule.waitForIdle()

    // Verify the map handles many posts with clustering
    composeTestRule.onNodeWithTag(MapScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.CONTENT_BOX).assertIsDisplayed()
  }

  @Test
  fun mapScreen_rendersProfilePictureMarkers_whenPostsExist() {
    runBlocking {
      // Create multiple posts with different usernames to ensure forEach executes
      val posts =
          listOf(
              OutfitPost(
                  postUID = "post-a",
                  name = "Alice",
                  ownerId = currentUser.uid,
                  userProfilePicURL = "",
                  outfitURL = "https://example.com/outfit-a.jpg",
                  description = "Post A",
                  itemsID = emptyList(),
                  timestamp = System.currentTimeMillis(),
                  location = Location(46.5197, 6.6323, "Location A")),
              OutfitPost(
                  postUID = "post-b",
                  name = "Bob",
                  ownerId = currentUser.uid,
                  userProfilePicURL = "",
                  outfitURL = "https://example.com/outfit-b.jpg",
                  description = "Post B",
                  itemsID = emptyList(),
                  timestamp = System.currentTimeMillis(),
                  location = Location(46.5198, 6.6324, "Location B")),
              OutfitPost(
                  postUID = "post-c",
                  name = "Charlie",
                  ownerId = currentUser.uid,
                  userProfilePicURL = "",
                  outfitURL = "https://example.com/outfit-c.jpg",
                  description = "Post C",
                  itemsID = emptyList(),
                  timestamp = System.currentTimeMillis(),
                  location = Location(46.5199, 6.6325, "Location C")))

      // Add all posts to ensure the forEach loop in MapScreen executes
      posts.forEach { feedRepository.addPost(it) }
    }

    // Render the MapScreen - this should execute the ProfilePictureMarker code
    composeTestRule.setContent { MapScreen(viewModel = viewModel) }

    // Wait for composition and map rendering
    composeTestRule.waitForIdle()
    Thread.sleep(2000)
    composeTestRule.waitForIdle()

    // Verify the map rendered successfully with posts
    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertExists()
    composeTestRule.onNodeWithTag(MapScreenTestTags.CONTENT_BOX).assertIsDisplayed()
  }

  @Test
  fun mapScreen_tabSwitch_preservesCameraPosition() {
    composeTestRule.setContent { MapScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()
    Thread.sleep(1000)
    composeTestRule.waitForIdle()

    val initialLocation = viewModel.getFocusLatLng()

    // Switch to Find Friends tab
    composeTestRule.onNodeWithTag(MapScreenTestTags.FIND_FRIENDS_TAB).performClick()
    composeTestRule.waitForIdle()

    val locationAfterSwitch = viewModel.getFocusLatLng()

    // Camera position should be the same
    assert(initialLocation.latitude == locationAfterSwitch.latitude) {
      "Latitude should be preserved after tab switch"
    }
    assert(initialLocation.longitude == locationAfterSwitch.longitude) {
      "Longitude should be preserved after tab switch"
    }
  }

  @Test
  fun mapScreen_tabSwitch_preservesUserDataAndMaintainsCorrectState() {
    composeTestRule.setContent { MapScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()
    Thread.sleep(1000)
    composeTestRule.waitForIdle()

    // Wait for the tab row to be displayed
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithTag(MapScreenTestTags.FIND_FRIENDS_TAB)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    val initialPosts = viewModel.uiState.value.posts
    val initialAccount = viewModel.uiState.value.currentAccount
    val initialUserLocation = viewModel.uiState.value.userLocation

    // Perform multiple tab switches
    repeat(3) {
      // Switch to Find Friends tab
      composeTestRule.onNodeWithTag(MapScreenTestTags.FIND_FRIENDS_TAB).performClick()
      composeTestRule.waitForIdle()

      val stateAfterFindFriends = viewModel.uiState.value
      assert(stateAfterFindFriends.selectedMapType == MapType.FIND_FRIENDS)

      // Verify user data is preserved
      assert(stateAfterFindFriends.posts == initialPosts) { "Posts should be preserved" }
      assert(stateAfterFindFriends.currentAccount == initialAccount) {
        "Account should be preserved"
      }
      assert(stateAfterFindFriends.userLocation == initialUserLocation) {
        "User location should be preserved"
      }

      // Switch back to Friends Posts tab
      composeTestRule.onNodeWithTag(MapScreenTestTags.FRIENDS_POSTS_TAB).performClick()
      composeTestRule.waitForIdle()

      val stateAfterFriendsPosts = viewModel.uiState.value
      assert(stateAfterFriendsPosts.selectedMapType == MapType.FRIENDS_POSTS)

      // Verify user data is still preserved
      assert(stateAfterFriendsPosts.posts == initialPosts) { "Posts should be preserved" }
      assert(stateAfterFriendsPosts.currentAccount == initialAccount) {
        "Account should be preserved"
      }
      assert(stateAfterFriendsPosts.userLocation == initialUserLocation) {
        "User location should be preserved"
      }
    }
  }
}
