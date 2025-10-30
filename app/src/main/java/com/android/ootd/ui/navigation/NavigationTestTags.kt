package com.android.ootd.ui.navigation

object NavigationTestTags {
  /**
   * const val BOTTOM_NAVIGATION_MENU = "BottomNavigationMenu" const val GO_BACK_BUTTON =
   * "GoBackButton" const val TOP_BAR_TITLE = "TopBarTitle" const val OVERVIEW_TAB = "OverviewTab"
   * const val MAP_TAB = "MapTab"
   */
  const val SPLASH = "Route_Splash"
  const val BOTTOM_NAVIGATION_MENU = "BottomNavigationMenu"

  fun getTabTestTag(tab: Tab): String =
      when (tab) {
        is Tab.Feed -> "FeedTab"
        is Tab.Search -> "SearchTab"
        is Tab.Inventory -> "InventoryTab"
        is Tab.Account -> "AccountTab"
      }
}
