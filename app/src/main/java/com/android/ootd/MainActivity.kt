package com.android.ootd

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.credentials.CredentialManager
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.android.ootd.ui.account.AccountScreen
import com.android.ootd.ui.account.InventoryScreen
import com.android.ootd.ui.authentication.SignInScreen
import com.android.ootd.ui.authentication.SplashScreen
import com.android.ootd.ui.feed.FeedScreen
import com.android.ootd.ui.navigation.BottomNavigationBar
import com.android.ootd.ui.navigation.NavigationActions
import com.android.ootd.ui.navigation.Screen
import com.android.ootd.ui.post.AddItemsScreen
import com.android.ootd.ui.post.EditItemsScreen
import com.android.ootd.ui.post.FitCheckScreen
import com.android.ootd.ui.post.PreviewItemScreen
import com.android.ootd.ui.register.RegisterScreen
import com.android.ootd.ui.search.UserSearchScreen
import com.android.ootd.ui.theme.OOTDTheme

/** Activity that hosts the app's Compose UI. */
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      OOTDTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
          OOTDApp()
        }
      }
    }
  }
}

/**
 * Root composable that hosts the app navigation graph.
 *
 * This composable:
 * - creates a [NavigationActions],
 * - accepts a [CredentialManager] for future auth usage.
 *
 * @param context Compose-provided [Context], defaults to [LocalContext].
 * @param credentialManager Default [CredentialManager] instance for authentication flows.
 */
@Composable
fun OOTDApp(
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context),
    testNavController: NavHostController? = null,
    testStartDestination: String? = null,
) {
  val navController = testNavController ?: rememberNavController()
  val navigationActions = remember { NavigationActions(navController) }
  val startDestination = testStartDestination ?: Screen.Splash.route

  // Observe nav backstack to reactively show the bottom bar
  val navBackStackEntry = navController.currentBackStackEntryAsState()
  val selectedRoute = navBackStackEntry.value?.destination?.route ?: startDestination
  val showBottomBar =
      selectedRoute in
          listOf(
              Screen.Feed.route,
              Screen.SearchScreen.route,
              Screen.InventoryScreen.route,
              Screen.Account.route)

  Scaffold(
      bottomBar = {
        if (showBottomBar) {
          BottomNavigationBar(
              selectedRoute = selectedRoute,
              onTabSelected = { screen -> navigationActions.navigateTo(screen) })
        }
      }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)) {
              // 1. Splash route (top-level, for all users)
              navigation(startDestination = Screen.Splash.route, route = Screen.Splash.name) {
                composable(Screen.Splash.route) {
                  SplashScreen(
                      onSignedIn = { navigationActions.navigateTo(Screen.Feed) },
                      onNotSignedIn = { navigationActions.navigateTo(Screen.Authentication) })
                }
                composable(Screen.RegisterUsername.route) {
                  RegisterScreen(onRegister = { navigationActions.navigateTo(Screen.Feed) })
                }
              }

              // 2. SignIn route (top-level, for unauthenticated users)
              navigation(
                  startDestination = Screen.Authentication.route,
                  route = Screen.Authentication.name) {
                    composable(Screen.Authentication.route) {
                      SignInScreen(
                          credentialManager = credentialManager,
                          onSignedIn = { navigationActions.navigateTo(Screen.Feed) },
                          onRegister = { navigationActions.navigateTo(Screen.RegisterUsername) })
                    }
                  }

              // 3. FeedScreen route (top-level, for authenticated users)
              navigation(startDestination = Screen.Feed.route, route = Screen.Feed.name) {
                composable(Screen.Feed.route) {
                  FeedScreen(
                      onAddPostClick = {
                        navigationActions.navigateTo(Screen.FitCheck)
                      }, // this will go to AddItemScreen
                      onSearchClick = { navigationActions.navigateTo(Screen.SearchScreen) },
                      onAccountIconClick = { navigationActions.navigateTo(Screen.Account) })
                }
                composable(Screen.SearchScreen.route) {
                  UserSearchScreen(onBack = { navigationActions.goBack() })
                }
                composable(Screen.Account.route) {
                  AccountScreen(
                      onBack = { navigationActions.goBack() },
                      onEditAvatar = { /*TODO: handle edit avatar*/},
                      onSignOut = { navigationActions.navigateTo(Screen.Authentication) })
                }

                composable(Screen.InventoryScreen.route) { InventoryScreen() }

                composable(Screen.FitCheck.route) {
                  FitCheckScreen(
                      onNextClick = { navigationActions.navigateTo(Screen.PreviewItemScreen) },
                      onBackClick = { navigationActions.goBack() })
                }

                composable(Screen.PreviewItemScreen.route) {
                  PreviewItemScreen(
                      onEditItem = { itemUuid ->
                        navigationActions.navigateTo(Screen.EditItem(itemUuid))
                      },
                      onAddItem = { navigationActions.navigateTo(Screen.AddItemScreen) },
                      onPostOutfit = { navigationActions.popUpTo(Screen.Feed.route) },
                      onGoBack = { navigationActions.goBack() },
                  )
                }

                composable(Screen.AddItemScreen.route) {
                  AddItemsScreen(
                      onNextScreen = { navigationActions.popUpTo(Screen.PreviewItemScreen.route) },
                      goBack = { navigationActions.goBack() },
                  )
                }

                /* TODO: add navigation to ProfileScreen*/
                // Navigation to User Profile screen is not yet implemented

                composable(
                    route = Screen.EditItem.route,
                    arguments = listOf(navArgument("itemUid") { type = NavType.StringType })) {
                        navBackStackEntry ->
                      val itemUid = navBackStackEntry.arguments?.getString("itemUid")

                      if (itemUid != null) {
                        EditItemsScreen(itemUuid = itemUid, goBack = { navigationActions.goBack() })
                      }
                    }
              }
            }
      }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  OOTDTheme { Text("Preview") }
}
