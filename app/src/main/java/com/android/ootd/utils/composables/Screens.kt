package com.android.ootd.utils.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextAlign.Companion.Center
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.ootd.R
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.ui.camera.CameraScreenTestTags
import com.android.ootd.ui.post.items.commonTextFieldColors
import com.android.ootd.ui.theme.Bodoni
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.Typography

// Disclaimer: Some parts of the code were written with the help of AI and verified by human

/**
 * Displays text with customizable styling.
 *
 * @param modifier The modifier to be applied to the text.
 * @param text The text to display.
 * @param style The text style to apply.
 * @param color The color of the text. Defaults to Primary theme color.
 * @param textAlign The text alignment. Defaults to Center.
 * @param fontFamily The font family to use. Defaults to Bodoni.
 */
@Composable
fun ShowText(
    modifier: Modifier = Modifier,
    text: String,
    style: TextStyle,
    color: Color = Primary,
    textAlign: TextAlign = Center,
    fontFamily: FontFamily = Bodoni,
) {
  Text(
      text = text,
      style = style,
      color = color,
      textAlign = textAlign,
      fontFamily = fontFamily,
      modifier = modifier.fillMaxWidth())
}

/**
 * Displays a user's profile picture. If no picture URL is provided, shows a circular avatar with
 * the first letter of the username.
 *
 * @param modifier The modifier to be applied to the profile picture.
 * @param size The size of the profile picture.
 * @param profilePicture The URL of the profile picture to display.
 * @param username The username, used to display the first letter as a fallback.
 * @param textStyle The text style for the username letter. Defaults to typography.headlineLarge.
 * @param shape The shape of the profile picture container. Defaults to CircleShape.
 * @param onClick Optional click handler for the profile picture.
 */
@Composable
fun ProfilePicture(
    modifier: Modifier = Modifier,
    size: Dp,
    profilePicture: String,
    username: String,
    textStyle: TextStyle = Typography.headlineLarge,
    shape: RoundedCornerShape = CircleShape,
    onClick: (() -> Unit)? = null
) {
  val color = colorScheme
  val defaultAvatarPainter = remember(color.tertiary) { ColorPainter(color.tertiary) }

  if (profilePicture.isNotBlank()) {
    // We apply modifier directly
    AsyncImage(
        model = profilePicture,
        contentDescription = "Profile Picture",
        placeholder = defaultAvatarPainter,
        error = defaultAvatarPainter,
        contentScale = ContentScale.Crop,
        modifier =
            modifier
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .size(size)
                .clip(shape))
  } else if (username.isNotBlank()) {
    // We apply the modifier to the Box only
    Box(
        modifier =
            modifier
                .semantics(mergeDescendants = true) {}
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .size(size)
                .clip(shape)
                .background(Primary),
        contentAlignment = Alignment.Center) {
          Text(text = username.first().uppercase(), style = textStyle, color = Secondary)
        }
  } else {
    // Defaulting to person icon
    Box(
        modifier =
            modifier
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .size(size)
                .clip(shape)
                .background(color.tertiary),
        contentAlignment = Alignment.Center) {
          Icon(
              imageVector = Icons.Default.Person,
              contentDescription = "Default Profile Picture",
              tint = color.onSurfaceVariant,
              modifier = Modifier.size(size * 0.6f))
        }
  }
}

/**
 * Displays a grid of user posts in a 3-column layout.
 *
 * @param posts The list of outfit posts to display.
 * @param onPostClick Callback invoked when a post is clicked, receives the post ID.
 * @param modifier The modifier to be applied to individual post items.
 * @param padding The padding around the entire grid.
 * @param spacing The spacing between grid items.
 */
@Composable
fun DisplayUserPosts(
    posts: List<OutfitPost>,
    onPostClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    displayPerRow: Int = 3,
    padding: Dp,
    spacing: Dp
) {
  val color = colorScheme
  val defaultPainter = remember(color.tertiary) { ColorPainter(color.tertiary) }
  val configuration = LocalConfiguration.current
  val screenWidth = configuration.screenWidthDp.dp
  val itemWidth = (screenWidth - padding - (spacing * 2)) / 3 // subtract padding and spacing
  val itemHeight = itemWidth
  val rowCount = (posts.size + 2) / 3
  val totalHeight = rowCount * itemHeight.value + (rowCount - 1) * spacing.value

  LazyVerticalGrid(
      columns = GridCells.Fixed(displayPerRow),
      horizontalArrangement = Arrangement.spacedBy(spacing),
      verticalArrangement = Arrangement.spacedBy(spacing),
      modifier = Modifier.fillMaxWidth().height(totalHeight.dp)) {
        items(posts) { post ->
          AsyncImage(
              model = post.outfitURL,
              contentDescription = "Post image",
              placeholder = defaultPainter,
              error = defaultPainter,
              contentScale = ContentScale.Crop,
              modifier =
                  modifier
                      .size(itemWidth)
                      .clip(RoundedCornerShape(6.dp))
                      .clickable(onClick = { onPostClick(post.postUID) })
                      .background(color.surfaceVariant))
        }
      }
}

/**
 * Displays a loading screen with a hanger icon and circular progress indicator.
 *
 * @param modifier The modifier to be applied to the loading screen container.
 * @param contentDescription The content description for the hanger icon.
 */
@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    loadingModifier: Modifier = Modifier,
    contentDescription: String? = null
) {
  Box(
      modifier = Modifier.fillMaxSize().background(colorScheme.background.copy(alpha = 0.95f)),
      contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Image(
              painter = painterResource(id = R.drawable.hanger),
              contentDescription = contentDescription,
              modifier = modifier.size(72.dp))
          Spacer(modifier = Modifier.height(16.dp))
          CircularProgressIndicator(modifier = loadingModifier, color = colorScheme.primary)
        }
      }
}

/**
 * Displays a permission request screen with message and action buttons.
 *
 * Shows a centered message explaining why permission is needed, with buttons to grant permission or
 * cancel.
 *
 * @param message The message explaining why the permission is needed.
 * @param onRequestPermission Callback invoked when user clicks to grant permission.
 * @param onCancel Callback invoked when user cancels the permission request.
 * @param modifier The modifier to be applied to the container.
 */
@Composable
fun PermissionRequestScreen(
    message: String,
    onRequestPermission: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
  Column(
      modifier = modifier.fillMaxSize().background(Secondary).padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Text(
            text = message,
            color = Primary,
            style = Typography.bodyLarge,
            textAlign = Center,
            modifier = Modifier.padding(16.dp))

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            modifier = Modifier.testTag(CameraScreenTestTags.PERMISSION_REQUEST_BUTTON)) {
              Text("Grant Permission !")
            }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onCancel) { Text("Cancel", color = Black) }
      }
}

/**
 * Displays a centered loading state with a circular progress indicator and optional message.
 *
 * Used to show a loading state in the center of a screen with an optional descriptive message.
 *
 * @param modifier The modifier to be applied to the container.
 * @param message The optional message to display below the progress indicator.
 */
@Composable
fun CenteredLoadingState(
    modifier: Modifier = Modifier,
    message: String? = null,
    textColor: Color = colorScheme.onSurfaceVariant
) {
  Column(
      modifier = modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = colorScheme.primary)
        if (message != null) {
          Spacer(modifier = Modifier.height(12.dp))
          Text(text = message, style = Typography.bodyLarge, color = textColor)
        }
      }
}

/**
 * Displays a centered empty state with a message.
 *
 * Used when there's no data to display in a screen or list.
 *
 * @param modifier The modifier to be applied to the container.
 */
@Composable
fun CenteredEmptyState(
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit),
    spacer: Dp = 8.dp,
) {
  Column(
      modifier = modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) {
          icon.invoke()
          Spacer(modifier = Modifier.height(spacer))
        }
        text()
      }
}

/** Reusable generic text field with common styling */
@Composable
fun CommonTextField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    isError: Boolean = false,
    maxLines: Int = 15,
    keyBoardActions: KeyboardActions = KeyboardActions.Default,
    keyBoardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
  OutlinedTextField(
      value = value,
      onValueChange = onChange,
      label = { Text(label) },
      placeholder = { Text(placeholder) },
      textStyle = Typography.bodyMedium.copy(color = Primary),
      singleLine = singleLine,
      readOnly = readOnly,
      isError = isError,
      maxLines = maxLines,
      enabled = enabled,
      keyboardActions = keyBoardActions,
      keyboardOptions = keyBoardOptions,
      trailingIcon = trailingIcon,
      colors = commonTextFieldColors(),
      modifier = modifier.fillMaxWidth())
}

/**
 * A reusable clickable profile row with picture + username + optional content. The entire ROW is
 * clickable
 *
 * @param userId The user's unique ID (passed to onClick)
 * @param username The display name of the user
 * @param profilePictureUrl The URL of the user's profile picture
 * @param profileSize The size of the profile picture
 * @param onProfileClick Callback when the row is clicked, receives userId
 * @param modifier Optional modifier for the row
 * @param enabled Whether the row is clickable (default true)
 * @param showUsername Whether to display the username (default true)
 * @param usernameStyle Text style for the username
 * @param usernameColor Color for the username text
 * @param usernameModifier Modifier for the username text
 * @param profileTestTag Test tag for the profile picture
 * @param usernameTestTag Test tag for the username
 * @param horizontalSpacing Spacing between profile picture and username
 * @param additionalContent Optional composable for extra content
 */
@Composable
fun ClickableProfileRow(
    userId: String,
    username: String,
    profilePictureUrl: String,
    profileSize: Dp,
    onProfileClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showUsername: Boolean = true,
    usernameStyle: TextStyle = Typography.titleLarge,
    usernameColor: Color = Primary,
    usernameModifier: Modifier = Modifier,
    profileTestTag: String? = null,
    usernameTestTag: String? = null,
    horizontalSpacing: Dp = 8.dp,
    additionalContent: @Composable ColumnScope.() -> Unit = {}
) {
  Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = modifier.clickable(enabled = enabled) { onProfileClick(userId) }) {
        ProfilePicture(
            modifier = if (profileTestTag != null) Modifier.testTag(profileTestTag) else Modifier,
            size = profileSize,
            profilePicture = profilePictureUrl,
            username = username,
            textStyle = Typography.bodySmall)

        if (showUsername) {
          Spacer(modifier = Modifier.width(horizontalSpacing))

          Column {
            Text(
                text = username,
                style = usernameStyle,
                color = usernameColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    usernameModifier.then(
                        if (usernameTestTag != null) Modifier.testTag(usernameTestTag)
                        else Modifier))

            additionalContent()
          }
        }
      }
}

/**
 * A clickable profile displayed vertically (picture on top, username below). The entire COLUMN is
 * clickable
 *
 * @param userId The user's unique ID (passed to onClick)
 * @param username The display name of the user
 * @param profilePictureUrl The URL of the user's profile picture
 * @param profileSize The size of the profile picture
 * @param onProfileClick Callback when clicked, receives userId
 * @param modifier Optional modifier
 * @param usernameStyle Text style for the username
 * @param usernameColor Color for the username
 * @param profileTestTag Test tag for the profile picture
 * @param usernameTestTag Test tag for the username
 */
@Composable
fun ClickableProfileColumn(
    userId: String,
    username: String,
    profilePictureUrl: String,
    profileSize: Dp = 48.dp,
    onProfileClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    usernameStyle: TextStyle = Typography.labelSmall,
    usernameColor: Color = Primary,
    profileTestTag: String? = null,
    usernameTestTag: String? = null
) {
  Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = modifier.clickable { onProfileClick(userId) }) {
        ProfilePicture(
            size = profileSize,
            profilePicture = profilePictureUrl,
            username = username,
            textStyle = Typography.bodyMedium,
            modifier = if (profileTestTag != null) Modifier.testTag(profileTestTag) else Modifier)

        Spacer(Modifier.height(4.dp))

        Text(
            text = username,
            style = usernameStyle,
            color = usernameColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = Center,
            modifier = if (usernameTestTag != null) Modifier.testTag(usernameTestTag) else Modifier)
      }
}
