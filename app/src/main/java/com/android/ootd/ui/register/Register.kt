package com.android.ootd.ui.register

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.R
import com.android.ootd.ui.theme.Bodoni
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.Tertiary
import com.android.ootd.ui.theme.Typography

/**
 * Test tags for the Register screen components. Used to identify UI elements in automated tests.
 */
object RegisterScreenTestTags {
  /** Test tag for the app logo image */
  const val APP_LOGO = "appLogo"

  /** Test tag for the username input field */
  const val INPUT_REGISTER_UNAME = "inputRegisterUname"

  /** Test tag for the date input field */
  const val INPUT_REGISTER_DATE = "inputRegisterDate"

  /** Test tag for the location input field */
  const val INPUT_REGISTER_LOCATION = "inputRegisterLocation"

  /** Test tag for the save/register button */
  const val REGISTER_SAVE = "registerSave"

  /** Test tag for error message text */
  const val ERROR_MESSAGE = "errorMessage"

  /** Test tag for the welcome title text */
  const val WELCOME_TITLE = "welcomeTitle"
}

/**
 * Register screen composable that allows users to create a new account.
 *
 * This screen displays a username input field and a save button. It validates the username by
 * checking if it's blank only after the user has interacted with the field and left it. The screen
 * shows loading state during registration and navigates to the next screen upon success.
 *
 * @param modelView The ViewModel that manages the registration state and logic. Defaults to a new
 *   instance.
 * @param onRegister Callback invoked when registration is successful. Defaults to an empty lambda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(modelView: RegisterViewModel = viewModel(), onRegister: () -> Unit = {}) {
  val registerUiState by modelView.uiState.collectAsState()
  val errorMsg = registerUiState.errorMsg

  val context = LocalContext.current
  var touchedUserName by remember { mutableStateOf(false) }
  var leftUsername by remember { mutableStateOf(false) }
  var textColor by remember { mutableStateOf(Tertiary) }
  val disabledLabelColor = if (registerUiState.isLoading) Secondary else Tertiary

  val usernameError = leftUsername && registerUiState.username.isBlank()

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      modelView.clearErrorMsg()
    }
  }

  // Navigate only when a successful registration is signaled
  LaunchedEffect(registerUiState.registered) {
    if (registerUiState.registered) {
      modelView.markRegisteredHandled()
      onRegister()
    }
  }

  Scaffold { innerPadding ->
    Column(
        modifier = Modifier.padding(innerPadding).padding(horizontal = 16.dp).fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
              text = "Welcome!\n Please register your account",
              fontFamily = Bodoni,
              color = Primary,
              style = Typography.displayMedium,
              textAlign = TextAlign.Center,
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(bottom = 16.dp)
                      .testTag(RegisterScreenTestTags.WELCOME_TITLE))

          Spacer(modifier = Modifier.height(60.dp))

          Image(
              painter = painterResource(id = R.drawable.app_logo),
              contentDescription = "Logo",
              contentScale = ContentScale.FillBounds,
              modifier =
                  Modifier.width(250.dp).height(250.dp).testTag(RegisterScreenTestTags.APP_LOGO))
          Spacer(modifier = Modifier.height(30.dp))

          OutlinedTextField(
              value = registerUiState.username,
              onValueChange = { modelView.setUsername(it) },
              label = { Text(text = "Username", color = textColor, fontFamily = Bodoni) },
              placeholder = {
                Text(text = "Enter your username", color = textColor, fontFamily = Bodoni)
              },
              singleLine = true,
              modifier =
                  Modifier.fillMaxWidth()
                      .testTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME)
                      .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                          touchedUserName = true
                          textColor = Primary
                        } else {
                          if (touchedUserName) leftUsername = true
                        }
                      },
              isError = usernameError,
              enabled = !registerUiState.isLoading)

          if (usernameError) {
            textColor = Color.Red
            Text(
                text = "Please enter a valid username",
                color = Color.Red,
                fontFamily = Bodoni,
                modifier = Modifier.padding(8.dp).testTag(RegisterScreenTestTags.ERROR_MESSAGE))
          }

          Spacer(modifier = Modifier.height(30.dp))

          Button(
              onClick = { modelView.registerUser() },
              modifier = Modifier.fillMaxWidth().testTag(RegisterScreenTestTags.REGISTER_SAVE),
              enabled =
                  !registerUiState.isLoading &&
                      touchedUserName &&
                      registerUiState.username.isNotBlank(),
              colors =
                  ButtonDefaults.buttonColors(
                      containerColor = Primary, disabledContentColor = disabledLabelColor)) {
                if (registerUiState.isLoading) {
                  Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Primary)
                        Text(text = "Saving…", color = textColor)
                      }
                } else {
                  Text("Save")
                }
              }
        }
  }
}
