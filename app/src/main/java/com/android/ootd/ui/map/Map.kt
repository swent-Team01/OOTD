package com.android.ootd.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.ui.map.MapScreenTestTags.getTestTagForPostMarker
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: MapViewModel = viewModel(), onBack: () -> Unit = {}) {
  val uiState by viewModel.uiState.collectAsState()

  Scaffold(
      modifier = Modifier.testTag(MapScreenTestTags.SCREEN),
      topBar = {
        CenterAlignedTopAppBar(
            title = {
              Text(
                  text = "MAP",
                  style =
                      MaterialTheme.typography.displayLarge.copy(
                          fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                  modifier = Modifier.testTag(MapScreenTestTags.TOP_BAR_TITLE))
            },
            navigationIcon = {
              Box(modifier = Modifier.padding(start = 4.dp), contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = onBack, modifier = Modifier.testTag(MapScreenTestTags.BACK_BUTTON)) {
                      Icon(
                          imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                          contentDescription = "Back",
                          tint = MaterialTheme.colorScheme.tertiary)
                    }
              }
            },
            modifier = Modifier.testTag(MapScreenTestTags.TOP_BAR))
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
                      viewModel.getPostsWithAdjustedLocations().forEach { postWithLocation ->
                        ProfilePictureMarker(
                            username = postWithLocation.post.name,
                            imageUrl = postWithLocation.post.userProfilePicURL,
                            location = postWithLocation.adjustedLocation,
                            tag = getTestTagForPostMarker(postWithLocation.post.postUID),
                            overlappingCount = postWithLocation.overlappingCount,
                            onClick = { /* TODO: Handle marker click if needed */ })
                      }
                    }
              }
            }
      })
}
