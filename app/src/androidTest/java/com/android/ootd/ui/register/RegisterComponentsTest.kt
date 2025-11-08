package com.android.ootd.ui.register

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.ootd.model.map.Location
import org.junit.Rule
import org.junit.Test

/**
 * Focused tests for the small UI components extracted in RegisterComponents.kt. These complement
 * RegisterViewModelTest by covering pure UI/Compose behaviors: focus transitions, error rendering,
 * callbacks, and dynamic enabling logic. No business logic is re-tested here.
 */
class RegisterComponentsTest {
  @get:Rule val composeTestRule = createComposeRule()

  private fun baseState(
      username: String = "",
      date: String = "",
      locationQuery: String = "",
      selectedLocation: Location? = null
  ) =
      RegisterUserViewModel(
          username = username,
          dateOfBirth = date,
          locationQuery = locationQuery,
          selectedLocation = selectedLocation)

  // --- Field color & error behavior ---
  @Test
  fun usernameField_empty_afterFocusLoss_showsError_then_validHidesError() {
    composeTestRule.setContent {
      val field = rememberFieldState()
      var value by remember { mutableStateOf("") }
      var dummy by remember { mutableStateOf("") }
      Column {
        UsernameField(
            value = value,
            onValueChange = { value = it },
            fieldState = field,
            isError = value.isBlank() && field.left.value,
            isLoading = false)
        // Second focusable to force focus loss
        OutlinedTextField(
            value = dummy,
            onValueChange = { dummy = it },
            label = { androidx.compose.material3.Text("Dummy") },
            modifier = Modifier.testTag("dummyFocus"))
      }
    }
    // Focus username field
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).performClick()
    // Lose focus by clicking dummy field
    composeTestRule.onNodeWithTag("dummyFocus").performClick()
    composeTestRule.waitForIdle()
    // Now error should be visible
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.ERROR_MESSAGE)
        .assertIsDisplayed()
        .assertTextContains("valid username", substring = true)
    // Enter valid text, error should disappear
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).performClick()
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME)
        .performTextInput("validUser")
    // Shift focus again to dummy to mark left
    composeTestRule.onNodeWithTag("dummyFocus").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.ERROR_MESSAGE).assertDoesNotExist()
  }

  @Test
  fun dateOfBirthField_iconClick_invokesCallback() {
    var invoked = false
    composeTestRule.setContent {
      val field = rememberFieldState()
      DateOfBirthField(
          value = "",
          fieldState = field,
          isError = false,
          isLoading = false,
          onShowDatePicker = { invoked = true })
    }
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.DATE_PICKER_ICON, useUnmergedTree = true)
        .assertExists()
        .performClick()
    composeTestRule.runOnIdle { assert(invoked) }
  }

  @Test
  fun locationField_errorShown_whenLeftEmpty() {
    composeTestRule.setContent {
      val f = rememberFieldState()
      LocationField(
          registerUiState = baseState(locationQuery = ""),
          viewModel = RegisterViewModel(), // real VM (default repos) just for signatures
          fieldState = f,
          isError = true)
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.ERROR_MESSAGE).assertIsDisplayed()
  }

  @Test
  fun footer_loadingIndicatorDisplayed_whenLoading() {
    composeTestRule.setContent {
      RegisterFooter(isLoading = true, isEnabled = false, onRegisterClick = {})
    }
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_LOADING).assertIsDisplayed()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE).assertIsNotEnabled()
  }

  @Test
  fun footer_enabled_whenAllValid() {
    composeTestRule.setContent {
      RegisterFooter(isLoading = false, isEnabled = true, onRegisterClick = {})
    }
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE).assertIsEnabled()
  }

  @Test
  fun handleRegistrationEffects_callsCallbacks() {
    var cleared = 0
    var registeredHandled = 0
    var hidePicker = 0
    var registerCallback = 0
    composeTestRule.setContent {
      HandleRegistrationEffects(
          errorMsg = "some error",
          registered = true,
          isLoading = true,
          onClearError = { cleared++ },
          onRegisteredHandled = { registeredHandled++ },
          onRegister = { registerCallback++ },
          onHideDatePicker = { hidePicker++ })
    }
    composeTestRule.waitForIdle()
    composeTestRule.runOnIdle {
      assert(cleared == 1)
      assert(registeredHandled == 1)
      assert(registerCallback == 1)
      assert(hidePicker == 1)
    }
  }

  @Test
  fun errorText_displaysMessage() {
    composeTestRule.setContent { ErrorText("Test error message") }
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.ERROR_MESSAGE)
        .assertIsDisplayed()
        .assertTextContains("Test error", substring = true)
  }
}
