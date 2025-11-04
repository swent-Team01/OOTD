package com.android.ootd

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import com.android.ootd.ui.notifications.NotificationsScreen
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
              Screen.Account.route,
              Screen.NotificationsScreen.route)

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
                      onAddPostClick = { navigationActions.navigateTo(Screen.FitCheck()) },
                      onSearchClick = { navigationActions.navigateTo(Screen.SearchScreen) },
                      onNotificationIconClick = {
                        navigationActions.navigateTo(Screen.NotificationsScreen)
                      })
                }
                composable(Screen.SearchScreen.route) {
                  UserSearchScreen(onBack = { navigationActions.goBack() })
                }
                composable(Screen.Account.route) {
                  AccountScreen(
                      onBack = { navigationActions.goBack() },
                      onSignOut = { navigationActions.navigateTo(Screen.Authentication) })
                }

                composable(Screen.InventoryScreen.route) { InventoryScreen() }

                composable(
                    route = Screen.FitCheck.route,
                    arguments =
                        listOf(
                            navArgument("postUuid") {
                              type = NavType.StringType
                              defaultValue = ""
                            })) { backStackEntry ->
                      val postUuid = backStackEntry.arguments?.getString("postUuid") ?: ""

                      FitCheckScreen(
                          postUuid = postUuid,
                          onNextClick = { imageUri, description ->
                            navigationActions.navigateTo(
                                Screen.PreviewItemScreen(imageUri, description))
                          },
                          onBackClick = {
                            // later we'll use postUuid to delete items
                            navigationActions.goBack()
                          })
                    }

                composable(
                    route = Screen.PreviewItemScreen.route,
                    arguments =
                        listOf(
                            navArgument("imageUri") { type = NavType.StringType },
                            navArgument("description") { type = NavType.StringType })) {
                        backStackEntry ->
                      val imageUri = backStackEntry.arguments?.getString("imageUri") ?: ""
                      val description = backStackEntry.arguments?.getString("description") ?: ""

                      PreviewItemScreen(
                          imageUri = imageUri,
                          description = description,
                          onAddItem = { postUuid ->
                            navController.navigate(Screen.AddItemScreen(postUuid).route)
                          },
                          onEditItem = { itemUuid ->
                            navController.navigate(Screen.EditItem(itemUuid).route)
                          },
                          onPostSuccess = {
                            Log.d("Navigation", "Post successful, navigating to Feed")
                            navController.navigate(Screen.Feed.route) {
                              popUpTo(Screen.Feed.route) { inclusive = true }
                              launchSingleTop = true
                            }
                          },
                          onGoBack = { postUuid ->
                            navController.navigate(Screen.FitCheck(postUuid).route) {
                              popUpTo(Screen.Feed.route) { inclusive = false }
                              launchSingleTop = true
                            }
                          })
                    }

                composable(
                    route = Screen.AddItemScreen.route,
                    arguments = listOf(navArgument("postUuid") { type = NavType.StringType })) {
                        backStackEntry ->
                      val postUuid = backStackEntry.arguments?.getString("postUuid") ?: ""
                      AddItemsScreen(
                          postUuid = postUuid,
                          onNextScreen = { navController.popBackStack() },
                          goBack = { navController.popBackStack() })
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
                composable(route = Screen.NotificationsScreen.route) { NotificationsScreen() }
              }
            }
      }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  OOTDTheme { Text("Preview") }
}
