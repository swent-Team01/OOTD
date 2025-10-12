package com.android.ootd.ui.register

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.R
import com.android.ootd.ui.theme.Bodoni
import com.android.ootd.ui.theme.OOTDTheme
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.Tertiary
import com.android.ootd.ui.theme.Typography
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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

  /** Test tag for the date Picker */
  const val REGISTER_DATE_PICKER = "datePicker"

  /** Test tag for the date picker icon */
  const val DATE_PICKER_ICON = "iconDatePicker"

  /** Test tag for the location input field */
  const val INPUT_REGISTER_LOCATION = "inputRegisterLocation"

  /** Test tag for the app's slogan */
  const val REGISTER_APP_SLOGAN = "appSlogan"

  /** Test tag for the save/register button */
  const val REGISTER_SAVE = "registerSave"

  /** Test tag for error message text */
  const val ERROR_MESSAGE = "errorMessage"

  /** Test tag for the welcome title text */
  const val WELCOME_TITLE = "welcomeTitle"

  /** Test tag for the circular progression when loading */
  const val REGISTER_LOADING = "registerLoading"
}

private val SPACER = 38.dp

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
fun RegisterScreen(viewModel: RegisterViewModel = viewModel(), onRegister: () -> Unit = {}) {
  val registerUiState by viewModel.uiState.collectAsState()
  val errorMsg = registerUiState.errorMsg
  val context = LocalContext.current

  var touchedUserName by remember { mutableStateOf(false) }
  var leftUsername by remember { mutableStateOf(false) }
  var textColorUname by remember { mutableStateOf(Tertiary) }
  var focusUname by remember { mutableStateOf(false) }

  var touchedDate by remember { mutableStateOf(false) }
  var leftDate by remember { mutableStateOf(false) }
  var showDatePicker by remember { mutableStateOf(false) }
  var textColorDate by remember { mutableStateOf(Tertiary) }
  var focusDate by remember { mutableStateOf(false) }
  val disabledLabelColor = if (registerUiState.isLoading) Secondary else Tertiary

  val usernameError = leftUsername && registerUiState.username.isBlank()
  val dateError = leftDate && registerUiState.dateOfBirth.isBlank()
  val anyError = usernameError || dateError

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      viewModel.clearErrorMsg()
    }
  }

  LaunchedEffect(registerUiState.registered) {
    if (registerUiState.registered) {
      viewModel.markRegisteredHandled()
      onRegister()
    }
  }
  LaunchedEffect(focusUname) {
    textColorUname =
        when {
          usernameError && !focusUname -> Color.Red
          touchedUserName -> Primary
          else -> Tertiary
        }
  }

  LaunchedEffect(focusDate) {
    textColorDate =
        when {
          dateError && !focusDate -> Color.Red
          touchedDate -> Primary
          else -> Tertiary
        }
  }
  LaunchedEffect(registerUiState.isLoading) {
    if (registerUiState.isLoading) showDatePicker = false
  }

  Scaffold { innerPadding ->
    Column(
        modifier = Modifier.padding(innerPadding).padding(horizontal = 16.dp).fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
          Image(
              painter = painterResource(id = R.drawable.app_logo),
              contentDescription = "Logo",
              contentScale = ContentScale.FillBounds,
              modifier =
                  Modifier.width(237.dp).height(237.dp).testTag(RegisterScreenTestTags.APP_LOGO))

          Text(
              text = "Welcome\nTime to drop a fit.",
              fontFamily = Bodoni,
              color = Primary,
              style = Typography.displayMedium,
              textAlign = TextAlign.Center,
              modifier =
                  Modifier.fillMaxWidth()
                      .padding(bottom = 16.dp)
                      .testTag(RegisterScreenTestTags.WELCOME_TITLE))

          Spacer(modifier = Modifier.height(SPACER))

          OutlinedTextField(
              value = registerUiState.username,
              onValueChange = { viewModel.setUsername(it) },
              label = { Text(text = "Username", color = textColorUname, fontFamily = Bodoni) },
              placeholder = {
                Text(text = "Enter your username", color = textColorUname, fontFamily = Bodoni)
              },
              singleLine = true,
              modifier =
                  Modifier.fillMaxWidth()
                      .testTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME)
                      .onFocusChanged { focusState ->
                        focusUname = focusState.isFocused
                        if (focusUname) {
                          touchedUserName = true
                        } else {
                          if (touchedUserName) leftUsername = true
                        }
                      },
              isError = usernameError && !focusUname,
              enabled = !registerUiState.isLoading)

          if (usernameError && !focusUname) {
            Text(
                text = "Please enter a valid username",
                color = Color.Red,
                fontFamily = Bodoni,
                modifier = Modifier.padding(8.dp).testTag(RegisterScreenTestTags.ERROR_MESSAGE))
          }

          Spacer(modifier = Modifier.height(SPACER))

          OutlinedTextField(
              value = registerUiState.dateOfBirth,
              onValueChange = {},
              label = { Text(text = "Date of Birth", color = textColorDate, fontFamily = Bodoni) },
              placeholder = {
                Text(text = "DD/MM/YYYY", color = textColorDate, fontFamily = Bodoni)
              },
              readOnly = true,
              singleLine = true,
              modifier =
                  Modifier.fillMaxWidth()
                      .testTag(RegisterScreenTestTags.INPUT_REGISTER_DATE)
                      .onFocusChanged { focusState ->
                        focusDate = focusState.isFocused
                        if (focusDate) {
                          touchedDate = true
                        } else {
                          if (touchedDate) leftDate = true
                        }
                      },
              isError = dateError && !focusDate,
              enabled = !registerUiState.isLoading,
              trailingIcon = {
                IconButton(
                    onClick = {
                      if (!registerUiState.isLoading) {
                        showDatePicker = true
                        touchedDate = true
                      }
                    }) {
                      Icon(
                          Icons.Default.DateRange,
                          contentDescription = "Select date",
                          modifier = Modifier.testTag(RegisterScreenTestTags.DATE_PICKER_ICON))
                    }
              })

          if (showDatePicker) {
            DatePickerModalInput(
                onDateSelected = { millis ->
                  millis?.let { viewModel.setDateOfBirth(formatSelectedDate(it)) }
                },
                onDismiss = { showDatePicker = false },
                disabledLabelColor = disabledLabelColor)
          }
          if (dateError && !focusDate) {
            Text(
                text = "Please enter a valid date",
                color = Color.Red,
                fontFamily = Bodoni,
                modifier = Modifier.padding(8.dp).testTag(RegisterScreenTestTags.ERROR_MESSAGE))
          }

          Spacer(modifier = Modifier.height(SPACER))

          Text(
              text = "Outfit Of The Day,\n Inspire Drip",
              fontFamily = Bodoni,
              fontSize = 24.sp,
              color = Primary,
              fontWeight = FontWeight(400),
              textAlign = TextAlign.Center,
              modifier = Modifier.testTag(RegisterScreenTestTags.REGISTER_APP_SLOGAN))

          Spacer(modifier = Modifier.height(SPACER))

          Button(
              onClick = { viewModel.registerUser() },
              modifier = Modifier.fillMaxWidth().testTag(RegisterScreenTestTags.REGISTER_SAVE),
              enabled =
                  !registerUiState.isLoading &&
                      registerUiState.dateOfBirth.isNotBlank() &&
                      registerUiState.username.isNotBlank() &&
                      !anyError,
              colors =
                  ButtonDefaults.buttonColors(
                      containerColor = Primary, disabledContentColor = disabledLabelColor)) {
                if (registerUiState.isLoading) {
                  Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(
                            modifier =
                                Modifier.size(18.dp)
                                    .testTag(RegisterScreenTestTags.REGISTER_LOADING),
                            strokeWidth = 2.dp,
                            color = Primary)
                        Text(text = "Saving…", color = Primary)
                      }
                } else {
                  Text("Save")
                }
              }
        }
  }
}

/**
 * This datePicker has been taken from the official Android Developers site
 * https://developer.android.com/develop/ui/compose/components/datepickers
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModalInput(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
    disabledLabelColor: Color
) {
  val datePickerState = rememberDatePickerState(initialDisplayMode = DisplayMode.Input)

  DatePickerDialog(
      onDismissRequest = onDismiss,
      confirmButton = {
        TextButton(
            onClick = {
              onDateSelected(datePickerState.selectedDateMillis)
              onDismiss()
            },
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Primary, disabledContentColor = disabledLabelColor)) {
              Text("Confirm")
            }
      },
      dismissButton = { TextButton(onClick = onDismiss) { Text("Dismiss") } }) {
        DatePicker(
            state = datePickerState,
            modifier = Modifier.testTag(RegisterScreenTestTags.REGISTER_DATE_PICKER))
      }
}

private fun formatSelectedDate(millis: Long): String {
  val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
  val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withLocale(Locale.getDefault())
  return localDate.format(formatter)
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
  OOTDTheme { RegisterScreen() }
}
