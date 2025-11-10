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
import com.android.ootd.LocationProvider
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.LocationRepository
import com.android.ootd.model.user.UserRepository
import com.android.ootd.ui.map.LocationSelectionTestTags
import com.android.ootd.ui.register.RegisterScreen
import com.android.ootd.ui.register.RegisterScreenTestTags
import com.android.ootd.ui.register.RegisterViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for the RegisterScreen.
 *
 * Note: The main registration logic (checking if user exists in backend) is tested in
 * AuthenticationTest. These tests focus on UI component rendering, validation, and user
 * interactions with the registration form.
 *
 * Disclaimer: Parts of the code were done with AI
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
    LocationProvider.fusedLocationClient = mockk<FusedLocationProviderClient>(relaxed = true)
    viewModel =
        RegisterViewModel(userRepository = repository, locationRepository = locationRepository)
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

  // ========== Location Selection Tests ==========

  @Test
  fun locationInput_showsMultipleSuggestions_and_closesAfterSelection() {
    // Arrange: mock repository to return multiple suggestions
    val suggestions =
        listOf(
            Location(47.3769, 8.5417, "Zürich, Switzerland"),
            Location(46.2044, 6.1432, "Lausanne, Switzerland"),
            Location(46.9481, 7.4474, "Bern, Switzerland"))

    coEvery { locationRepository.search(any()) } returns suggestions

    // Act: type to trigger dropdown
    composeTestRule.enterLocation("Switz")
    composeTestRule.waitForIdle()

    // Assert: should show exactly 3 suggestions
    composeTestRule
        .onAllNodesWithTag(LocationSelectionTestTags.LOCATION_SUGGESTION)
        .assertCountEquals(3)

    // Now select one suggestion to verify selection behavior and that the dropdown closes
    // Arrange: mock repository to return a single suggestion for selection interaction
    val testLocation = Location(47.3769, 8.5417, "Zürich, Switzerland")
    coEvery { locationRepository.search(any()) } returns listOf(testLocation)

    // Act: type and click the first suggestion
    composeTestRule.enterLocation("Zur")
    composeTestRule.waitForIdle()
    composeTestRule
        .onAllNodesWithTag(LocationSelectionTestTags.LOCATION_SUGGESTION)[0]
        .performClick()
    composeTestRule.waitForIdle()

    // Assert: location should be selected and query updated
    composeTestRule.runOnUiThread {
      assertEquals(testLocation, viewModel.uiState.value.selectedLocation)
      assertEquals(testLocation.name, viewModel.uiState.value.locationQuery)
    }

    // Assert: dropdown should be closed (suggestions cleared)
    composeTestRule
        .onAllNodesWithTag(LocationSelectionTestTags.LOCATION_SUGGESTION)
        .assertCountEquals(0)
  }

  @Test
  fun registerButton_disabled_whenLocationError() {
    composeTestRule.enterUsername("validUser")
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.DATE_PICKER_ICON, useUnmergedTree = true)
        .performClick()
    composeTestRule.enterDate("10102020")
    composeTestRule.waitForIdle()

    // Touch and leave location field without selecting
    composeTestRule.onNodeWithTag(LocationSelectionTestTags.INPUT_LOCATION).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).performClick()
    composeTestRule.waitForIdle()

    // Button should be disabled due to location error
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE).assertIsNotEnabled()
  }

  @Test
  fun errorMessage_triggersToast() {
    // Set an error message directly in the viewModel
    composeTestRule.runOnUiThread { viewModel.emitError("Test error message") }
    composeTestRule.waitForIdle()

    // The toast is displayed (we can't easily verify Toast content in tests,
    // but we can verify the error was cleared after being shown)
    composeTestRule.runOnUiThread { assertNull(viewModel.uiState.value.errorMsg) }
  }

  // ========== Dropdown Behavior Tests ==========

  @Test
  fun locationDropdown_showsAutomatically_whenSuggestionsArriveWhileFocused() {
    // Arrange: focus the location field first
    composeTestRule.onNodeWithTag(LocationSelectionTestTags.INPUT_LOCATION).performClick()
    composeTestRule.waitForIdle()

    // Mock repository to return suggestions
    val suggestions = listOf(Location(47.3769, 8.5417, "Zürich, Switzerland"))
    coEvery { locationRepository.search(any()) } returns suggestions

    // Act: type to trigger search
    composeTestRule.enterLocation("Zur")
    composeTestRule.waitForIdle()

    // Assert: dropdown should show suggestions automatically
    composeTestRule
        .onAllNodesWithTag(LocationSelectionTestTags.LOCATION_SUGGESTION)
        .assertCountEquals(1)
  }

  @Test
  fun locationDropdown_hidesAutomatically_whenSuggestionsCleared() {
    // Arrange: show suggestions first
    val suggestions = listOf(Location(47.3769, 8.5417, "Zürich, Switzerland"))
    coEvery { locationRepository.search(any()) } returns suggestions
    composeTestRule.enterLocation("Zur")
    composeTestRule.waitForIdle()

    // Act: clear suggestions via viewModel
    composeTestRule.runOnUiThread { viewModel.clearLocationSuggestions() }
    composeTestRule.waitForIdle()

    // Assert: dropdown should be hidden
    composeTestRule
        .onAllNodesWithTag(LocationSelectionTestTags.LOCATION_SUGGESTION)
        .assertCountEquals(0)
  }

  @Test
  fun locationDropdown_hidesWhenLosingFocus() {
    // Arrange: show suggestions
    val suggestions = listOf(Location(47.3769, 8.5417, "Zürich, Switzerland"))
    coEvery { locationRepository.search(any()) } returns suggestions
    composeTestRule.enterLocation("Zur")
    composeTestRule.waitForIdle()

    // Act: click another field to lose focus
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).performClick()
    composeTestRule.waitForIdle()

    // Assert: dropdown should be hidden
    composeTestRule
        .onAllNodesWithTag(LocationSelectionTestTags.LOCATION_SUGGESTION)
        .assertCountEquals(0)
  }

  @Test
  fun locationDropdown_showsAgain_whenRefocusingWithExistingSuggestions() {
    // Arrange: set suggestions and then blur the field
    val suggestions = listOf(Location(47.3769, 8.5417, "Zürich, Switzerland"))
    composeTestRule.runOnUiThread { viewModel.setLocationSuggestions(suggestions) }
    composeTestRule.waitForIdle()

    // Focus another field first
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).performClick()
    composeTestRule.waitForIdle()

    // Act: refocus location field
    composeTestRule.onNodeWithTag(LocationSelectionTestTags.INPUT_LOCATION).performClick()
    composeTestRule.waitForIdle()

    // Assert: dropdown should show again
    composeTestRule
        .onAllNodesWithTag(LocationSelectionTestTags.LOCATION_SUGGESTION)
        .assertCountEquals(1)
  }

  @Test
  fun locationField_isReadOnly_whenValidLocationSelected() {
    // Arrange: select a valid location
    val validLocation = Location(47.3769, 8.5417, "Zürich, Switzerland")
    composeTestRule.runOnUiThread { viewModel.setLocation(validLocation) }
    composeTestRule.waitForIdle()

    // Assert: location field should display the location name
    composeTestRule
        .onNodeWithTag(LocationSelectionTestTags.INPUT_LOCATION)
        .assertTextContains("Zürich, Switzerland")

    // Assert: field should be read-only (text input won't change the value)
    // When a location is selected, the field becomes read-only
    composeTestRule.runOnUiThread {
      assertEquals(validLocation, viewModel.uiState.value.selectedLocation)
      assertEquals("Zürich, Switzerland", viewModel.uiState.value.locationQuery)
    }
  }

  @Test
  fun locationField_becomesEditable_whenLocationIsCleared() {
    // Arrange: first set a location
    val location = Location(47.3769, 8.5417, "Zürich, Switzerland")
    composeTestRule.runOnUiThread { viewModel.setLocation(location) }
    composeTestRule.waitForIdle()

    // Assert: field is read-only with location
    composeTestRule.runOnUiThread {
      assertEquals(location, viewModel.uiState.value.selectedLocation)
    }

    // Act: clear the location by setting empty query
    composeTestRule.runOnUiThread { viewModel.setLocationQuery("") }
    composeTestRule.waitForIdle()

    // Assert: field becomes editable again (no selected location)
    composeTestRule.runOnUiThread {
      assertNull(viewModel.uiState.value.selectedLocation)
      assertEquals("", viewModel.uiState.value.locationQuery)
    }

    // Assert: field should now accept input
    composeTestRule.enterLocation("Lausanne")
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      assertEquals("Lausanne", viewModel.uiState.value.locationQuery)
    }
  }

  @Test
  fun clearButton_appearsOnlyWhenLocationSelected_andResetsLocation() {
    // Initially no clear button
    composeTestRule
        .onNodeWithTag(LocationSelectionTestTags.LOCATION_CLEAR_BUTTON)
        .assertDoesNotExist()

    // Select location - clear button appears
    val location = Location(47.3769, 8.5417, "Zürich, Switzerland")
    composeTestRule.runOnUiThread { viewModel.setLocation(location) }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(LocationSelectionTestTags.LOCATION_CLEAR_BUTTON)
        .assertIsDisplayed()

    // Click clear - location reset, button disappears
    composeTestRule.onNodeWithTag(LocationSelectionTestTags.LOCATION_CLEAR_BUTTON).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      assertNull(viewModel.uiState.value.selectedLocation)
      assertEquals("", viewModel.uiState.value.locationQuery)
    }

    composeTestRule
        .onNodeWithTag(LocationSelectionTestTags.LOCATION_CLEAR_BUTTON)
        .assertDoesNotExist()
  }

  @Test
  fun gpsButton_triggersPermissionFlow_onGrant() {
    // When GPS button is clicked, it should trigger permission request
    // In a real scenario this would launch the permission dialog
    // Here we verify the button exists and can trigger the callback
    composeTestRule.onNodeWithTag(LocationSelectionTestTags.LOCATION_GPS_BUTTON).assertIsDisplayed()

    // Simulate permission granted via ViewModel directly (UI test can't test system dialogs)
    composeTestRule.runOnUiThread { viewModel.onLocationPermissionGranted() }
    composeTestRule.waitForIdle()

    // Verify loading state was set (GPS retrieval started)
    assertTrue(viewModel.uiState.value.isLoadingLocations)
  }

  @Test
  fun gpsButton_triggersPermissionFlow_onDeny() {
    // Verify GPS button exists
    composeTestRule.onNodeWithTag(LocationSelectionTestTags.LOCATION_GPS_BUTTON).assertIsDisplayed()

    val freshViewModel =
        RegisterViewModel(userRepository = repository, locationRepository = locationRepository)

    // Simulate permission denied on the fresh VM
    freshViewModel.onLocationPermissionDenied()

    // Verify error message set on the ViewModel
    assertNotNull(freshViewModel.uiState.value.errorMsg)
    assertTrue(freshViewModel.uiState.value.errorMsg!!.contains("Location permission denied"))
  }
}
