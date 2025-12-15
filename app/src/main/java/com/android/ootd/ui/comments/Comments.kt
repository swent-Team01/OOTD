package com.android.ootd.ui.comments

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.android.ootd.model.posts.Comment
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.ui.camera.CameraScreen
import com.android.ootd.ui.theme.Background
import com.android.ootd.ui.theme.OnSurface
import com.android.ootd.ui.theme.OnSurfaceVariant
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.Tertiary
import com.android.ootd.ui.theme.Typography
import com.android.ootd.utils.composables.ImageSelectionDialog
import com.android.ootd.utils.composables.ProfilePicture
import kotlinx.coroutines.launch

object CommentScreenTestTags {
  const val COMMENT_BOTTOM_SHEET = "commentBottomSheet"
  const val CLOSE_COMMENTS_BUTTON = "closeCommentsButton"
  const val COMMENTS_LIST = "commentsList"
  const val COMMENT_ITEM = "commentItem"
  const val REACTION_IMAGE = "reactionImage"
  const val DELETE_COMMENT_BUTTON = "deleteCommentButton"
  const val ADD_COMMENT_SECTION = "addCommentSection"
  const val COMMENT_TEXT_FIELD = "commentTextField"
  const val ADD_IMAGE_BUTTON = "addImageButton"
  const val SELECTED_IMAGE_PREVIEW = "selectedImagePreview"
  const val POST_COMMENT_BUTTON = "postCommentButton"
  const val CAMERA_OPTION_BUTTON = "cameraOptionButton"
  const val GALLERY_OPTION_BUTTON = "galleryOptionButton"
  const val CANCEL_IMAGE_SOURCE_BUTTON = "cancelImageSourceButton"
  const val EMPTY_COMMENTS_STATE = "emptyCommentsState"
}

/**
 * Modal bottom sheet for viewing and adding comments to a post.
 *
 * @param post The outfit post to display comments for
 * @param currentUserId The ID of the current user
 * @param onDismiss Callback when the bottom sheet is dismissed
 * @param onCommentAdded Callback when a comment is successfully added
 * @param viewModel ViewModel for managing comment operations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentBottomSheet(
    post: OutfitPost,
    currentUserId: String,
    onDismiss: () -> Unit,
    onCommentAdded: () -> Unit,
    onProfileClick: (String) -> Unit = {},
    viewModel: CommentViewModel = viewModel()
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val uiState by viewModel.uiState.collectAsState()
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  // Show error message in snackbar
  LaunchedEffect(uiState.errorMessage) {
    uiState.errorMessage?.let { error ->
      snackbarHostState.showSnackbar(error)
      viewModel.clearError()
    }
  }

  ModalBottomSheet(
      onDismissRequest = onDismiss,
      sheetState = sheetState,
      modifier = Modifier.testTag(CommentScreenTestTags.COMMENT_BOTTOM_SHEET),
      containerColor = Background) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f) // 90% of screen height
            ) {
              // Header
              CommentHeader(commentCount = post.comments.size, onClose = onDismiss)

              HorizontalDivider(color = Tertiary.copy(alpha = 0.3f))

              // Comments list (scrollable, takes available space)
              Box(modifier = Modifier.weight(1f)) {
                CommentsList(
                    comments = post.comments,
                    currentUserId = currentUserId,
                    viewModel = viewModel,
                    onDeleteComment = { comment ->
                      scope.launch {
                        viewModel.deleteComment(post.postUID, comment).onSuccess {
                          onCommentAdded()
                        }
                      }
                    },
                    onProfileClick = onProfileClick)
              }

              // Input section (fixed at bottom)
              AddCommentSection(
                  postId = post.postUID,
                  currentUserId = currentUserId,
                  viewModel = viewModel,
                  onCommentAdded = onCommentAdded)
            }

        // Snackbar host
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.padding(16.dp))
      }
}

/**
 * Header section of the comment bottom sheet showing comment count and close button.
 *
 * @param commentCount Number of comments on the post
 * @param onClose Callback when close button is clicked
 */
@Composable
private fun CommentHeader(commentCount: Int, onClose: () -> Unit) {
  Box(
      modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 24.dp),
      contentAlignment = Alignment.Center) {
        Text(
            text = "Comments ($commentCount)",
            style = Typography.titleLarge,
            color = Primary,
            fontWeight = FontWeight.Bold)

        IconButton(
            onClick = onClose,
            modifier =
                Modifier.align(Alignment.CenterEnd)
                    .testTag(CommentScreenTestTags.CLOSE_COMMENTS_BUTTON)) {
              Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Close comments",
                  tint = OnSurfaceVariant)
            }
      }
}

/**
 * Scrollable list of comments with empty state.
 *
 * @param comments List of comments to display
 * @param currentUserId The ID of the current user
 * @param viewModel ViewModel for fetching user data
 * @param onDeleteComment Callback when a comment is deleted
 */
@Composable
private fun CommentsList(
    comments: List<Comment>,
    currentUserId: String,
    viewModel: CommentViewModel,
    onDeleteComment: (Comment) -> Unit,
    onProfileClick: (String) -> Unit = {}
) {
  if (comments.isEmpty()) {
    // Empty state
    Box(
        modifier = Modifier.fillMaxSize().testTag(CommentScreenTestTags.EMPTY_COMMENTS_STATE),
        contentAlignment = Alignment.Center) {
          Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Comment,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Tertiary)
                Text(
                    text = "No comments yet",
                    style = Typography.bodyLarge,
                    color = Tertiary,
                    fontWeight = FontWeight.Bold)
                Text(
                    text = "Be the first to comment!",
                    style = Typography.bodyMedium,
                    color = Tertiary)
              }
        }
  } else {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(CommentScreenTestTags.COMMENTS_LIST),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
          items(comments, key = { it.commentId }) { comment ->
            CommentItem(
                comment = comment,
                currentUserId = currentUserId,
                viewModel = viewModel,
                onDeleteComment = { onDeleteComment(comment) },
                onProfileClick = onProfileClick)
          }
        }
  }
}

/**
 * Individual comment item showing user info, text, optional reaction image, and delete button.
 *
 * @param comment The comment to display
 * @param currentUserId The ID of the current user
 * @param viewModel ViewModel for fetching user data
 * @param onDeleteComment Callback when delete button is clicked
 */
@Composable
private fun CommentItem(
    comment: Comment,
    currentUserId: String,
    viewModel: CommentViewModel,
    onDeleteComment: () -> Unit,
    onProfileClick: (String) -> Unit = {}
) {
  // Fetch user data
  var userData by remember { mutableStateOf<com.android.ootd.model.user.User?>(null) }

  LaunchedEffect(comment.ownerId) { userData = viewModel.getUserData(comment.ownerId) }

  val isOwnComment = comment.ownerId == currentUserId

  Row(
      modifier =
          Modifier.fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 8.dp)
              .testTag("${CommentScreenTestTags.COMMENT_ITEM}_${comment.commentId}")) {
        // Profile Picture
        ProfilePicture(
            modifier = Modifier.padding(top = 4.dp),
            size = 40.dp,
            profilePicture = userData?.profilePicture ?: "",
            username = userData?.username ?: "",
            onClick = { onProfileClick(comment.ownerId) })

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          // Username and Timestamp
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = userData?.username ?: "Loading...",
                style = Typography.bodyLarge,
                color = Primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onProfileClick(comment.ownerId) })

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formatTimestamp(comment.timestamp),
                style = Typography.bodySmall,
                color = Tertiary)
          }

          Spacer(modifier = Modifier.height(4.dp))

          // Comment text
          Text(text = comment.text, style = Typography.bodyLarge, color = OnSurface)

          // Reaction image (if exists)
          if (comment.reactionImage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            AsyncImage(
                model = comment.reactionImage,
                contentDescription = "Reaction image",
                modifier =
                    Modifier.size(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Secondary)
                        .testTag("${CommentScreenTestTags.REACTION_IMAGE}_${comment.commentId}"),
                contentScale = ContentScale.Crop)
          }
        }

        // Delete button (only for own comments)
        if (isOwnComment) {
          IconButton(
              onClick = onDeleteComment,
              modifier =
                  Modifier.size(24.dp)
                      .testTag(
                          "${CommentScreenTestTags.DELETE_COMMENT_BUTTON}_${comment.commentId}")) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete comment",
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(16.dp))
              }
        }
      }
}

/**
 * Input section for adding a new comment with optional reaction image.
 *
 * @param postId The ID of the post to comment on
 * @param currentUserId The ID of the current user
 * @param viewModel ViewModel for managing comment operations
 * @param onCommentAdded Callback when a comment is successfully added
 */
@Composable
private fun AddCommentSection(
    postId: String,
    currentUserId: String,
    viewModel: CommentViewModel,
    onCommentAdded: () -> Unit
) {
  var commentText by remember { mutableStateOf("") }
  var showImageSourceDialog by remember { mutableStateOf(false) }
  var showCamera by remember { mutableStateOf(false) }
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsState()
  val scope = rememberCoroutineScope()

  val maxChars = 500

  // Gallery launcher for selecting existing images
  val galleryLauncher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.setSelectedImage(it) }
      }

  Column(
      modifier =
          Modifier.fillMaxWidth()
              .background(Background)
              .testTag(CommentScreenTestTags.ADD_COMMENT_SECTION)) {
        HorizontalDivider(color = Tertiary.copy(alpha = 0.3f))

        // Selected Image Preview (if any)
        uiState.selectedImage?.let { uri ->
          Box(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
            AsyncImage(
                model = uri,
                contentDescription = "Selected image",
                modifier =
                    Modifier.size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Secondary)
                        .testTag(CommentScreenTestTags.SELECTED_IMAGE_PREVIEW),
                contentScale = ContentScale.Crop)

            IconButton(
                onClick = { viewModel.setSelectedImage(null) },
                modifier =
                    Modifier.size(24.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-6).dp)
                        .background(OnSurface, CircleShape)) {
                  Icon(
                      imageVector = Icons.Default.Close,
                      contentDescription = "Remove image",
                      tint = Background,
                      modifier = Modifier.size(16.dp))
                }
          }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Bottom) {

              // Add Image Button
              IconButton(
                  onClick = { showImageSourceDialog = true },
                  modifier = Modifier.testTag(CommentScreenTestTags.ADD_IMAGE_BUTTON)) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Add reaction image",
                        tint = Primary)
                  }

              // Text Input
              OutlinedTextField(
                  value = commentText,
                  onValueChange = { if (it.length <= maxChars) commentText = it },
                  modifier = Modifier.weight(1f).testTag(CommentScreenTestTags.COMMENT_TEXT_FIELD),
                  placeholder = {
                    Text("Add a comment...", style = Typography.bodyLarge, color = Tertiary)
                  },
                  maxLines = 4,
                  shape = RoundedCornerShape(24.dp),
                  colors =
                      OutlinedTextFieldDefaults.colors(
                          focusedBorderColor = Primary,
                          unfocusedBorderColor = Tertiary,
                          focusedContainerColor = Background,
                          unfocusedContainerColor = Background),
                  textStyle = Typography.bodyLarge)

              Spacer(modifier = Modifier.width(8.dp))

              // Post Button
              val isEnabled = commentText.isNotBlank() && !uiState.isSubmitting
              IconButton(
                  onClick = {
                    scope.launch {
                      val result =
                          viewModel.addComment(
                              postId = postId,
                              userId = currentUserId,
                              text = commentText,
                              imageUri = uiState.selectedImage,
                              context = context)

                      result.onSuccess {
                        commentText = ""
                        onCommentAdded()
                      }
                    }
                  },
                  enabled = isEnabled,
                  modifier =
                      Modifier.size(48.dp) // Match standard touch target size
                          .background(if (isEnabled) Primary else Tertiary, CircleShape)
                          .testTag(CommentScreenTestTags.POST_COMMENT_BUTTON)) {
                    if (uiState.isSubmitting) {
                      CircularProgressIndicator(
                          modifier = Modifier.size(20.dp), color = Background, strokeWidth = 2.dp)
                    } else {
                      Icon(
                          imageVector = Icons.AutoMirrored.Filled.Send,
                          contentDescription = "Post",
                          tint = Background,
                          modifier =
                              Modifier.padding(start = 2.dp)) // Visual correction for send icon
                    }
                  }
            }
      }

  // Image source dialog (Camera or Gallery)
  if (showImageSourceDialog) {
    ImageSelectionDialog(
        onDismissRequest = { showImageSourceDialog = false },
        onTakePhoto = {
          showImageSourceDialog = false
          showCamera = true
        },
        onPickFromGallery = {
          showImageSourceDialog = false
          galleryLauncher.launch("image/*")
        },
        takePhotoTag = CommentScreenTestTags.CAMERA_OPTION_BUTTON,
        pickGalleryTag = CommentScreenTestTags.GALLERY_OPTION_BUTTON)
  }

  // Show custom camera
  if (showCamera) {
    CameraScreen(
        onImageCaptured = { uri ->
          viewModel.setSelectedImage(uri)
          showCamera = false
        },
        onDismiss = { showCamera = false })
  }
}

/**
 * Formats a timestamp into a relative time string for displaying on comment.
 *
 * @param timestamp The timestamp in milliseconds
 * @return Formatted string like "Just now", "5m ago", "2h ago", "3d ago"
 */
internal fun formatTimestamp(timestamp: Long): String {
  val now = System.currentTimeMillis()
  val diff = now - timestamp

  return when {
    diff < 60_000 -> "Just now"
    diff < 3600_000 -> "${diff / 60_000}m ago"
    diff < 86400_000 -> "${diff / 3600_000}h ago"
    else -> "${diff / 86400_000}d ago"
  }
}
