package com.android.ootd.ui.map

import androidx.compose.foundation.layout.Box
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
import com.android.ootd.ui.map.MapScreenTestTags.getTestTagForPostMarker
import com.android.ootd.model.user.UserRepositoryProvider
import com.android.ootd.utils.composables.BackArrow
import com.android.ootd.utils.composables.OOTDTopBar
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapEffect
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.rememberCameraPositionState

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
  const val BACK_BUTTON = "backButton"
  const val CONTENT_BOX = "contentBox"

  fun getTestTagForPostMarker(postId: String): String = "postMarker_$postId"
}

@OptIn(ExperimentalMaterial3Api::class, MapsComposeExperimentalApi::class)
@Composable
fun MapScreen(viewModel: MapViewModel = viewModel(), onBack: () -> Unit = {}) {
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
                // Camera position centered on user's location
                val cameraPositionState = rememberCameraPositionState {
                  position = CameraPosition.fromLatLngZoom(viewModel.getUserLatLng(), 12f)
                }

                // Update camera position when user location changes
                androidx.compose.runtime.LaunchedEffect(uiState.userLocation) {
                  cameraPositionState.animate(
                      CameraUpdateFactory.newLatLngZoom(viewModel.getUserLatLng(), 12f))
                }

                GoogleMap(
                    modifier = Modifier.fillMaxSize().testTag(MapScreenTestTags.GOOGLE_MAP_SCREEN),
                    cameraPositionState = cameraPositionState) {

                      // Set up marker clustering
                      MapEffect(key1 = uiState.posts) { map ->
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
                          // TODO: navigate to see post
                          true
                        }

                        // Set up map listeners to forward to cluster manager
                        map.setOnCameraIdleListener(clusterManager)
                        map.setOnMarkerClickListener(clusterManager)

                        // Clear existing items and add new ones with adjusted locations
                        clusterManager.clearItems()
                        val postsWithAdjusted = viewModel.getPostsWithAdjustedLocations()
                        val clusterItems =
                            postsWithAdjusted.map { PostMarker(it.post, it.adjustedLocation) }
                        clusterManager.addItems(clusterItems)
                        clusterManager.cluster()
                      }
                    }
              }
            }
      })
}
