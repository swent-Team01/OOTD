package com.android.ootd.utils.composables

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.ootd.ui.navigation.NavigationTestTags
import com.android.ootd.ui.navigation.Tab
import com.android.ootd.ui.theme.Background
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Typography

/**
 * Displays a center-aligned top app bar with customizable left and right composables.
 *
 * The center displays the app name or custom text. Left and right slots can contain composables
 * like back arrows, action buttons, or other icons.
 *
 * @param modifier The modifier to be applied to the top app bar container.
 * @param textModifier The modifier to be applied to the title text.
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
                Typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold, color = Primary))
      },
      navigationIcon = { leftComposable() },
      actions = { rightComposable() },
      colors = TopAppBarDefaults.topAppBarColors(containerColor = Background))
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
