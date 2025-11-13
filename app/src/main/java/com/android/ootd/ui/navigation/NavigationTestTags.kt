package com.android.ootd.ui.navigation

object NavigationTestTags {
  /**
   * const val BOTTOM_NAVIGATION_MENU = "BottomNavigationMenu" const val GO_BACK_BUTTON =
   * "GoBackButton" const val TOP_BAR_TITLE = "TopBarTitle" const val OVERVIEW_TAB = "OverviewTab"
   * const val MAP_TAB = "MapTab"
   */
  const val BOTTOM_NAVIGATION_MENU = "BottomNavigationMenu"
  const val FEED_TAB = "FeedTab"
  const val SEARCH_TAB = "SearchTab"
  const val INVENTORY_TAB = "InventoryTab"
  const val ACCOUNT_TAB = "AccountTab"
  const val MAP_TAB = "MapTab"

  fun getTabTestTag(tab: Tab): String =
      when (tab) {
        is Tab.Feed -> FEED_TAB
        is Tab.Search -> SEARCH_TAB
        is Tab.Inventory -> INVENTORY_TAB
        is Tab.Account -> ACCOUNT_TAB
        is Tab.Map -> MAP_TAB
      }
}
