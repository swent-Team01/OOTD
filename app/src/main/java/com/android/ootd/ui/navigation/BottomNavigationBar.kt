package com.android.ootd.ui.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Tabs used by the bottom navigation. Each tab points to a `Screen` destination. */
sealed class Tab(val name: String, val icon: ImageVector, val destination: Screen) {
  object Feed : Tab("Feed", Icons.Outlined.Home, Screen.Feed)

  object Search : Tab("Search", Icons.Outlined.Search, Screen.SearchScreen)

  object Inventory : Tab("Inventory", Icons.AutoMirrored.Outlined.List, Screen.InventoryScreen)

  object Account : Tab("Account", Icons.Outlined.Person, Screen.Account)

  object Map : Tab("Map", Icons.Outlined.Place, Screen.Map)
}

private val tabs = listOf(Tab.Feed, Tab.Search, Tab.Inventory, Tab.Account, Tab.Map)

/** Helpers for unit tests */
fun routeToTab(route: String): Tab = tabs.find { it.destination.route == route } ?: Tab.Feed

fun tabToScreen(tab: Tab): Screen = tab.destination

@Composable
fun BottomNavigationMenu(
    selectedTab: Tab?,
    onTabSelected: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
  NavigationBar(
      modifier =
          modifier
              .fillMaxWidth()
              .height(80.dp)
              .padding(top = 4.dp)
              .testTag(NavigationTestTags.BOTTOM_NAVIGATION_MENU),
      containerColor = MaterialTheme.colorScheme.secondary,
  ) {
    tabs.forEach { tab ->
      NavigationBarItem(
          icon = { Icon(imageVector = tab.icon, contentDescription = "Navigate to ${tab.name}") },
          label = { Text(tab.name) },
          selected = tab == selectedTab,
          onClick = { onTabSelected(tab) },
          colors =
              NavigationBarItemDefaults.colors(
                  selectedIconColor = MaterialTheme.colorScheme.secondary,
                  indicatorColor = MaterialTheme.colorScheme.tertiaryContainer),
          modifier = Modifier.padding(top = 8.dp).testTag(NavigationTestTags.getTabTestTag(tab)))
    }
  }
}

/** Maps route -> Tab and forwards clicks by emitting the Tab's destination Screen. */
@Composable
fun BottomNavigationBar(
    selectedRoute: String,
    onTabSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
  // Map current route string to a Tab; default to Feed
  val selectedTab = tabs.find { it.destination.route == selectedRoute }
  BottomNavigationMenu(
      selectedTab = selectedTab,
      onTabSelected = { tab -> onTabSelected(tab.destination) },
      modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun BottomNavigationMenuPreview() {
  Surface { BottomNavigationMenu(selectedTab = Tab.Feed, onTabSelected = {}) }
}
