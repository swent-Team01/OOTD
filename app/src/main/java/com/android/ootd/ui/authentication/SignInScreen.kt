package com.android.ootd.ui.authentication

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.R
import com.android.ootd.ui.theme.Background
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Typography

object SignInScreenTestTags {
  const val APP_LOGO = "appLogo"
  const val LOGIN_TITLE = "loginTitle"
  const val LOGIN_BUTTON = "loginButton"
}

/**
 * A composable function for the SignIn screen, which includes functionality for user login.
 *
 * @param authViewModel The ViewModel that manages authentication state. Defaults to a view model
 *   instance for SignIn.
 * @param credentialManager Manages user credentials, created from the current context.
 * @param onSignedIn Callback invoked when the user successfully signs in to the app.
 * @param onRegister Callback invoked to navigate to the registration screen.
 */
@Preview
@Composable
fun SignInScreen(
    authViewModel: SignInViewModel = viewModel(),
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onSignedIn: () -> Unit = {},
    onRegister: () -> Unit = {}
) {
  val context = LocalContext.current
  val uiState by authViewModel.uiState.collectAsState()

  val screenHeight = LocalConfiguration.current.screenHeightDp.dp

  // Show error message if login fails
  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      authViewModel.clearErrorMsg()
    }
  }

  LaunchedEffect(uiState.user?.uid) {
    Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show()
    authViewModel.routeAfterGoogleSignIn(
        onSignedIn = onSignedIn,
        onRegister = onRegister,
        onFailure = { err ->
          Toast.makeText(
                  context,
                  "Post sign-in routing failed: ${err.localizedMessage}",
                  Toast.LENGTH_SHORT)
              .show()
        })
  }

  Scaffold { innerPadding ->
    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top) {
          // Use a fraction of the screen height so the content group shifts lower on taller screens
          Spacer(modifier = Modifier.height(screenHeight * 0.18f))

          Text(
              text = "WELCOME",
              style = Typography.headlineMedium,
              color = Primary,
              modifier = Modifier.padding(top = 8.dp).testTag(SignInScreenTestTags.LOGIN_TITLE))

          Spacer(modifier = Modifier.height(24.dp))

          Box(modifier = Modifier.size(320.dp), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "OOTD Logo",
                modifier = Modifier.size(360.dp).testTag(SignInScreenTestTags.APP_LOGO),
                contentScale = ContentScale.Fit,
            )
          }

          Spacer(modifier = Modifier.height(36.dp))

          if (uiState.isLoading) {
            CircularProgressIndicator(color = Primary, modifier = Modifier.size(48.dp))
          } else {
            GoogleSignInButton(
                onClick = { authViewModel.signIn(context, credentialManager) },
                modifier = Modifier.testTag(SignInScreenTestTags.LOGIN_BUTTON))
          }

          // Add a small bottom spacer to keep some breathing room on shorter screens
          Spacer(modifier = Modifier.height(24.dp))
        }
  }
}

@Composable
fun GoogleSignInButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
  OutlinedButton(
      onClick = onClick,
      shape = RoundedCornerShape(50),
      modifier = modifier,
      border = androidx.compose.foundation.BorderStroke(1.dp, Primary),
      colors = ButtonDefaults.outlinedButtonColors(containerColor = Background)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
              modifier =
                  Modifier.size(28.dp)
                      .background(Background, shape = CircleShape)
                      .border(1.dp, Primary, CircleShape),
              contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = R.drawable.google_logo),
                    contentDescription = "Google logo",
                    modifier = Modifier.size(18.dp))
              }

          Spacer(modifier = Modifier.size(12.dp))

          Text(text = "Sign in with Google", style = Typography.titleLarge, color = Primary)
        }
      }
}
