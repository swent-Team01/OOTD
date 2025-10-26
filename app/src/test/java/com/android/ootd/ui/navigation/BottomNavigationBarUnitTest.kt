package com.android.ootd.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class BottomNavigationBarUnitTest {

  @Test
  fun routeToTab_mapsKnownRoutes_toCorrectTabs() {
    assertEquals(Tab.Feed, routeToTab(Screen.Feed.route))
    assertEquals(Tab.Search, routeToTab(Screen.SearchScreen.route))
    assertEquals(Tab.Inventory, routeToTab(Screen.InventoryScreen.route))
    assertEquals(Tab.Profile, routeToTab(Screen.Account.route))
  }

  @Test
  fun routeToTab_unknownRoute_defaultsToFeed() {
    assertEquals(Tab.Feed, routeToTab("unknown_route"))
  }

  @Test
  fun tabToScreen_mapsTabs_toCorrectScreens() {
    assertEquals(Screen.Feed, tabToScreen(Tab.Feed))
    assertEquals(Screen.SearchScreen, tabToScreen(Tab.Search))
    assertEquals(Screen.InventoryScreen, tabToScreen(Tab.Inventory))
    assertEquals(Screen.Account, tabToScreen(Tab.Profile))
  }

  @Test
  fun getTabTestTag_returnsStableNames() {
    assertEquals("FeedTab", NavigationTestTags.getTabTestTag(Tab.Feed))
    assertEquals("SearchTab", NavigationTestTags.getTabTestTag(Tab.Search))
    assertEquals("InventoryTab", NavigationTestTags.getTabTestTag(Tab.Inventory))
    assertEquals("ProfileTab", NavigationTestTags.getTabTestTag(Tab.Profile))
  }
}
