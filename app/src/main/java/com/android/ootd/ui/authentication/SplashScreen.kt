package com.android.ootd.ui.authentication

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.R
import com.android.ootd.ui.theme.Background
import com.android.ootd.ui.theme.Primary
import kotlinx.coroutines.delay

private const val SPLASH_TIMEOUT = 1000L
private val splashProgressTab = "splashProgress"
private val splashLogoTag = "splashLogo"

/**
 * UI content for the app splash screen.
 *
 * This composable is intentionally stateless and contains only layout and visual elements so it can
 * be safely used inside `@Preview` functions. It centers the app's hanger image and a progress
 * indicator vertically and horizontally.
 *
 * @param modifier Optional [Modifier] to be applied to the root container. Defaults to [Modifier].
 */
@Composable
fun SplashScreenContent(modifier: Modifier = Modifier) {
  Column(
      modifier =
          modifier
              .fillMaxWidth()
              .fillMaxHeight()
              .background(Background)
              .verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(id = R.drawable.hanger),
            contentDescription = null,
            modifier = Modifier.size(128.dp).padding(bottom = 16.dp).testTag(splashLogoTag))

        CircularProgressIndicator(color = Primary, modifier = Modifier.testTag(splashProgressTab))
      }
}

/**
 * Full splash screen entry point used in the running app.
 *
 * This composable composes [SplashScreenContent] and performs production side effects such as
 * notifying the [SplashViewModel] that the app started and delaying for [SPLASH_TIMEOUT]
 * milliseconds before continuing navigation.
 *
 * @param modifier Optional [Modifier] applied to the content container.
 * @param onSignedIn: Navigation callback invoked if the user is signed in.
 * @param onNotSignedIn: Navigation callback invoked if the user is not signed in.
 * @param viewModel The [SplashViewModel] instance to notify about app start; provided by
 *   `viewModel()` by default.
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    onSignedIn: () -> Unit = {},
    onNotSignedIn: () -> Unit = {},
    viewModel: SplashViewModel = viewModel()
) {
  SplashScreenContent(modifier)

  LaunchedEffect(Unit) {
    delay(SPLASH_TIMEOUT)
    viewModel.onAppStart(onSignedIn = onSignedIn, onNotSignedIn = onNotSignedIn)
  }
}

/**
 * Preview of the splash screen UI.
 *
 * This preview uses the stateless [SplashScreenContent] so it can be rendered inside Android
 * Studio's Preview tooling without requiring a `ViewModelStoreOwner` or lifecycle.
 */
@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
  SplashScreenContent(modifier = Modifier.fillMaxSize())
}
