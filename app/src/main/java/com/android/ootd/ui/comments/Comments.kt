package com.android.ootd.ui.comments

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.android.ootd.ui.theme.OnSecondaryContainer
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.Tertiary
import com.android.ootd.ui.theme.Typography
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
      containerColor = Secondary) {
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
                    })
              }

              HorizontalDivider(color = Tertiary.copy(alpha = 0.3f))

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
  Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Comments ($commentCount)",
            style = Typography.titleLarge,
            color = Primary,
            fontWeight = FontWeight.Bold)

        IconButton(
            onClick = onClose,
            modifier = Modifier.testTag(CommentScreenTestTags.CLOSE_COMMENTS_BUTTON)) {
              Icon(
                  imageVector = Icons.Default.Close,
                  contentDescription = "Close comments",
                  tint = Primary)
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
    onDeleteComment: (Comment) -> Unit
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
                    imageVector = Icons.Outlined.Comment,
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
                onDeleteComment = { onDeleteComment(comment) })
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
    onDeleteComment: () -> Unit
) {
  // Fetch user data
  var userData by remember { mutableStateOf<com.android.ootd.model.user.User?>(null) }

  LaunchedEffect(comment.ownerId) { userData = viewModel.getUserData(comment.ownerId) }

  val isOwnComment = comment.ownerId == currentUserId

  Row(
      modifier =
          Modifier.fillMaxWidth()
              .testTag("${CommentScreenTestTags.COMMENT_ITEM}_${comment.commentId}"),
      horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        // Profile picture (circular, 40dp)
        ProfilePicture(
            size = 40.dp,
            profilePicture = userData?.profilePicture ?: "",
            username = userData?.username ?: "User",
            textStyle = Typography.bodyMedium)

        // Comment content
        Column(modifier = Modifier.weight(1f)) {
          // Username and timestamp
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = userData?.username ?: "Loading...",
                    style = Typography.titleSmall,
                    color = Primary,
                    fontWeight = FontWeight.Bold)

                Text(
                    text = formatTimestamp(comment.timestamp),
                    style = Typography.bodySmall,
                    color = Tertiary)
              }

          Spacer(modifier = Modifier.height(4.dp))

          // Comment text
          Text(text = comment.text, style = Typography.bodyMedium, color = OnSecondaryContainer)

          // Reaction image (if exists)
          if (comment.reactionImage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))

            AsyncImage(
                model = comment.reactionImage,
                contentDescription = "Reaction image",
                modifier =
                    Modifier.size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .testTag("${CommentScreenTestTags.REACTION_IMAGE}_${comment.commentId}"),
                contentScale = ContentScale.Crop)
          }
        }

        // Delete button (only for own comments)
        if (isOwnComment) {
          IconButton(
              onClick = onDeleteComment,
              modifier =
                  Modifier.testTag(
                      "${CommentScreenTestTags.DELETE_COMMENT_BUTTON}_${comment.commentId}")) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete comment",
                    tint = MaterialTheme.colorScheme.error)
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
  val remainingChars = maxChars - commentText.length

  // Gallery launcher for selecting existing images
  val galleryLauncher =
      rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.setSelectedImage(it) }
      }

  Column(
      modifier =
          Modifier.fillMaxWidth()
              .background(Secondary)
              .padding(16.dp)
              .testTag(CommentScreenTestTags.ADD_COMMENT_SECTION)) {
        // Text input with character counter
        OutlinedTextField(
            value = commentText,
            onValueChange = { if (it.length <= maxChars) commentText = it },
            modifier = Modifier.fillMaxWidth().testTag(CommentScreenTestTags.COMMENT_TEXT_FIELD),
            placeholder = { Text("Add a comment...") },
            maxLines = 4,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Tertiary,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White),
            supportingText = {
              Text(
                  text = "$remainingChars characters remaining",
                  style = Typography.bodySmall,
                  color = if (remainingChars < 50) MaterialTheme.colorScheme.error else Tertiary)
            })

        Spacer(modifier = Modifier.height(8.dp))

        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically) {
                    // Add image button
                    IconButton(
                        onClick = { showImageSourceDialog = true },
                        modifier = Modifier.testTag(CommentScreenTestTags.ADD_IMAGE_BUTTON)) {
                          Icon(
                              imageVector = Icons.Default.AddPhotoAlternate,
                              contentDescription = "Add reaction image",
                              tint = Primary)
                        }

                    // Image preview (circular, 40dp)
                    uiState.selectedImage?.let { uri ->
                      Box {
                        AsyncImage(
                            model = uri,
                            contentDescription = "Selected image",
                            modifier =
                                Modifier.size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .testTag(CommentScreenTestTags.SELECTED_IMAGE_PREVIEW),
                            contentScale = ContentScale.Crop)

                        // Remove button overlay
                        IconButton(
                            onClick = { viewModel.setSelectedImage(null) },
                            modifier =
                                Modifier.size(20.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)) {
                              Icon(
                                  imageVector = Icons.Default.Close,
                                  contentDescription = "Remove image",
                                  tint = Color.White,
                                  modifier = Modifier.size(12.dp))
                            }
                      }
                    }
                  }

              // Post button
              Button(
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
                  enabled = commentText.isNotBlank() && !uiState.isSubmitting,
                  modifier = Modifier.testTag(CommentScreenTestTags.POST_COMMENT_BUTTON),
                  colors =
                      ButtonDefaults.buttonColors(
                          containerColor = Primary, contentColor = Color.White),
                  shape = RoundedCornerShape(20.dp)) {
                    if (uiState.isSubmitting) {
                      CircularProgressIndicator(
                          modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                      Text("Post", style = Typography.bodyMedium)
                    }
                  }
            }
      }

  // Image source dialog (Camera or Gallery)
  if (showImageSourceDialog) {
    ImageSourceDialog(
        onDismiss = { showImageSourceDialog = false },
        onCameraSelected = {
          showImageSourceDialog = false
          showCamera = true
        },
        onGallerySelected = {
          showImageSourceDialog = false
          galleryLauncher.launch("image/*")
        })
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
 * Dialog for selecting image source (camera or gallery).
 *
 * @param onDismiss Callback when dialog is dismissed
 * @param onCameraSelected Callback when camera option is selected
 * @param onGallerySelected Callback when gallery option is selected
 */
@Composable
private fun ImageSourceDialog(
    onDismiss: () -> Unit,
    onCameraSelected: () -> Unit,
    onGallerySelected: () -> Unit
) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Add Reaction Image") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          TextButton(
              onClick = onCameraSelected,
              modifier =
                  Modifier.fillMaxWidth().testTag(CommentScreenTestTags.CAMERA_OPTION_BUTTON)) {
                Text("Take Photo")
              }

          TextButton(
              onClick = onGallerySelected,
              modifier =
                  Modifier.fillMaxWidth().testTag(CommentScreenTestTags.GALLERY_OPTION_BUTTON)) {
                Text("Choose from Gallery")
              }
        }
      },
      confirmButton = {})
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
