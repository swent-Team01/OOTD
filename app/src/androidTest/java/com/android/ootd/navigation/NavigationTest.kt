package com.android.ootd.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.android.ootd.ui.navigation.NavigationActions
import com.android.ootd.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NavigationTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var navController: NavHostController
  private lateinit var navigation: NavigationActions

  @Before
  fun setUp() {
    composeRule.setContent {
      val controller = rememberNavController()
      navController = controller

      NavHost(navController = controller, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) { /* minimal screen */}
        composable(Screen.Authentication.route) { /* minimal screen */}
      }
    }
    composeRule.runOnIdle { navigation = NavigationActions(navController) }
  }

  @Test
  fun basicNavigationTests() {
    composeRule.runOnIdle {
      navigation.navigateTo(Screen.Authentication)
      assertEquals(Screen.Authentication.route, navigation.currentRoute())

      navigation.goBack()
      assertEquals(Screen.Splash.route, navigation.currentRoute())
    }

    composeRule.runOnIdle {
      navController.navigate(Screen.Authentication.route)
      assertEquals(Screen.Authentication.route, navController.currentDestination?.route)
    }
  }
}
