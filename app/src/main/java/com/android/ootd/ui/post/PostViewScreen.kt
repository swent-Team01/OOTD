package com.android.ootd.ui.post

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.android.ootd.model.map.Location
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.model.user.User
import com.android.ootd.ui.map.LocationSelectionSection
import com.android.ootd.ui.map.LocationSelectionViewModel
import com.android.ootd.ui.map.LocationSelectionViewState
import com.android.ootd.ui.post.PostViewTestTags.FIRST_LIKE_BUTTON
import com.android.ootd.ui.post.items.commonTextFieldColors
import com.android.ootd.ui.theme.*
import com.android.ootd.ui.theme.Background
import com.android.ootd.utils.composables.ActionIconButton
import com.android.ootd.utils.composables.BackArrow
import com.android.ootd.utils.composables.ClickableProfileColumn
import com.android.ootd.utils.composables.ClickableProfileRow
import com.android.ootd.utils.composables.OOTDTopBar
import com.android.ootd.utils.composables.ShowText
import kotlinx.coroutines.launch

object PostViewTestTags {
  const val SCREEN = "postViewScreen"
  const val TOP_BAR = "postViewTopBar"
  const val BACK_BUTTON = "postViewBackButton"
  const val POST_IMAGE = "postViewImage"
  const val LIKE_ROW = "postViewLikeRow"
  const val LOADING_INDICATOR = "postViewLoading"
  const val SNACKBAR_HOST = "postViewErrorSnackbarHost"
  const val DROPDOWN_OPTIONS_MENU = "dropdownOptionsMenu"
  const val DESCRIPTION_COUNTER = "descriptionCounter"
  const val EDIT_DESCRIPTION_OPTION = "editDescriptionOption"
  const val DELETE_POST_OPTION = "deletePostOption"
  const val SAVE_EDITED_DESCRIPTION_BUTTON = "saveEditedDescriptionButton"
  const val CANCEL_EDITING_BUTTON = "cancelEditingButton"
  const val EDIT_DESCRIPTION_FIELD = "editDescriptionField"
  const val LIKED_USER_PROFILE_PREFIX = "likedUserProfile_"
  const val LIKED_USER_USERNAME_PREFIX = "likedUserUsername_"
  const val FIRST_LIKE_BUTTON = "firstLikeButton"
}

private const val MAX_DESCRIPTION_LENGTH = 100
private const val DEL_POST = "Delete Post"

private fun resolveLocation(
    post: OutfitPost,
    locationUiState: LocationSelectionViewState
): Location {
  return locationUiState.selectedLocation
      ?: post.location.copy(
          name = locationUiState.locationQuery.ifBlank { post.location.name },
          latitude = post.location.latitude,
          longitude = post.location.longitude)
}

/**
 * Screen for viewing a single post in full detail
 *
 * @param postId The unique identifier of the post to display
 * @param onBack Callback to navigate back
 * @param viewModel ViewModel for managing post view state
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostViewScreen(
    postId: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit = { onBack() },
    onProfileClick: (String) -> Unit = {},
    viewModel: PostViewViewModel = viewModel(factory = PostViewViewModelFactory(postId))
) {
  val uiState by viewModel.uiState.collectAsState()
  val snackBarHostState = remember { SnackbarHostState() }
  val coroutineScope = rememberCoroutineScope()

  LaunchedEffect(postId) { viewModel.loadPost(postId) }

  LaunchedEffect(uiState.error) {
    uiState.error?.let { errorMessage ->
      snackBarHostState.showSnackbar(
          message = errorMessage,
      )
    }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize().testTag(PostViewTestTags.SCREEN),
      containerColor = Background,
      topBar = {
        OOTDTopBar(
            modifier = Modifier.testTag(PostViewTestTags.TOP_BAR),
            centerText = "Post",
            leftComposable = {
              BackArrow(
                  onBackClick = onBack, modifier = Modifier.testTag(PostViewTestTags.BACK_BUTTON))
            })
      },
      snackbarHost = {
        SnackbarHost(
            hostState = snackBarHostState,
            modifier = Modifier.testTag(PostViewTestTags.SNACKBAR_HOST))
      }) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).background(Background)) {
          when {
            uiState.isLoading -> {
              CircularProgressIndicator(
                  color = Primary,
                  modifier =
                      Modifier.align(Alignment.Center).testTag(PostViewTestTags.LOADING_INDICATOR))
            }

            uiState.post != null -> {
              PostDetailsContent(
                  uiState = uiState,
                  onToggleLike = { viewModel.toggleLike() },
                  modifier = Modifier.fillMaxSize(),
                  viewModel = viewModel,
                  onProfileClick = onProfileClick,
                  onDeletePost = {
                    viewModel.deletePost(
                        onSuccess = { onDeleted() },
                        onError = { msg ->
                          coroutineScope.launch { snackBarHostState.showSnackbar(msg) }
                        })
                  })
            }
          }
        }
      }
}

/**
 * Composable displaying the details of a post
 *
 * @ param uiState The current UI state of the post view @ param onToggleLike Callback when the like
 * button is toggled @ param modifier Modifier for styling
 */
@Composable
fun PostDetailsContent(
    uiState: PostViewUiState,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PostViewViewModel,
    onProfileClick: (String) -> Unit,
    onDeletePost: () -> Unit
) {
  val post = uiState.post!!

  var isEditing by remember { mutableStateOf(false) }
  var editedDescription by remember { mutableStateOf(post.description) }
  val locationSelectionViewModel = remember { LocationSelectionViewModel() }
  val locationUiState by locationSelectionViewModel.uiState.collectAsState()
  var isLocationExpanded by remember { mutableStateOf(false) }

  LaunchedEffect(post.postUID) { locationSelectionViewModel.setLocation(post.location) }

  val onStartEditing =
      remember(post.postUID) {
        {
          isEditing = true
          editedDescription = post.description
        }
      }
  val onCancelEdit = remember { { isEditing = false } }
  val onSaveEdits: () -> Unit = {
    val chosenLocation = resolveLocation(post, locationUiState)
    viewModel.savePostEdits(editedDescription, chosenLocation)
    isEditing = false
  }
  val onDescriptionChange: (String) -> Unit = { description ->
    if (description.length <= MAX_DESCRIPTION_LENGTH) editedDescription = description
  }

  Column(modifier = modifier.verticalScroll(rememberScrollState())) {
    // Location box at the top
    LocationRow(
        location = post.location.name,
        isExpanded = isLocationExpanded,
        onToggleExpanded = { isLocationExpanded = !isLocationExpanded })

    Column(modifier = Modifier.padding(16.dp)) {
      PostHeroImage(
          imageUrl = post.outfitURL,
          likeCount = uiState.likedByUsers.size,
          isLiked = uiState.isLikedByCurrentUser,
          onToggleLike = onToggleLike)

      Spacer(Modifier.height(16.dp))

      PostOwnerSection(
          username = uiState.ownerUsername,
          profilePicture = uiState.ownerProfilePicture,
          ownerId = post.ownerId,
          onProfileClick = onProfileClick,
          onEditClicked = { onStartEditing() },
          onDeletePost = onDeletePost,
          isOwner = uiState.isOwner)

      Spacer(Modifier.height(16.dp))

      DescriptionSection(
          isEditing = isEditing,
          editedDescription = editedDescription,
          ownerUsername = uiState.ownerUsername,
          postDescription = post.description,
          onDescriptionChange = onDescriptionChange,
          onSave = onSaveEdits,
          onCancel = onCancelEdit,
          locationSelectionViewModel = locationSelectionViewModel)

      Spacer(Modifier.height(16.dp))

      PostLikeRow(
          isLiked = uiState.isLikedByCurrentUser,
          likeCount = uiState.likedByUsers.size,
          onToggleLike = onToggleLike)

      LikedUsersRow(likedUsers = uiState.likedByUsers, onProfileClick = onProfileClick)
    }
  }
}

@Composable
private fun DescriptionSection(
    isEditing: Boolean,
    editedDescription: String,
    ownerUsername: String?,
    postDescription: String,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    locationSelectionViewModel: LocationSelectionViewModel
) {
  if (isEditing) {
    DescriptionEditor(
        editedDescription = editedDescription,
        onDescriptionChange = onDescriptionChange,
        onSave = onSave,
        onCancel = onCancel,
        locationSelectionViewModel = locationSelectionViewModel)
  } else {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        color = Secondary) {
          PostDescription(
              username = ownerUsername,
              description = postDescription,
              modifier = Modifier.padding(16.dp).fillMaxWidth(),
              textAlign = TextAlign.Start)
        }
  }
}

@Composable
private fun DescriptionEditor(
    editedDescription: String,
    onDescriptionChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    locationSelectionViewModel: LocationSelectionViewModel
) {
  Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      tonalElevation = 0.dp,
      color = Secondary) {
        Column(modifier = Modifier.padding(12.dp)) {
          TextField(
              value = editedDescription,
              onValueChange = onDescriptionChange,
              modifier = Modifier.fillMaxWidth().testTag(PostViewTestTags.EDIT_DESCRIPTION_FIELD),
              colors = commonTextFieldColors(),
              trailingIcon = {
                Row {
                  ActionIconButton(
                      onClick = onSave,
                      modifier = Modifier.testTag(PostViewTestTags.SAVE_EDITED_DESCRIPTION_BUTTON),
                      icon = Icons.Default.Check,
                      contentDescription = "Save editing",
                      tint = Primary)
                  ActionIconButton(
                      onClick = onCancel,
                      modifier = Modifier.testTag(PostViewTestTags.CANCEL_EDITING_BUTTON),
                      icon = Icons.Outlined.Close,
                      contentDescription = "Cancel editing",
                      tint = OOTDerror)
                }
              })
          ShowText(
              text = "${editedDescription.length}/$MAX_DESCRIPTION_LENGTH characters",
              style = Typography.bodySmall,
              color = Primary,
              textAlign = TextAlign.End,
              modifier =
                  Modifier.align(Alignment.End)
                      .padding(top = 4.dp, end = 4.dp)
                      .testTag(PostViewTestTags.DESCRIPTION_COUNTER))

          Spacer(Modifier.height(12.dp))

          LocationSelectionSection(
              viewModel = locationSelectionViewModel,
              textGPSButton = "Use current location",
              textLocationField = "Location",
              onLocationSelect = { locationSelectionViewModel.setLocation(it) },
              onGPSClick = { locationSelectionViewModel.onLocationPermissionGranted() },
              modifier = Modifier.fillMaxWidth())
        }
      }
}

/**
 * Composable displaying the post owner's profile picture and username
 *
 * @ param username The username of the post owner @ param profilePicture The URL of the post
 * owner's profile picture
 */
@Composable
fun PostOwnerSection(
    username: String?,
    profilePicture: String?,
    ownerId: String,
    onProfileClick: (String) -> Unit,
    onEditClicked: () -> Unit,
    onDeletePost: () -> Unit,
    isOwner: Boolean = false
) {
  Box(Modifier.fillMaxWidth()) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      ClickableProfileRow(
          userId = ownerId,
          username = username ?: "Unknown User",
          profilePictureUrl = profilePicture ?: "",
          profileSize = 48.dp,
          onProfileClick = onProfileClick,
          usernameStyle = Typography.titleLarge,
          usernameColor = Primary,
          modifier = Modifier.weight(1f))

      // Dropdown menu for post options
      if (isOwner) DropdownMenuWithDetails(onEditClicked, onDeletePost)
    }
  }
}

/**
 * Composable displaying a dropdown menu with post options
 *
 * @param onEditClicked Callback when the edit option is clicked
 */
@Composable
fun DropdownMenuWithDetails(onEditClicked: () -> Unit, onDeleteClicked: () -> Unit) {
  var expanded by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }

  Box(modifier = Modifier.size(32.dp).padding(8.dp).fillMaxWidth()) {
    IconButton(
        onClick = { expanded = !expanded },
        modifier = Modifier.testTag(PostViewTestTags.DROPDOWN_OPTIONS_MENU)) {
          Icon(
              Icons.Default.MoreHoriz,
              contentDescription = "More options",
              tint = OnSurface,
              modifier = Modifier.size(24.dp))
        }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      // Edit option section
      DropdownMenuItem(
          text = { Text("Edit") },
          leadingIcon = {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = "Edit description",
            )
          },
          onClick = {
            onEditClicked()
            expanded = false
          },
          colors = MenuDefaults.itemColors(textColor = OnSurface, leadingIconColor = OnSurface),
          modifier = Modifier.testTag(PostViewTestTags.EDIT_DESCRIPTION_OPTION))

      HorizontalDivider()

      // Delete option section
      DropdownMenuItem(
          text = { Text(DEL_POST) },
          leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = DEL_POST) },
          onClick = {
            expanded = false
            showDeleteDialog = true
          },
          colors = MenuDefaults.itemColors(textColor = OOTDerror, leadingIconColor = OOTDerror),
          modifier = Modifier.testTag(PostViewTestTags.DELETE_POST_OPTION))
    }
  }

  if (showDeleteDialog) {
    AlertDialog(
        onDismissRequest = { showDeleteDialog = false },
        title = { Text(DEL_POST) },
        text = { Text("This will permanently delete this post. Continue?") },
        confirmButton = {
          TextButton(onClick = { onDeleteClicked() }) { Text("Delete", color = OOTDerror) }
        },
        dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } })
  }
}

/**
 * Composable displaying the like button and like count
 *
 * @ param isLiked Whether the post is liked by the current user @ param likeCount The total number
 * of likes on the post @ param onToggleLike Callback when the like button is toggled
 */
@Composable
fun PostLikeRow(isLiked: Boolean, likeCount: Int, onToggleLike: () -> Unit) {
  Row(
      modifier = Modifier.testTag(PostViewTestTags.LIKE_ROW),
      verticalAlignment = Alignment.CenterVertically) {
        ActionIconButton(
            onClick = onToggleLike,
            icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (isLiked) "Unlike" else "Like",
            tint = if (isLiked) MaterialTheme.colorScheme.error else OnSurface)

        Text(text = "$likeCount likes", style = Typography.bodyMedium, color = OnSurface)
      }
}

@Composable
fun LocationRow(location: String, isExpanded: Boolean, onToggleExpanded: () -> Unit) {
  Surface(
      modifier = Modifier.fillMaxWidth().clickable { onToggleExpanded() }.animateContentSize(),
      color = Background) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              Icon(
                  imageVector = Icons.Outlined.LocationOn,
                  contentDescription = "Location",
                  tint = Primary,
                  modifier = Modifier.size(20.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = location.ifBlank { "No location" },
                    style = Typography.bodyMedium,
                    color = OnSurface,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    overflow = if (isExpanded) TextOverflow.Visible else TextOverflow.Ellipsis)

                if (isExpanded && location.isNotBlank()) {
                  Spacer(Modifier.height(4.dp))
                  Text(text = "Tap to collapse", style = Typography.bodySmall, color = Tertiary)
                }
              }

              Icon(
                  imageVector =
                      if (isExpanded) Icons.Filled.KeyboardArrowUp
                      else Icons.Filled.KeyboardArrowDown,
                  contentDescription = if (isExpanded) "Collapse" else "Expand",
                  tint = Tertiary,
                  modifier = Modifier.size(24.dp))
            }
      }
}

@Composable
private fun ImagePlaceholder() {
  Box(
      modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f).background(Secondary),
      contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Primary)
      }
}

@Composable
private fun ImageErrorState() {
  Box(
      modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f).background(Secondary),
      contentAlignment = Alignment.Center) {
        Text("Failed to load image", color = OnSurface)
      }
}

@Composable
private fun BoxScope.ImageGradientOverlay() {
  Box(
      modifier =
          Modifier.matchParentSize()
              .background(
                  Brush.verticalGradient(
                      colors =
                          listOf(
                              Color.Transparent,
                              Color.Black.copy(alpha = 0.25f),
                              Color.Black.copy(alpha = 0.55f)))))
}

@Composable
private fun BoxScope.LikeChip(likeCount: Int, isLiked: Boolean, onToggleLike: () -> Unit) {
  Row(
      modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically) {
        val icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
        AssistChip(
            onClick = onToggleLike,
            label = { Text("$likeCount likes") },
            leadingIcon = {
              Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            colors =
                AssistChipDefaults.assistChipColors(
                    containerColor = Color.White.copy(alpha = 0.2f),
                    labelColor = Color.White,
                    leadingIconContentColor = Color.White),
            modifier = Modifier.testTag(FIRST_LIKE_BUTTON))
      }
}

@Composable
private fun PostHeroImage(
    imageUrl: String,
    likeCount: Int,
    isLiked: Boolean,
    onToggleLike: () -> Unit
) {
  Card(
      shape = RoundedCornerShape(24.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
      modifier = Modifier.fillMaxWidth().testTag(PostViewTestTags.POST_IMAGE)) {
        Box(modifier = Modifier.fillMaxWidth()) {
          SubcomposeAsyncImage(
              model = imageUrl,
              contentDescription = "Post image",
              contentScale = ContentScale.FillWidth,
              modifier = Modifier.fillMaxWidth()) {
                when (painter.state) {
                  is AsyncImagePainter.State.Loading -> ImagePlaceholder()
                  is AsyncImagePainter.State.Error -> ImageErrorState()
                  else -> SubcomposeAsyncImageContent()
                }
              }
          ImageGradientOverlay()
          LikeChip(likeCount, isLiked, onToggleLike)
        }
      }
}

/**
 * Composable displaying the post description
 *
 * @ param description The description text of the post
 */
@Composable
fun PostDescription(
    username: String?,
    description: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
  if (description.isNotBlank()) {
    val annotated = buildAnnotatedString {
      if (!username.isNullOrBlank()) {
        withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold, color = Primary)) {
          append(username)
        }
        append(" ")
      }
      withStyle(style = SpanStyle(color = Primary)) { append(description) }
    }
    Text(
        text = annotated,
        style = Typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
        modifier = modifier,
        textAlign = textAlign)
  }
}

/**
 * Composable displaying a horizontal row of users who liked the post
 *
 * @ param likedUsers List of users who liked the post
 */
@Composable
fun LikedUsersRow(likedUsers: List<User>, onProfileClick: (String) -> Unit) {
  LazyRow(
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        items(likedUsers.size) { index ->
          val user = likedUsers[index]

          ClickableProfileColumn(
              userId = user.uid,
              username = user.username,
              profilePictureUrl = user.profilePicture,
              profileSize = 48.dp,
              onProfileClick = onProfileClick,
              usernameStyle = Typography.labelSmall,
              usernameColor = Primary,
              modifier = Modifier.width(64.dp),
              profileTestTag = PostViewTestTags.LIKED_USER_PROFILE_PREFIX + user.uid,
              usernameTestTag = PostViewTestTags.LIKED_USER_USERNAME_PREFIX + user.uid)
        }
      }
}
