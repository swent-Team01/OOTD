package com.android.ootd.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.LocationRepository
import com.android.ootd.model.user.UserRepository
import com.android.ootd.ui.map.LocationSelectionTestTags
import com.android.ootd.ui.register.RegisterScreen
import com.android.ootd.ui.register.RegisterScreenTestTags
import com.android.ootd.ui.register.RegisterViewModel
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the RegisterScreen.
 *
 * Note: The main registration logic (checking if user exists in backend) is tested in
 * AuthenticationTest. These tests focus on UI component rendering, validation, and user
 * interactions with the registration form.
 */
class RegisterScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var viewModel: RegisterViewModel
  private lateinit var repository: UserRepository
  private lateinit var locationRepository: LocationRepository

  @Before
  fun setUp() {
    repository = mockk(relaxed = true)
    locationRepository = mockk(relaxed = true)
    viewModel = RegisterViewModel(repository, locationRepository = locationRepository)
    composeTestRule.setContent { RegisterScreen(viewModel = viewModel) }
  }

  // ========== Component Display Tests ==========

  @Test
  fun displayAllComponents() {
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE)
        .assertTextContains("Save", substring = true, ignoreCase = true)
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_DATE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.APP_LOGO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.WELCOME_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LocationSelectionTestTags.LOCATION_GPS_BUTTON).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsNotDisplayed()
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.REGISTER_APP_SLOGAN)
        .assertTextContains("Outfit Of The Day,\n Inspire Drip")
  }

  @Test
  fun canEnterUsername() {
    val text = "user1"
    composeTestRule.enterUsername(text)

    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME)
        .assertTextContains(text)
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  // ========== Loading State Tests ==========

  @Test
  fun registerScreen_showsLoadingIndicator_whenIsLoadingIsTrue() {
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_LOADING).assertDoesNotExist()
    viewModel.showLoading(true)

    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_LOADING).assertExists()
    composeTestRule.onNodeWithText("Saving…").assertExists()
    viewModel.showLoading(false)
  }

  @Test
  fun loadingCircle_not_visible_when_saving_invalid_user() {
    composeTestRule.enterUsername("  ")

    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_LOADING).assertDoesNotExist()
  }

  @Test
  fun inputFields_disabled_whenLoading() {
    viewModel.showLoading(true)

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).assertIsNotEnabled()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_DATE).assertIsNotEnabled()
  }

  // ========== Validation Tests ==========

  @Test
  fun noError_whenFieldNotTouched() {
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun noError_whenTyping_beforeLeavingField() {
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).performClick()
    composeTestRule.enterUsername("validUser")

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun showsErrorMessage_whenUsernameBlank_afterLeavingField() {
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_DATE)
        .performClick() // Leave field

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextContains("Please enter a valid username")
  }

  @Test
  fun showsErrorMessage_whenDateBlank_afterLeavingField() {
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.DATE_PICKER_ICON, useUnmergedTree = true)
        .performClick()
    composeTestRule.enterDate(" ")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).performClick()

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertExists()
        .assertTextContains("Please enter a valid date")
  }

  @Test
  fun showsErrorMessage_whenLocationBlank_afterLeavingField() {
    composeTestRule.onNodeWithTag(LocationSelectionTestTags.INPUT_LOCATION).performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME)
        .performClick() // Leave field

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextContains("Please select a valid location")
  }

  @Test
  fun registerButton_disabled_whenUsernameError() {
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).performClick()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_DATE).performClick()

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE).assertIsNotEnabled()
  }

  @Test
  fun registerButton_disabled_whenDateError() {
    composeTestRule.enterUsername("validUser")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_DATE).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).performClick()

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE).assertIsNotEnabled()
  }

  @Test
  fun registerButton_enabled_whenAllFieldsValid() {
    composeTestRule.enterUsername("validUser")
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.DATE_PICKER_ICON, useUnmergedTree = true)
        .performClick()
    composeTestRule.waitForIdle()
    composeTestRule.enterDate("10102020")
    composeTestRule.waitForIdle()

    // Set a valid location
    composeTestRule.runOnUiThread {
      viewModel.setLocation(Location(47.0, 8.0, "Zürich, Switzerland"))
    }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE).assertIsEnabled()
  }

  // ========== Date Picker Tests ==========

  @Test
  fun datePickerIcon_opensDatePicker() {
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.DATE_PICKER_ICON, useUnmergedTree = true)
        .performClick()

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_DATE_PICKER).assertIsDisplayed()
  }

  @Test
  fun datePickerDismiss_closesDatePicker() {
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.DATE_PICKER_ICON, useUnmergedTree = true)
        .performClick()
    composeTestRule.onNodeWithText("Dismiss").performClick()

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_DATE_PICKER).assertDoesNotExist()

    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_DATE).performClick()
    composeTestRule.onNodeWithText("Dismiss").performClick()

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_DATE_PICKER).assertDoesNotExist()
  }

  @Test
  fun locationSuggestions_show_whenTyping() {
    // Fast arrangement: set suggestions directly on the ViewModel
    composeTestRule.runOnUiThread {
      viewModel.setLocationSuggestions(listOf(Location(47.0, 8.0, "Zürich, Switzerland")))
    }
    composeTestRule.waitForIdle()

    // Type into the location input to open the dropdown
    val input = composeTestRule.onNodeWithTag(LocationSelectionTestTags.INPUT_LOCATION)
    input.performClick()
    input.performTextInput("Z")
    composeTestRule.waitForIdle()

    // Expect at least one suggestion node
    composeTestRule
        .onAllNodesWithTag(LocationSelectionTestTags.LOCATION_SUGGESTION)
        .assertCountEquals(1)
  }

  // ========== Location Selection Tests ==========

  @Test
  fun locationInput_showsMultipleSuggestions_whenAvailable() {
    // Arrange: set multiple suggestions
    val suggestions =
        listOf(
            Location(47.3769, 8.5417, "Zürich, Switzerland"),
            Location(46.2044, 6.1432, "Lausanne, Switzerland"),
            Location(46.9481, 7.4474, "Bern, Switzerland"))

    composeTestRule.runOnUiThread { viewModel.setLocationSuggestions(suggestions) }

    // Act: type to trigger dropdown
    composeTestRule.enterLocation("Switz")

    // Assert: should show exactly 3 suggestions (max displayed)
    composeTestRule
        .onAllNodesWithTag(LocationSelectionTestTags.LOCATION_SUGGESTION)
        .assertCountEquals(3)
  }

  @Test
  fun locationSuggestion_selectsLocation_whenClicked() {
    // Arrange: set suggestions
    val testLocation = Location(47.3769, 8.5417, "Zürich, Switzerland")
    composeTestRule.runOnUiThread { viewModel.setLocationSuggestions(listOf(testLocation)) }

    // Act: type and click suggestion
    composeTestRule.enterLocation("Zur")
    composeTestRule
        .onAllNodesWithTag(LocationSelectionTestTags.LOCATION_SUGGESTION)[0]
        .performClick()
    composeTestRule.waitForIdle()

    // Assert: location should be selected and query updated
    composeTestRule.runOnUiThread {
      assertEquals(testLocation, viewModel.uiState.value.selectedLocation)
      assertEquals(testLocation.name, viewModel.uiState.value.locationQuery)
    }
  }

  @Test
  fun locationDropdown_closesAfterSelection() {
    // Arrange
    val testLocation = Location(47.3769, 8.5417, "Zürich, Switzerland")
    composeTestRule.runOnUiThread { viewModel.setLocationSuggestions(listOf(testLocation)) }

    // Act: type, click suggestion
    composeTestRule.enterLocation("Zur")
    composeTestRule.waitForIdle()
    composeTestRule
        .onAllNodesWithTag(LocationSelectionTestTags.LOCATION_SUGGESTION)[0]
        .performClick()
    composeTestRule.waitForIdle()

    // Assert: dropdown should be closed (suggestions cleared)
    composeTestRule
        .onAllNodesWithTag(LocationSelectionTestTags.LOCATION_SUGGESTION)
        .assertCountEquals(0)
  }

  @Test
  fun locationInput_clearsSelectedLocation_whenQueryChanged() {
    // Arrange: set a location first
    val initialLocation = Location(47.3769, 8.5417, "Zürich, Switzerland")
    composeTestRule.runOnUiThread { viewModel.setLocation(initialLocation) }
    composeTestRule.waitForIdle()

    // Act: type new text
    composeTestRule.enterLocation("Lausanne")

    // Assert: selected location should be cleared
    composeTestRule.runOnUiThread { assertNull(viewModel.uiState.value.selectedLocation) }
  }
}
