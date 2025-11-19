package com.android.ootd.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextAlign.Companion.Center
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.android.ootd.R
import com.android.ootd.model.posts.OutfitPost
import com.android.ootd.ui.camera.CameraScreenTestTags
import com.android.ootd.ui.navigation.NavigationTestTags
import com.android.ootd.ui.navigation.Tab
import com.android.ootd.ui.theme.Bodoni
import com.android.ootd.ui.theme.LightColorScheme
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary

/**
 * Displays a back arrow icon button.
 *
 * @param onBackClick Callback invoked when the back arrow is clicked.
 */
@Composable
fun BackArrow(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
  IconButton(onClick = onBackClick, modifier = modifier) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Back",
        tint = colorScheme.onBackground,
        modifier = Modifier.size(48.dp))
  }
}

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
 * @param shape The shape of the profile picture container.
 */
@Composable
fun ProfilePicture(
    modifier: Modifier = Modifier,
    size: Dp,
    profilePicture: String,
    username: String,
    textStyle: TextStyle = typography.headlineLarge,
    shape: RoundedCornerShape = CircleShape
) {
  val color = colorScheme
  val defaultAvatarPainter = remember(color.tertiary) { ColorPainter(color.tertiary) }

  if (profilePicture.isNotBlank()) {
    AsyncImage(
        model = profilePicture,
        contentDescription = "Profile Picture",
        placeholder = defaultAvatarPainter,
        error = defaultAvatarPainter,
        contentScale = ContentScale.Crop,
        modifier = modifier.size(size).clip(shape))
  } else {
    Box(
        modifier = Modifier.size(size).clip(shape).background(Primary),
        contentAlignment = Alignment.Center) {
          Text(
              text = username.firstOrNull()?.uppercase() ?: "",
              style = textStyle,
              fontFamily = Bodoni,
              color = Secondary,
              modifier = modifier) // Only test tag
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
    padding: Dp,
    spacing: Dp
) {
  val color = colorScheme
  val defaultPainter = remember(color.tertiary) { ColorPainter(color.tertiary) }
  val configuration = androidx.compose.ui.platform.LocalConfiguration.current
  val screenWidth = configuration.screenWidthDp.dp
  val itemWidth = (screenWidth - padding - (spacing * 2)) / 3 // subtract padding and spacing
  val itemHeight = itemWidth
  val rowCount = (posts.size + 2) / 3
  val totalHeight = rowCount * itemHeight.value + (rowCount - 1) * spacing.value

  LazyVerticalGrid(
      columns = GridCells.Fixed(3),
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
fun LoadingScreen(modifier: Modifier = Modifier, contentDescription: String? = null) {
  Box(
      modifier = modifier.fillMaxSize().background(colorScheme.background.copy(alpha = 0.95f)),
      contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Image(
              painter = painterResource(id = R.drawable.hanger),
              contentDescription = contentDescription,
              modifier = Modifier.size(72.dp))
          Spacer(modifier = Modifier.height(16.dp))
          CircularProgressIndicator(color = colorScheme.primary)
        }
      }
}

/**
 * Displays a floating action button with customizable composable content.
 *
 * @param onButtonClick Callback invoked when the button is clicked, receives null.
 * @param modifier The modifier to be applied to the floating action button.
 * @param buttonText The composable content to display inside the button.
 */
@Composable
fun FloatingButton(
    onButtonClick: (String?) -> Unit,
    modifier: Modifier = Modifier,
    buttonText: @Composable () -> Unit
) {
  androidx.compose.material3.FloatingActionButton(
      onClick = { onButtonClick(null) },
      containerColor = colorScheme.primary,
      modifier = modifier) {
        buttonText()
      }
}

/**
 * Displays a circular action button with optional text content.
 *
 * If no text is provided, only the button container is displayed (typically used with an icon added
 * separately).
 *
 * @param onButtonClick Callback invoked when the button is clicked.
 * @param modifier The modifier to be applied to the button.
 * @param buttonText The text to display inside the button, or null for no text.
 */
@Composable
fun ActionButton(
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonText: String? = null
) {
  val colors = LightColorScheme
  Button(
      onClick = onButtonClick,
      shape = CircleShape,
      colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
      modifier = modifier) {
        if (buttonText != null) {
          Text(
              text = buttonText,
              color = colors.onPrimary,
              style = typography.titleMedium.copy(fontFamily = Bodoni))
        }
      }
}

/**
 * Displays a center-aligned top app bar with customizable left and right composables.
 *
 * The center displays the app name or custom text. Left and right slots can contain composables
 * like back arrows, action buttons, or other icons.
 *
 * @param modifier The modifier to be applied to the title text.
 * @param centerText The text to display in the center of the top bar. Defaults to "OOTD".
 * @param leftComposable The composable to display on the left side (navigation icon slot).
 * @param rightComposable The composable to display on the right side (actions slot).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OOTDTopBar(
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    centerText: String = "OOTD",
    leftComposable: @Composable () -> Unit = {},
    rightComposable: @Composable () -> Unit = {}
) {
  CenterAlignedTopAppBar(
      modifier = modifier.fillMaxWidth(),
      title = {
        Text(
            modifier = textModifier,
            text = centerText,
            style =
                typography.displayLarge.copy(
                    fontWeight = FontWeight.ExtraBold, color = colorScheme.primary))
      },
      navigationIcon = { leftComposable() },
      actions = { rightComposable() },
      colors =
          TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = colorScheme.background))
}

/**
 * Displays a bottom navigation bar with multiple tabs.
 *
 * Each tab shows an icon (filled when selected, outlined when not) and highlights the currently
 * selected tab.
 *
 * @param tabs The list of tabs to display in the navigation bar.
 * @param selectedTab The currently selected tab, or null if none is selected.
 * @param onTabSelected Callback invoked when a tab is selected.
 * @param modifier The modifier to be applied to the navigation bar.
 */
@Composable
fun BottomNavigationMenu(
    tabs: List<Tab>,
    selectedTab: Tab?,
    onTabSelected: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
  NavigationBar(
      modifier =
          modifier
              .fillMaxWidth()
              .height(80.dp)
              .border(
                  width = 0.5.dp,
                  color = Color.LightGray.copy(alpha = 0.3f),
              )
              .padding(top = 4.dp)
              .testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU),
      containerColor = colorScheme.background,
  ) {
    tabs.forEach { tab ->
      val isSelected = tab == selectedTab
      NavigationBarItem(
          icon = {
            Icon(
                imageVector = if (isSelected) tab.filledIcon else tab.outlinedIcon,
                contentDescription = "Navigate to ${tab.name}")
          },
          selected = isSelected,
          onClick = { onTabSelected(tab) },
          colors =
              NavigationBarItemDefaults.colors(
                  selectedIconColor = colorScheme.primary,
                  unselectedIconColor = colorScheme.onBackground,
                  indicatorColor = Color.Transparent),
          modifier = Modifier.padding(top = 8.dp).testTag(NavigationTestTags.getTabTestTag(tab)))
    }
  }
}

/**
 * Displays a settings icon button.
 *
 * @param onEditAccount Callback invoked when the settings button is clicked.
 * @param modifier The modifier to be applied to the icon button.
 * @param size The size of the settings icon.
 */
@Composable
fun SettingsButton(onEditAccount: () -> Unit, modifier: Modifier = Modifier, size: Dp) {
  IconButton(onClick = onEditAccount, modifier = modifier) {
    Icon(
        imageVector = Icons.Default.Settings,
        contentDescription = "Settings",
        tint = colorScheme.onBackground,
        modifier = Modifier.size(size))
  }
}

@Composable
fun NotificationButton(
    onNotificationIconClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp
) {
  IconButton(onClick = onNotificationIconClick, modifier = modifier) {
    Icon(
        painter = painterResource(id = R.drawable.ic_notification),
        contentDescription = "Notifications",
        tint = colorScheme.primary,
        modifier = Modifier.size(size))
  }
}

/**
 * Displays a Google Sign-In button with the Google logo and text.
 *
 * This button follows Material Design guidelines and includes the Google branding with proper
 * styling and spacing.
 *
 * @param onClick Callback invoked when the button is clicked.
 * @param modifier The modifier to be applied to the button.
 */
@Composable
fun GoogleSignInButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
  OutlinedButton(
      onClick = onClick,
      shape = RoundedCornerShape(50),
      modifier = modifier,
      border = BorderStroke(1.dp, Primary),
      colors = ButtonDefaults.outlinedButtonColors(containerColor = colorScheme.background)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
              modifier =
                  Modifier.size(28.dp)
                      .background(colorScheme.background, shape = CircleShape)
                      .border(1.dp, Primary, CircleShape),
              contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.google_logo),
                    contentDescription = "Google logo",
                    modifier = Modifier.size(18.dp))
              }

          Spacer(modifier = Modifier.size(12.dp))

          Text(text = "Sign in with Google", style = typography.titleLarge, color = Primary)
        }
      }
}

/**
 * Displays an error message text with consistent styling.
 *
 * @param errorMessage The error message to display.
 * @param modifier The modifier to be applied to the text.
 * @param textAlign The text alignment. Defaults to Center.
 */
@Composable
fun ErrorText(errorMessage: String, modifier: Modifier = Modifier, textAlign: TextAlign = Center) {
  Text(
      text = errorMessage,
      style = typography.bodyLarge,
      color = colorScheme.error,
      textAlign = textAlign,
      modifier = modifier.fillMaxWidth().padding(16.dp))
}

/**
 * Displays an empty state message with consistent styling.
 *
 * @param message The empty state message to display.
 * @param modifier The modifier to be applied to the text.
 */
@Composable
fun EmptyStateText(message: String, modifier: Modifier = Modifier) {
  Text(
      text = message,
      style = typography.bodyLarge,
      color = Color.Gray,
      textAlign = Center,
      modifier = modifier.fillMaxWidth().padding(32.dp))
}

/**
 * Displays a circular icon button with a background and customizable icon.
 *
 * Commonly used for floating actions like close, camera switch, or other circular buttons that need
 * to stand out from the background.
 *
 * @param onClick Callback invoked when the button is clicked.
 * @param icon The icon to display inside the button.
 * @param contentDescription The content description for accessibility.
 * @param modifier The modifier to be applied to the button.
 * @param backgroundColor The background color of the button. Defaults to Primary with 50% alpha.
 * @param iconTint The tint color of the icon. Defaults to White.
 * @param iconSize The size of the icon. Defaults to 36.dp.
 */
@Composable
fun CircularIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Primary.copy(alpha = 0.5f),
    iconTint: Color = Color.White,
    iconSize: Dp = 36.dp
) {
  IconButton(onClick = onClick, modifier = modifier.background(backgroundColor, CircleShape)) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = iconTint,
        modifier = Modifier.size(iconSize))
  }
}

/**
 * Displays a permission request screen with message and action buttons.
 *
 * Shows a centered message explaining why permission is needed, with buttons to grant permission or
 * cancel.
 *
 * @param permissionName The name of the permission being requested (e.g., "Camera").
 * @param message The message explaining why the permission is needed.
 * @param onRequestPermission Callback invoked when user clicks to grant permission.
 * @param onCancel Callback invoked when user cancels the permission request.
 * @param modifier The modifier to be applied to the container.
 */
@Composable
fun PermissionRequestScreen(
    permissionName: String,
    message: String,
    onRequestPermission: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
  Column(
      modifier = modifier.fillMaxSize().background(colorScheme.background).padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Text(
            text = message,
            color = colorScheme.onBackground,
            style = typography.bodyLarge,
            textAlign = Center,
            modifier = Modifier.padding(16.dp))

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
            modifier = Modifier.testTag(CameraScreenTestTags.PERMISSION_REQUEST_BUTTON)) {
              Text("Grant Permission !")
            }

        Spacer(modifier = Modifier.height(16.dp))

        androidx.compose.material3.TextButton(onClick = onCancel) {
          Text("Cancel", color = Secondary)
        }
      }
}

@Composable
fun CenteredLoadingState(message: String? = null, modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = colorScheme.primary)
        if (message != null) {
          Spacer(modifier = Modifier.height(12.dp))
          Text(text = message, style = typography.bodyLarge, color = colorScheme.onSurfaceVariant)
        }
      }
}

/**
 * Displays a centered empty state with a message.
 *
 * Used when there's no data to display in a screen or list.
 *
 * @param message The message to display.
 * @param modifier The modifier to be applied to the container.
 */
@Composable
fun CenteredEmptyState(message: String, modifier: Modifier = Modifier) {
  Column(
      modifier = modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = message,
            style = typography.bodyLarge,
            color = colorScheme.onSurfaceVariant,
            textAlign = Center,
            modifier = Modifier.padding(32.dp))
      }
}

/**
 * Displays a profile picture with a circular progress indicator.
 *
 * Used for showing user profiles with time-based progress indicators (e.g., story-like features).
 *
 * @param profilePictureUrl URL of the profile picture to display.
 * @param username Username for fallback initial display.
 * @param progressFraction Progress value between 0 and 1 for the circular indicator.
 * @param modifier The modifier to be applied to the container.
 * @param size The size of the profile picture and progress indicator.
 */
@Composable
fun ProfilePictureWithProgress(
    profilePictureUrl: String,
    username: String,
    progressFraction: Float,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    // Circular progress indicator
    CircularProgressIndicator(
        progress = { progressFraction.coerceIn(0f, 1f) },
        color = colorScheme.primary,
        trackColor = colorScheme.surfaceVariant,
        strokeWidth = 3.dp,
        modifier = Modifier.size(size))
    ProfilePicture(
        modifier = modifier,
        size = size,
        profilePicture = profilePictureUrl,
        username = username,
        shape = CircleShape)
  }
}
