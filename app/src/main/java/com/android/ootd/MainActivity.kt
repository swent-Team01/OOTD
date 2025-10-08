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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.android.ootd.ui.authentication.SignInScreen
import com.android.ootd.ui.authentication.SplashScreen
import com.android.ootd.ui.navigation.NavigationActions
import com.android.ootd.ui.navigation.Screen
import com.android.ootd.ui.register.RegisterScreen
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
 * - creates a [NavHostController] and [NavigationActions],
 * - accepts a [CredentialManager] for future auth usage.
 *
 * @param context Compose-provided [Context], defaults to [LocalContext].
 * @param credentialManager Default [CredentialManager] instance for authentication flows.
 */
@Composable
fun OOTDApp(
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context),
) {
  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController)
  val startDestination = Screen.Splash.route

  NavHost(navController = navController, startDestination = startDestination) {
    // 1. Splash route (top-level, for all users)
    navigation(startDestination = Screen.Splash.route, route = Screen.Splash.name) {
      composable(Screen.Splash.route) {
        SplashScreen(
            onSignedIn = { navigationActions.navigateTo(Screen.Overview) },
            onNotSignedIn = { navigationActions.navigateTo(Screen.Authentication) })
      }
      composable(Screen.CreateAccount.route) {
        RegisterScreen(onRegister = { navigationActions.navigateTo(Screen.Overview) })
      }
    }

    // 2. SignIn route (top-level, for unauthenticated users)
    navigation(startDestination = Screen.Authentication.route, route = Screen.Authentication.name) {
      composable(Screen.Authentication.route) {
        SignInScreen(
            credentialManager = credentialManager,
            onSignedIn = { navigationActions.navigateTo(Screen.Overview) })
      }
    }

    // 3. Overview route (top-level, for authenticated users)
    // Todo: Replace overview with main when implemented
    navigation(startDestination = Screen.Overview.route, route = Screen.Overview.name) {
      composable(Screen.Overview.route) { Text("Overview Placeholder") }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  OOTDTheme { Text("Preview") }
}
