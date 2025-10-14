package com.android.ootd

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.credentials.CredentialManager
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.android.ootd.ui.authentication.SignInScreen
import com.android.ootd.ui.authentication.SplashScreen
import com.android.ootd.ui.feed.FeedScreen
import com.android.ootd.ui.navigation.NavigationActions
import com.android.ootd.ui.navigation.Screen
import com.android.ootd.ui.post.EditItemsScreen
import com.android.ootd.ui.register.RegisterScreen
import com.android.ootd.ui.theme.OOTDTheme
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage

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
    storage: FirebaseStorage = Firebase.storage
) {
  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController)
  val startDestination = Screen.Splash.route

  NavHost(navController = navController, startDestination = startDestination) {
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
    navigation(startDestination = Screen.Authentication.route, route = Screen.Authentication.name) {
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
            onAddPostClick = { /* TODO: handle add post */}, // this will go to AddItemScreen
            onSearchClick = { /* TODO: show search profile page */},
            onProfileClick = { /* TODO: show user profile page */})
      }

      /* TODO: add navigation to ProfileScreen and SearchScreen */
      // Navigation to Search screen is not yet implemented

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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  OOTDTheme { Text("Preview") }
}
