package com.android.ootd.utils.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.ootd.R
import com.android.ootd.ui.theme.LightColorScheme
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.Tertiary
import com.android.ootd.ui.theme.Typography

/**
 * Displays a back arrow icon button.
 *
 * @param onBackClick Callback invoked when the back arrow is clicked.
 * @param modifier The modifier to be applied to the icon button.
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
          Text(text = buttonText, color = colors.onPrimary, style = Typography.titleMedium)
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

/**
 * Displays a notification icon button.
 *
 * @param onNotificationIconClick Callback invoked when the notification button is clicked.
 * @param modifier The modifier to be applied to the icon button.
 * @param size The size of the notification icon.
 */
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

          Text(text = "Sign in with Google", style = Typography.titleLarge, color = Primary)
        }
      }
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
    icon: ImageVector,
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
 * Reusable pill showing a friend count with an icon, clickable for navigation/dialogs.
 *
 * @param friendCount number of friends to display
 * @param modifier optional modifier for tagging/placement
 * @param onClick invoked when the chip is tapped
 */
@Composable
fun FriendCountChip(
    friendCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    label: String? = null
) {
  val text = label ?: "$friendCount friends"
  Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .clickable(onClick = onClick)
                .background(Secondary, RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 10.dp)) {
          Text(
              text = text,
              style = Typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
              color = Primary)
          Spacer(modifier = Modifier.size(8.dp))
          Icon(
              imageVector = Icons.Filled.Group, contentDescription = "View friends", tint = Primary)
        }
  }
}

/**
 * Displays a standard icon button with customizable icon, color, and size.
 *
 * @param onClick Callback invoked when the button is clicked.
 * @param icon The icon to display.
 * @param contentDescription The content description for accessibility.
 * @param modifier The modifier to be applied to the button.
 * @param tint The tint color of the icon. Defaults to Primary.
 * @param size The size of the icon. Defaults to 24.dp.
 */
@Composable
fun ActionIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color = colorScheme.primary,
    size: Dp = 24.dp
) {
  IconButton(onClick = onClick, modifier = modifier) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(size))
  }
}

/**
 * Displays a custom tab row with OOTD styling.
 *
 * @param selectedTabIndex The index of the currently selected tab.
 * @param tabs The list of tab titles.
 * @param onTabClick Callback invoked when a tab is clicked.
 * @param modifier The modifier to be applied to the tab row.
 * @param tabModifiers Optional list of modifiers to apply to each tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OOTDTabRow(
    selectedTabIndex: Int,
    tabs: List<String>,
    onTabClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tabModifiers: List<Modifier> = emptyList()
) {
  PrimaryTabRow(
      selectedTabIndex = selectedTabIndex,
      containerColor = Color.White,
      contentColor = Secondary,
      indicator = {
        TabRowDefaults.PrimaryIndicator(
            modifier = Modifier.tabIndicatorOffset(selectedTabIndex), color = Primary)
      },
      modifier = modifier) {
        tabs.forEachIndexed { index, title ->
          Tab(
              selected = selectedTabIndex == index,
              onClick = { onTabClick(index) },
              text = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight =
                        if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal)
              },
              selectedContentColor = Primary,
              unselectedContentColor = Tertiary,
              modifier = tabModifiers.getOrElse(index) { Modifier })
        }
      }
}
