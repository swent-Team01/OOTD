package com.android.ootd.ui.register

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.android.ootd.ui.account.AvatarSection
import com.android.ootd.ui.account.ProfilePictureEditor
import com.android.ootd.ui.map.LocationSelectionViewState
import com.android.ootd.ui.theme.Bodoni
import com.android.ootd.ui.theme.OOTDTheme
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.Tertiary
import com.android.ootd.ui.theme.Typography
import com.android.ootd.utils.LocationUtils
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

private val SPACER = 16.dp

/**
 * Register screen composable that allows users to create a new account.
 *
 * This screen displays username, date of birth, and location input fields with validation. It
 * validates fields only after the user has interacted with them and left focus. The screen shows
 * loading state during registration and navigates to the next screen upon success.
 *
 * @param viewModel The ViewModel that manages the registration state and logic. Defaults to a new
 *   instance.
 * @param onRegister Callback invoked when registration is successful. Defaults to an empty lambda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(viewModel: RegisterViewModel = viewModel(), onRegister: () -> Unit = {}) {
  val registerUiState by viewModel.uiState.collectAsState()
  val locationUiState by viewModel.locationSelectionViewModel.uiState.collectAsState()
  val usernameField = rememberFieldState()
  val dateField = rememberFieldState()
  val locationField = rememberFieldState()
  val context = LocalContext.current
  var showDatePicker by remember { mutableStateOf(false) }
  var showImageSourceDialog by remember { mutableStateOf(false) }

  // Reset the form when the screen is first shown
  DisposableEffect(Unit) {
    viewModel.refresh()
    onDispose {}
  }

  val locationPermissionLauncher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestPermission(),
          onResult = { isGranted ->
            if (isGranted) {
              viewModel.onLocationPermissionGranted()
            } else {
              viewModel.onLocationPermissionDenied()
            }
          })

  val usernameError = usernameField.left.value && registerUiState.username.isBlank()
  val dateError = dateField.left.value && registerUiState.dateOfBirth.isBlank()
  val locationError = locationField.left.value && locationUiState.selectedLocation == null

  val onGPSClick = rememberGPSClickHandler(viewModel, locationPermissionLauncher)

  HandleRegistrationEffects(
      errorMsg = registerUiState.errorMsg,
      registered = registerUiState.registered,
      isLoading = registerUiState.isLoading,
      onClearError = viewModel::clearErrorMsg,
      onRegisteredHandled = viewModel::markRegisteredHandled,
      onRegister = onRegister,
      onHideDatePicker = { showDatePicker = false })

  ProfilePictureEditor(
      context = context,
      editProfilePicture = { url -> viewModel.setProfilePicture(url) },
      showImageSourceDialog = showImageSourceDialog,
      onShowImageSourceDialogChange = { showImageSourceDialog = it })

  UpdateFieldColors(usernameField, usernameError)
  UpdateFieldColors(dateField, dateError)
  UpdateFieldColors(locationField, locationError)

  Scaffold { innerPadding ->
    Column(
        modifier =
            Modifier.padding(innerPadding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
          RegisterHeader()

          Spacer(modifier = Modifier.height(SPACER))

          AvatarSection(
              avatarUri = registerUiState.profilePicture,
              username = registerUiState.username,
              onEditClick = { showImageSourceDialog = true },
              deleteProfilePicture = { viewModel.clearProfilePicture() },
          )

          UsernameField(
              value = registerUiState.username,
              onValueChange = viewModel::setUsername,
              fieldState = usernameField,
              isError = usernameError,
              isLoading = registerUiState.isLoading)

          Spacer(modifier = Modifier.height(SPACER))

          DateOfBirthField(
              value = registerUiState.dateOfBirth,
              fieldState = dateField,
              isError = dateError,
              isLoading = registerUiState.isLoading,
              onShowDatePicker = {
                showDatePicker = true
                dateField.touched.value = true
              })

          if (showDatePicker) {
            DatePickerModalInput(
                onDateSelected = { millis ->
                  millis?.let { viewModel.setDateOfBirth(formatSelectedDate(it)) }
                },
                onDismiss = { showDatePicker = false },
                onLeaveDate = {
                  showDatePicker = false
                  dateField.left.value = true
                },
                disabledLabelColor = if (registerUiState.isLoading) Secondary else Tertiary)
          }

          LocationField(
              locationViewModel = viewModel.locationSelectionViewModel,
              fieldState = locationField,
              isError = locationError,
              onGPSClick = onGPSClick)

          Spacer(modifier = Modifier.height(SPACER))

          RegisterFooter(
              isLoading = registerUiState.isLoading,
              isEnabled =
                  isRegisterEnabled(
                      registerUiState, locationUiState, usernameError, dateError, locationError),
              onRegisterClick = viewModel::registerUser)
        }
  }
}

/** Handles the GPS button click, checking for location permissions and requesting if needed. */
@Composable
private fun rememberGPSClickHandler(
    viewModel: RegisterViewModel,
    locationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
): () -> Unit {
  val context = LocalContext.current
  return remember(viewModel, locationPermissionLauncher, context) {
    {
      if (LocationUtils.hasLocationPermission(context)) {
        viewModel.onLocationPermissionGranted()
      } else {
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
      }
    }
  }
}

/** Calculates whether the register button should be enabled based on form state. */
private fun isRegisterEnabled(
    registerUiState: RegisterUserViewModel,
    locationUiState: LocationSelectionViewState,
    usernameError: Boolean,
    dateError: Boolean,
    locationError: Boolean
): Boolean {
  return !registerUiState.isLoading &&
      registerUiState.dateOfBirth.isNotBlank() &&
      registerUiState.username.isNotBlank() &&
      locationUiState.selectedLocation != null &&
      !usernameError &&
      !dateError &&
      !locationError
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
    onLeaveDate: () -> Unit,
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
              Text(text = "Confirm", fontFamily = Bodoni)
            }
      },
      dismissButton = {
        TextButton(onClick = onLeaveDate) { Text(text = "Dismiss", fontFamily = Bodoni) }
      }) {
        CompositionLocalProvider(
            androidx.compose.ui.platform.LocalConfiguration provides
                androidx.compose.ui.platform.LocalConfiguration.current.apply {
                  setLocale(Locale.UK) // or Locale("en", "GB") for DD/MM/YYYY
                }) {
              DatePicker(
                  state = datePickerState,
                  title = {
                    Text(
                        text = "Enter your date of birth",
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                        color = Primary,
                        fontFamily = Bodoni)
                  },
                  headline = {
                    Text(
                        text = "Entered birth date",
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp),
                        style = Typography.bodyLarge,
                        color = Primary,
                        fontFamily = Bodoni)
                  },
                  modifier = Modifier.testTag(RegisterScreenTestTags.REGISTER_DATE_PICKER))
            }
      }
}

private fun formatSelectedDate(millis: Long): String {
  val localDate = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
  val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.UK)
  return localDate.format(formatter)
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
  OOTDTheme { RegisterScreen() }
}
