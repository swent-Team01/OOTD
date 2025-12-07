package com.android.ootd.ui.map

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.model.user.UserRepositoryProvider
import com.android.ootd.utils.composables.OOTDTabRow
import com.android.ootd.utils.composables.OOTDTopBar
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.CoroutineScope

/*
 * Disclaimer: This takes inspiration from
 * https://developers.google.com/maps/documentation/android-sdk/utility/marker-clustering. Additionally,
 * AI was used to assist in the development of this class.
 */

object MapScreenTestTags {
  const val SCREEN = "mapScreenScaffold"
  const val GOOGLE_MAP_SCREEN = "mapScreen"
  const val LOADING_INDICATOR = "loadingIndicator"
  const val TOP_BAR = "topBar"
  const val TOP_BAR_TITLE = "topBarTitle"
  const val CONTENT_BOX = "contentBox"
  const val TAB_ROW = "mapTabRow"
  const val FRIENDS_POSTS_TAB = "friendsPostsTab"
  const val FIND_FRIENDS_TAB = "findFriendsTab"

  fun getTestTagForPostMarker(postId: String): String = "postMarker_$postId"
}

@SuppressLint("PotentialBehaviorOverride")
@OptIn(ExperimentalMaterial3Api::class, MapsComposeExperimentalApi::class)
@Composable
fun MapScreen(viewModel: MapViewModel = viewModel(), onPostClick: (String) -> Unit = {}) {
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()

  Scaffold(
      modifier = Modifier.testTag(MapScreenTestTags.SCREEN),
      topBar = {
        OOTDTopBar(
            modifier = Modifier.testTag(MapScreenTestTags.TOP_BAR),
            textModifier = Modifier.testTag(MapScreenTestTags.TOP_BAR_TITLE),
            centerText = "MAP")
      },
      content = { paddingValues ->
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .padding(paddingValues)
                    .testTag(MapScreenTestTags.CONTENT_BOX)) {
              if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier =
                        Modifier.align(Alignment.Center)
                            .testTag(MapScreenTestTags.LOADING_INDICATOR))
              } else {
                Column(modifier = Modifier.fillMaxSize()) {
                  // Tab selector for switching between maps
                  OOTDTabRow(
                      selectedTabIndex =
                          if (uiState.selectedMapType == MapType.FRIENDS_POSTS) 0 else 1,
                      tabs = listOf("Friends Posts", "Find Friends"),
                      onTabClick = { index ->
                        viewModel.setMapType(
                            if (index == 0) MapType.FRIENDS_POSTS else MapType.FIND_FRIENDS)
                      },
                      modifier = Modifier.testTag(MapScreenTestTags.TAB_ROW),
                      tabModifiers =
                          listOf(
                              Modifier.testTag(MapScreenTestTags.FRIENDS_POSTS_TAB),
                              Modifier.testTag(MapScreenTestTags.FIND_FRIENDS_TAB)))

                  // Camera position centered on focus location (either provided location or user's
                  // location)
                  val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(viewModel.getFocusLatLng(), 12f)
                  }

                  // Update camera position when focus location changes
                  androidx.compose.runtime.LaunchedEffect(
                      uiState.focusLocation, uiState.userLocation) {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(viewModel.getFocusLatLng(), 12f))
                      }

                  // Display the appropriate map based on selected tab
                  when (uiState.selectedMapType) {
                    MapType.FRIENDS_POSTS -> {
                      FriendsPostsMap(
                          modifier = Modifier.fillMaxSize(),
                          cameraPositionState = cameraPositionState,
                          posts = uiState.posts,
                          viewModel = viewModel,
                          onPostClick = onPostClick,
                          context = context,
                          coroutineScope = coroutineScope)
                    }
                    MapType.FIND_FRIENDS -> {
                      FindFriendsMap(
                          modifier = Modifier.fillMaxSize(),
                          cameraPositionState = cameraPositionState)
                    }
                  }
                }
              }
            }
      })
}

/** Map displaying friends' outfit posts with clustering. */
@SuppressLint("PotentialBehaviorOverride")
@OptIn(MapsComposeExperimentalApi::class)
@Composable
private fun FriendsPostsMap(
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState,
    posts: List<OutfitPost>,
    viewModel: MapViewModel,
    onPostClick: (String) -> Unit,
    context: Context,
    coroutineScope: CoroutineScope
) {
  GoogleMap(
      modifier = modifier.testTag(MapScreenTestTags.GOOGLE_MAP_SCREEN),
      cameraPositionState = cameraPositionState) {

        // Set up marker clustering
        MapEffect(key1 = posts) { map ->
          val clusterManager = ClusterManager<PostMarker>(context, map)

          // Set custom renderer for clusters
          val renderer =
              PostClusterRenderer(
                  context = context,
                  map = map,
                  clusterManager = clusterManager,
                  userRepository = UserRepositoryProvider.repository,
                  coroutineScope = coroutineScope)
          // Assign renderer to correctly display markers
          clusterManager.renderer = renderer

          // Set cluster click listener for when clicking on clusters
          clusterManager.setOnClusterClickListener { cluster ->
            // Zoom into the cluster
            val builder = LatLngBounds.builder()
            cluster.items.forEach { builder.include(it.position) }
            val bounds = builder.build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            true
          }

          // Set item click listener
          clusterManager.setOnClusterItemClickListener { item ->
            onPostClick(item.post.postUID)
            true
          }

          // Set up map listeners to forward to cluster manager
          map.setOnCameraIdleListener(clusterManager)
          map.setOnMarkerClickListener(clusterManager)

          // Clear existing items and add new ones with adjusted locations
          clusterManager.clearItems()
          val postsWithAdjusted = viewModel.getPostsWithAdjustedLocations()
          val clusterItems = postsWithAdjusted.map { PostMarker(it.post, it.adjustedLocation) }
          clusterManager.addItems(clusterItems)
          clusterManager.cluster()
        }
      }
}

/** Map displaying public profiles (currently empty, will be implemented later). */
@OptIn(MapsComposeExperimentalApi::class)
@Composable
private fun FindFriendsMap(
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState
) {
  GoogleMap(
      modifier = modifier.testTag(MapScreenTestTags.GOOGLE_MAP_SCREEN),
      cameraPositionState = cameraPositionState) {
        // For now, just display an empty map
      }
}
