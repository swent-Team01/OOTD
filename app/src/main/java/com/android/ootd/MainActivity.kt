package com.android.ootd

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.android.ootd.LocationProvider.fusedLocationClient
import com.android.ootd.model.map.Location
import com.android.ootd.model.notifications.Notification
import com.android.ootd.model.notifications.NotificationRepositoryProvider
import com.android.ootd.model.notifications.scheduleBackgroundNotificationSync
import com.android.ootd.model.notifications.sendLocalNotification
import com.android.ootd.ui.Inventory.InventoryScreen
import com.android.ootd.ui.account.AccountPage
import com.android.ootd.ui.account.AccountScreen
import com.android.ootd.ui.account.ViewUserProfile
import com.android.ootd.ui.authentication.SignInScreen
import com.android.ootd.ui.authentication.SplashScreen
import com.android.ootd.ui.consent.BetaConsentScreen
import com.android.ootd.ui.consent.BetaConsentViewModel
import com.android.ootd.ui.consent.BetaConsentViewModelFactory
import com.android.ootd.ui.feed.FeedScreen
import com.android.ootd.ui.feed.SeeFitScreen
import com.android.ootd.ui.map.MapScreen
import com.android.ootd.ui.map.MapViewModelFactory
import com.android.ootd.ui.navigation.BottomNavigationBar
import com.android.ootd.ui.navigation.NavigationActions
import com.android.ootd.ui.navigation.Screen
import com.android.ootd.ui.notifications.NotificationsScreen
import com.android.ootd.ui.post.FitCheckScreen
import com.android.ootd.ui.post.PostViewScreen
import com.android.ootd.ui.post.PreviewItemScreen
import com.android.ootd.ui.post.items.AddItemsScreen
import com.android.ootd.ui.post.items.EditItemsScreen
import com.android.ootd.ui.register.RegisterScreen
import com.android.ootd.ui.search.UserSearchScreen
import com.android.ootd.ui.theme.OOTDTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import okhttp3.OkHttpClient

/**
 * Provide an OkHttpClient client for network requests.
 *
 * Property `client` is mutable for testing purposes.
 */
object HttpClientProvider {
  var client: OkHttpClient = OkHttpClient()
}

const val OOTD_CHANNEL_ID = "ootd_channel"
const val NOTIFICATION_CLICK_ACTION = "com.android.ootd.NOTIFICATION_CLICK"

/** Function to create the notification channel for push notifications */
fun createNotificationChannel(context: Context) {
  val name = "OOTD Notifications"
  val descriptionText = "Notifications for OOTD app"
  val importance = NotificationManager.IMPORTANCE_DEFAULT
  val channel =
      NotificationChannel(OOTD_CHANNEL_ID, name, importance).apply { description = descriptionText }

  val notificationManager: NotificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  notificationManager.createNotificationChannel(channel)
}

/**
 * Provide a FusedLocationProviderClient for location services.
 *
 * Property `fusedLocationClient` is mutable for testing purposes.
 */
object LocationProvider {
  lateinit var fusedLocationClient: FusedLocationProviderClient
}

/** Activity that hosts the app's Compose UI. */
class MainActivity : ComponentActivity() {
  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  override fun onCreate(savedInstanceState: Bundle?) {
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    super.onCreate(savedInstanceState)
    scheduleBackgroundNotificationSync(this)
    setContent {
      OOTDTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
          // Check if activity was launched from notification
          val shouldNavigateToUserProfile = intent?.action == NOTIFICATION_CLICK_ACTION
          val senderId = intent.getStringExtra("senderId") ?: ""
          OOTDApp(shouldNavigateToUserProfile = shouldNavigateToUserProfile, senderId = senderId)
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
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
 * @param testMode Used for overriding permission screens for testing mode
 * @param shouldNavigateToUserProfile Whether to navigate to the sender of the follow request on
 *   launch
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun OOTDApp(
    context: Context = LocalContext.current,
    credentialManager: CredentialManager = CredentialManager.create(context),
    testNavController: NavHostController? = null,
    testStartDestination: String? = null,
    testMode: Boolean = false,
    shouldNavigateToUserProfile: Boolean = false,
    senderId: String = ""
) {
  val navController = testNavController ?: rememberNavController()
  val navigationActions = remember { NavigationActions(navController) }
  val startDestination = testStartDestination ?: Screen.Splash.route

  // Navigate to notifications screen if launched from notification
  LaunchedEffect(shouldNavigateToUserProfile) {
    if (shouldNavigateToUserProfile) {
      // Wait for navigation to be ready and user to be authenticated
      if (senderId != "") {
        navigationActions.navigateTo(Screen.ViewUser(senderId))
      } else {
        // If there is no user, we just bring them to the notifications screen
        navController.navigate(Screen.NotificationsScreen.route) {
          // Don't pop the back stack, allow user to navigate back normally
          launchSingleTop = true
        }
      }
    }
  }

  // Observe nav backstack to reactively show the bottom bar
  val navBackStackEntry = navController.currentBackStackEntryAsState()
  val selectedRoute = navBackStackEntry.value?.destination?.route ?: startDestination
  val showBottomBar =
      selectedRoute in
          listOf(
              Screen.Feed.route,
              Screen.SearchScreen.route,
              Screen.InventoryScreen.route,
              Screen.AccountView.route,
              Screen.Map.route,
              Screen.MapWithLocation.route,
              Screen.NotificationsScreen.route)

  // Create ViewModel using factory to properly inject SharedPreferences
  val betaConsentViewModel: BetaConsentViewModel =
      viewModel(factory = BetaConsentViewModelFactory(context))

  val isNotificationsPermissionGranted =
      testMode ||
          ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
              PackageManager.PERMISSION_GRANTED

  // Use remember to persist listener across recompositions
  val listenerRegistration = remember { mutableListOf<ListenerRegistration?>().apply { add(null) } }

  /**
   * Creates a listener for new repositories coming in.
   *
   * If a listener was already created, it does nothing.
   */
  fun observeUnpushedNotifications(userId: String) {
    if (listenerRegistration[0] != null) return

    if (testMode) {
      sendLocalNotification(
          context,
          Notification(
              uid = "",
              senderId = "",
              receiverId = "",
              type = "",
              content = "",
              wasPushed = false,
              senderName = ""))
    }

    listenerRegistration[0] =
        NotificationRepositoryProvider.repository.listenForUnpushedNotifications(
            receiverId = userId) { notification ->
              sendLocalNotification(context, notification)
            }
  }

  // Watch for permission changes and create listener when granted
  LaunchedEffect(isNotificationsPermissionGranted) {
    val currentUserId = Firebase.auth.currentUser?.uid ?: ""
    if ((testMode || currentUserId.isNotEmpty()) && isNotificationsPermissionGranted) {
      createNotificationChannel(context)
      observeUnpushedNotifications(currentUserId)
    }
  }

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
                  RegisterScreen(
                      onRegister = {
                        // After registration, show beta consent if not already given
                        if (betaConsentViewModel.getConsentStatus()) {
                          navigationActions.navigateTo(Screen.Feed)
                        } else {
                          navigationActions.navigateTo(Screen.BetaConsent)
                        }
                      })
                }
                composable(Screen.BetaConsent.route) {
                  val consentSaved by betaConsentViewModel.consentSaved.collectAsState()
                  val isLoading by betaConsentViewModel.isLoading.collectAsState()
                  val error by betaConsentViewModel.error.collectAsState()

                  // Navigate to Feed when consent is successfully saved
                  LaunchedEffect(consentSaved) {
                    if (consentSaved) {
                      betaConsentViewModel.resetConsentSavedFlag()
                      navigationActions.navigateTo(Screen.Feed)
                    }
                  }

                  BetaConsentScreen(
                      onAgree = { betaConsentViewModel.recordConsent() },
                      onDecline = {
                        // If user declines, sign them out and return to authentication
                        navigationActions.navigateTo(Screen.Authentication)
                      },
                      isLoading = isLoading,
                      errorMessage = error,
                      onErrorDismiss = { betaConsentViewModel.clearError() })
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
                      onNotificationIconClick = {
                        navigationActions.navigateTo(Screen.NotificationsScreen)
                      },
                      onOpenPost = { postId ->
                        navigationActions.navigateTo(Screen.PostView(postId))
                      },
                      onSeeFitClick = { postUuid ->
                        navigationActions.navigateTo(Screen.SeeFitScreen(postUuid))
                      },
                      onLocationClick = { location ->
                        navigationActions.navigateTo(
                            Screen.MapWithLocation(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                locationName = location.name))
                      })
                }

                composable(Screen.SearchScreen.route) {
                  UserSearchScreen(
                      onUserClick = { userId ->
                        navigationActions.navigateTo(Screen.ViewUser(userId))
                      })
                }

                composable(Screen.AccountView.route) {
                  AccountPage(
                      onEditAccount = { navigationActions.navigateTo(Screen.AccountEdit) },
                      onPostClick = { postId ->
                        navigationActions.navigateTo(Screen.PostView(postId))
                      })
                }
                composable(Screen.AccountEdit.route) {
                  AccountScreen(
                      onBack = { navigationActions.goBack() },
                      onSignOut = { navigationActions.navigateTo(Screen.Authentication) })
                }
                composable(Screen.Map.route) {
                  MapScreen(
                      onPostClick = { postId ->
                        navigationActions.navigateTo(Screen.PostView(postId))
                      })
                }

                // Map with location parameters
                composable(
                    route = Screen.MapWithLocation.route,
                    arguments =
                        listOf(
                            navArgument("lat") { type = NavType.StringType },
                            navArgument("lon") { type = NavType.StringType },
                            navArgument("name") { type = NavType.StringType })) { backStackEntry ->
                      val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
                      val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull() ?: 0.0
                      val name = backStackEntry.arguments?.getString("name") ?: ""

                      val focusLocation = Location(latitude = lat, longitude = lon, name = name)

                      MapScreen(
                          // Create ViewModel with focus location
                          viewModel = viewModel(factory = MapViewModelFactory(focusLocation)),
                          onPostClick = { postId ->
                            navigationActions.navigateTo(Screen.PostView(postId))
                          })
                    }

                composable(Screen.InventoryScreen.route) {
                  InventoryScreen(navigationActions = navigationActions)
                }

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
                          onNextClick = { imageUri, description, location ->
                            navigationActions.navigateTo(
                                Screen.PreviewItemScreen(imageUri, description, location))
                          },
                          onBackClick = {
                            // later we'll use postUuid to delete items
                            navigationActions.goBack()
                          },
                          overridePhoto = testMode)
                    }

                composable(
                    route = Screen.SeeFitScreen.route,
                    arguments = listOf(navArgument("postUuid") { type = NavType.StringType })) {
                        navBackStackEntry ->
                      val postUuid = navBackStackEntry.arguments?.getString("postUuid") ?: ""

                      // Placeholder for SeeFitScreen
                      SeeFitScreen(
                          postUuid = postUuid,
                          goBack = { navigationActions.goBack() },
                          onEditItem = { itemUuid ->
                            navController.navigate(Screen.EditItem(itemUuid).route)
                          })
                    }

                composable(
                    route = Screen.PreviewItemScreen.route,
                    arguments =
                        listOf(
                            navArgument("imageUri") { type = NavType.StringType },
                            navArgument("description") { type = NavType.StringType },
                            navArgument("locationLat") { type = NavType.FloatType },
                            navArgument("locationLon") { type = NavType.FloatType },
                            navArgument("locationName") { type = NavType.StringType })) {
                        backStackEntry ->
                      val imageUri = backStackEntry.arguments?.getString("imageUri") ?: ""
                      val description = backStackEntry.arguments?.getString("description") ?: ""
                      val locationLat = backStackEntry.arguments?.getFloat("locationLat") ?: 0.0
                      val locationLon = backStackEntry.arguments?.getFloat("locationLon") ?: 0.0
                      val locationName = backStackEntry.arguments?.getString("locationName") ?: ""

                      // Reconstruct Location object from navigation arguments
                      val location =
                          Location(
                              latitude = locationLat.toDouble(),
                              longitude = locationLon.toDouble(),
                              name = locationName)

                      PreviewItemScreen(
                          imageUri = imageUri,
                          description = description,
                          location = location,
                          onAddItem = { postUuid ->
                            navController.navigate(Screen.AddItemScreen(postUuid).route)
                          },
                          onSelectFromInventory = { postUuid ->
                            navController.navigate(Screen.SelectInventoryItem(postUuid).route)
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
                          onGoBack = { navController.popBackStack() },
                          overridePhoto = testMode)
                    }

                composable(
                    route = Screen.AddItemScreen.route,
                    arguments = listOf(navArgument("postUuid") { type = NavType.StringType })) {
                        backStackEntry ->
                      val postUuid = backStackEntry.arguments?.getString("postUuid") ?: ""
                      AddItemsScreen(
                          postUuid = postUuid,
                          onNextScreen = { navController.popBackStack() },
                          goBack = { navController.popBackStack() },
                          overridePhoto = testMode)
                    }

                composable(
                    route = Screen.EditItem.route,
                    arguments = listOf(navArgument("itemUid") { type = NavType.StringType })) {
                        navBackStackEntry ->
                      val itemUid = navBackStackEntry.arguments?.getString("itemUid")

                      if (itemUid != null) {
                        EditItemsScreen(itemUuid = itemUid, goBack = { navigationActions.goBack() })
                      }
                    }
                composable(
                    route = Screen.PostView.route,
                    arguments = listOf(navArgument("postId") { type = NavType.StringType })) {
                        navBackStackEntry ->
                      val postId = navBackStackEntry.arguments?.getString("postId")

                      if (postId != null) {
                        PostViewScreen(postId = postId, onBack = { navigationActions.goBack() })
                      }
                    }

                composable(
                    route = Screen.ViewUser.ROUTE,
                    arguments = listOf(navArgument("userId") { type = NavType.StringType })) {
                        navBackStackEntry ->
                      val userId = navBackStackEntry.arguments?.getString("userId")
                      if (userId != null) {
                        ViewUserProfile(
                            userId = userId,
                            onBackButton = { navigationActions.goBack() },
                            onPostClick = { postId ->
                              navigationActions.navigateTo(Screen.PostView(postId))
                            })
                      }
                    }

                composable(
                    route = Screen.SelectInventoryItem.route,
                    arguments = listOf(navArgument("postUuid") { type = NavType.StringType })) {
                        backStackEntry ->
                      val postUuid = backStackEntry.arguments?.getString("postUuid") ?: ""
                      com.android.ootd.ui.post.SelectInventoryItemScreen(
                          postUuid = postUuid,
                          onItemSelected = { navController.popBackStack() },
                          onGoBack = { navController.popBackStack() })
                    }

                composable(route = Screen.NotificationsScreen.route) {
                  NotificationsScreen(testMode = testMode)
                }
              }
            }
      }
}
