package com.android.ootd.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.android.ootd.utils.composables.BottomNavigationMenu

/** Tabs used by the bottom navigation. Each tab points to a `Screen` destination. */
sealed class Tab(
    val name: String,
    val outlinedIcon: ImageVector,
    val filledIcon: ImageVector,
    val destination: Screen
) {
  object Feed : Tab("Feed", Icons.Outlined.Home, Icons.Filled.Home, Screen.Feed)

  object Search : Tab("Search", Icons.Outlined.Search, Icons.Filled.Search, Screen.SearchScreen)

  object Inventory :
      Tab(
          "Inventory",
          Icons.AutoMirrored.Outlined.List,
          Icons.AutoMirrored.Filled.List,
          Screen.InventoryScreen)

  object Account : Tab("Account", Icons.Outlined.Person, Icons.Filled.Person, Screen.AccountView)

  object Map : Tab("Map", Icons.Outlined.Place, Icons.Filled.Place, Screen.Map)
}

private val tabs = listOf(Tab.Map, Tab.Search, Tab.Feed, Tab.Inventory, Tab.Account)

/** Helpers for unit tests */
fun routeToTab(route: String): Tab = tabs.find { it.destination.route == route } ?: Tab.Feed

fun tabToScreen(tab: Tab): Screen = tab.destination

/** Maps route -> Tab and forwards clicks by emitting the Tab's destination Screen. */
@Composable
fun BottomNavigationBar(
    modifier: Modifier = Modifier,
    tabList: List<Tab> = tabs,
    selectedRoute: String,
    onTabSelected: (Screen) -> Unit,
) {
  // Map current route string to a Tab; default to Feed
  val selectedTab = tabList.find { it.destination.route == selectedRoute }
  BottomNavigationMenu(
      tabs = com.android.ootd.ui.navigation.tabs,
      selectedTab = selectedTab,
      onTabSelected = { tab -> onTabSelected(tab.destination) },
      modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun BottomNavigationMenuPreview() {
  Surface { BottomNavigationMenu(selectedTab = Tab.Feed, onTabSelected = {}, tabs = tabs) }
}
