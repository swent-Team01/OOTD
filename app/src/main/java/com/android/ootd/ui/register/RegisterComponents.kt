package com.android.ootd.ui.register

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.ootd.R
import com.android.ootd.ui.map.LocationSelectionSection
import com.android.ootd.ui.map.LocationSelectionViewModel
import com.android.ootd.ui.theme.Bodoni
import com.android.ootd.ui.theme.Primary
import com.android.ootd.ui.theme.Secondary
import com.android.ootd.ui.theme.Tertiary
import com.android.ootd.utils.composables.CommonTextField

// Error messages (kept as constants to avoid repetition)
internal const val ERROR_USERNAME = "Please enter a valid username"
internal const val ERROR_DATE = "Please enter a valid date"
internal const val ERROR_LOCATION = "Please select a valid location"

// Field state container to unify focus/touch/left management and color
internal data class FieldState(
    val touched: MutableState<Boolean>,
    val left: MutableState<Boolean>,
    val focused: MutableState<Boolean>,
    val textColor: MutableState<Color>
)

@Composable
internal fun rememberFieldState(): FieldState = remember {
  FieldState(
      touched = mutableStateOf(false),
      left = mutableStateOf(false),
      focused = mutableStateOf(false),
      textColor = mutableStateOf(Tertiary))
}

@Composable
internal fun HandleRegistrationEffects(
    errorMsg: String?,
    registered: Boolean,
    isLoading: Boolean,
    onClearError: () -> Unit,
    onRegisteredHandled: () -> Unit,
    onRegister: () -> Unit,
    onHideDatePicker: () -> Unit
) {
  val context = LocalContext.current

  LaunchedEffect(errorMsg) {
    errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      onClearError()
    }
  }

  LaunchedEffect(registered) {
    if (registered) {
      onRegisteredHandled()
      onRegister()
    }
  }

  LaunchedEffect(isLoading) { if (isLoading) onHideDatePicker() }
}

@Composable
internal fun UpdateFieldColors(fieldState: FieldState, hasError: Boolean) {
  LaunchedEffect(fieldState.focused.value, hasError, fieldState.touched.value) {
    fieldState.textColor.value =
        when {
          hasError && !fieldState.focused.value -> Color.Red
          fieldState.touched.value -> Primary
          else -> Tertiary
        }
  }
}

@Composable
internal fun RegisterHeader() {
  Image(
      painter = painterResource(id = R.drawable.app_logo),
      contentDescription = "Logo",
      contentScale = ContentScale.FillBounds,
      modifier = Modifier.width(237.dp).height(237.dp).testTag(RegisterScreenTestTags.APP_LOGO))

  Text(
      text = "Welcome\nTime to drop a fit.",
      fontFamily = Bodoni,
      color = Primary,
      fontSize = 20.sp,
      textAlign = TextAlign.Center,
      modifier =
          Modifier.fillMaxWidth()
              .padding(bottom = 16.dp)
              .testTag(RegisterScreenTestTags.WELCOME_TITLE))
}

@Composable
internal fun UsernameField(
    value: String,
    onValueChange: (String) -> Unit,
    fieldState: FieldState,
    isError: Boolean,
    isLoading: Boolean
) {

  CommonTextField(
      value = value,
      onChange = onValueChange,
      label = "Username",
      placeholder = "Enter your username",
      singleLine = true,
      modifier =
          Modifier.fillMaxWidth()
              .testTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME)
              .onFocusChanged { focusState ->
                fieldState.focused.value = focusState.isFocused
                if (focusState.isFocused) {
                  fieldState.touched.value = true
                } else if (fieldState.touched.value) {
                  fieldState.left.value = true
                }
              },
      isError = isError && !fieldState.focused.value,
      enabled = !isLoading)

  if (isError && !fieldState.focused.value) {
    ErrorText(ERROR_USERNAME)
  }
}

@Composable
internal fun DateOfBirthField(
    value: String,
    fieldState: FieldState,
    isError: Boolean,
    isLoading: Boolean,
    onShowDatePicker: () -> Unit
) {

  CommonTextField(
      value = value,
      onChange = {},
      label = "Date of Birth",
      placeholder = "DD/MM/YYYY",
      readOnly = true,
      singleLine = true,
      modifier =
          Modifier.fillMaxWidth()
              .testTag(RegisterScreenTestTags.INPUT_REGISTER_DATE)
              .onFocusChanged { focusState ->
                fieldState.focused.value = focusState.isFocused
                if (focusState.isFocused) {
                  onShowDatePicker()
                } else if (fieldState.touched.value) {
                  fieldState.left.value = true
                }
              },
      isError = isError && !fieldState.focused.value,
      enabled = !isLoading,
      trailingIcon = {
        IconButton(
            onClick = { if (!isLoading) onShowDatePicker() },
        ) {
          Icon(
              Icons.Default.DateRange,
              contentDescription = "Select date",
              modifier = Modifier.testTag(RegisterScreenTestTags.DATE_PICKER_ICON))
        }
      })

  if (isError && !fieldState.focused.value) {
    ErrorText(ERROR_DATE)
  }
}

@Composable
internal fun LocationField(
    locationViewModel: LocationSelectionViewModel,
    fieldState: FieldState,
    isError: Boolean,
    onGPSClick: () -> Unit = {}
) {
  LocationSelectionSection(
      viewModel = locationViewModel,
      textGPSButton = "Use current location (GPS)",
      textLocationField = "Search Location",
      onGPSClick = onGPSClick,
      isError = isError && !fieldState.focused.value,
      onFocusChanged = { isFocused ->
        fieldState.focused.value = isFocused
        if (isFocused) {
          fieldState.touched.value = true
        } else if (fieldState.touched.value) {
          fieldState.left.value = true
        }
      },
      modifier =
          Modifier.padding(vertical = 8.dp).testTag(RegisterScreenTestTags.INPUT_REGISTER_LOCATION))

  if (isError && !fieldState.focused.value) {
    ErrorText(ERROR_LOCATION)
  }
}

@Composable
internal fun RegisterFooter(isLoading: Boolean, isEnabled: Boolean, onRegisterClick: () -> Unit) {
  Text(
      text = "Outfit Of The Day,\n Inspire Drip",
      fontFamily = Bodoni,
      fontSize = 18.sp,
      color = Primary,
      fontWeight = FontWeight(400),
      textAlign = TextAlign.Center,
      modifier = Modifier.testTag(RegisterScreenTestTags.REGISTER_APP_SLOGAN))

  Spacer(modifier = Modifier.height(16.dp))

  Button(
      onClick = onRegisterClick,
      modifier = Modifier.fillMaxWidth().testTag(RegisterScreenTestTags.REGISTER_SAVE),
      enabled = isEnabled,
      colors =
          ButtonDefaults.buttonColors(
              containerColor = Primary,
              disabledContentColor = if (isLoading) Secondary else Tertiary)) {
        if (isLoading) {
          Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(
                    modifier =
                        Modifier.size(18.dp).testTag(RegisterScreenTestTags.REGISTER_LOADING),
                    strokeWidth = 2.dp,
                    color = Primary)
                Text(text = "Saving…", color = Primary)
              }
        } else {
          Text("Save")
        }
      }
}

@Composable
internal fun ErrorText(message: String) {
  Text(
      text = message,
      color = Color.Red,
      fontFamily = Bodoni,
      modifier = Modifier.padding(8.dp).testTag(RegisterScreenTestTags.ERROR_MESSAGE))
}
