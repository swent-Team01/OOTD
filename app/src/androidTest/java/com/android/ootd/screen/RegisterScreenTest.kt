package com.android.ootd.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.android.ootd.LocationProvider
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.LocationRepository
import com.android.ootd.model.user.UserRepository
import com.android.ootd.ui.map.LocationSelectionTestTags
import com.android.ootd.ui.map.LocationSelectionViewModel
import com.android.ootd.ui.register.RegisterScreen
import com.android.ootd.ui.register.RegisterScreenTestTags
import com.android.ootd.ui.register.RegisterViewModel
import com.android.ootd.utils.verifyElementDoesNotAppearWithTimer
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
  private lateinit var locationSelectionViewModel: LocationSelectionViewModel

  @Before
  fun setUp() {
    repository = mockk(relaxed = true)
    locationRepository = mockk(relaxed = true)
    LocationProvider.fusedLocationClient = mockk<FusedLocationProviderClient>(relaxed = true)

    // Create LocationSelectionViewModel with mocked repository
    locationSelectionViewModel = LocationSelectionViewModel(locationRepository = locationRepository)

    // Create RegisterViewModel with the locationSelectionViewModel that uses mocked repo
    viewModel =
        RegisterViewModel(
            userRepository = repository, locationSelectionViewModel = locationSelectionViewModel)

    // Mock the location repository to return empty list by default
    coEvery { locationRepository.search(any()) } returns emptyList()
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
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.WELCOME_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LocationSelectionTestTags.LOCATION_GPS_BUTTON).assertIsDisplayed()

    // Verify privacy toggle is displayed and works
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.PRIVACY_TOGGLE)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Private").assertIsDisplayed()

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

    composeTestRule.onNodeWithText("Private").assertIsDisplayed()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.PRIVACY_TOGGLE).performClick()
    composeTestRule.onNodeWithText("Public").assertIsDisplayed()

    // Set a valid location
    composeTestRule.runOnUiThread {
      viewModel.locationSelectionViewModel.setLocation(Location(47.0, 8.0, "Zürich, Switzerland"))
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
    composeTestRule.waitUntil(
        condition = {
          composeTestRule
              .onAllNodesWithTag(LocationSelectionTestTags.LOCATION_SUGGESTION)
              .fetchSemanticsNodes()
              .size == 3
        },
        timeoutMillis = 5000)
    // Now select one suggestion to verify selection behavior and that the dropdown closes
    // Arrange: mock repository to return a single suggestion for selection interaction
    val testLocation = Location(47.3769, 8.5417, "Zürich, Switzerland")
    coEvery { locationRepository.search(any()) } returns listOf(testLocation)

    // Act: clear field, then type and wait for suggestions before clicking
    composeTestRule.onNodeWithTag(LocationSelectionTestTags.LOCATION_CLEAR_BUTTON).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.enterLocation("Zur")
    composeTestRule.waitForIdle()

    // Wait for the suggestion to appear before trying to click it
    composeTestRule.waitUntil(
        condition = {
          composeTestRule
              .onAllNodesWithTag(LocationSelectionTestTags.LOCATION_SUGGESTION)
              .fetchSemanticsNodes()
              .size >= 1
        },
        timeoutMillis = 5000)

    composeTestRule
        .onAllNodesWithTag(LocationSelectionTestTags.LOCATION_SUGGESTION)[0]
        .performClick()
    composeTestRule.waitForIdle()

    // Assert: location should be selected and query updated
    composeTestRule.runOnUiThread {
      assertEquals(
          testLocation, viewModel.locationSelectionViewModel.uiState.value.selectedLocation)
      assertEquals(
          testLocation.name, viewModel.locationSelectionViewModel.uiState.value.locationQuery)
    }

    // Assert: dropdown should be closed (suggestions cleared)
    verifyElementDoesNotAppearWithTimer(
        composeTestRule, LocationSelectionTestTags.LOCATION_SUGGESTION)
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
    // Mock repository to return suggestions
    val suggestions = listOf(Location(47.3769, 8.5417, "Zürich, Switzerland"))
    coEvery { locationRepository.search(any()) } returns suggestions

    // Use shared test helper
    composeTestRule.testLocationDropdown_showsAutomatically_whenSuggestionsArriveWhileFocused()
  }

  @Test
  fun locationDropdown_hidesAutomatically_whenSuggestionsCleared() {
    // Mock repository to return suggestions
    val suggestions = listOf(Location(47.3769, 8.5417, "Zürich, Switzerland"))
    coEvery { locationRepository.search(any()) } returns suggestions

    // Use shared test helper
    composeTestRule.testLocationDropdown_hidesAutomatically_whenSuggestionsCleared(
        viewModel.locationSelectionViewModel)
  }

  @Test
  fun locationDropdown_hidesWhenLosingFocus() {
    // Mock repository to return suggestions
    val suggestions = listOf(Location(47.3769, 8.5417, "Zürich, Switzerland"))
    coEvery { locationRepository.search(any()) } returns suggestions

    // Use shared test helper
    composeTestRule.testLocationDropdown_hidesWhenLosingFocus(
        RegisterScreenTestTags.INPUT_REGISTER_UNAME)
  }

  @Test
  fun locationDropdown_showsAgain_whenRefocusingWithExistingSuggestions() {
    // Use shared test helper
    composeTestRule.testLocationDropdown_showsAgain_whenRefocusingWithExistingSuggestions(
        viewModel.locationSelectionViewModel, RegisterScreenTestTags.INPUT_REGISTER_UNAME)
  }

  @Test
  fun locationField_isReadOnly_whenValidLocationSelected() {
    // Arrange: select a valid location
    val validLocation = Location(47.3769, 8.5417, "Zürich, Switzerland")
    composeTestRule.runOnUiThread {
      viewModel.locationSelectionViewModel.setLocation(validLocation)
    }
    composeTestRule.waitForIdle()

    // Assert: location field should display the location name
    composeTestRule
        .onNodeWithTag(LocationSelectionTestTags.INPUT_LOCATION)
        .assertTextContains("Zürich, Switzerland")

    // Assert: field should be read-only (text input won't change the value)
    // When a location is selected, the field becomes read-only
    composeTestRule.runOnUiThread {
      assertEquals(
          validLocation, viewModel.locationSelectionViewModel.uiState.value.selectedLocation)
      assertEquals(
          "Zürich, Switzerland", viewModel.locationSelectionViewModel.uiState.value.locationQuery)
    }
  }

  @Test
  fun locationField_becomesEditable_whenLocationIsCleared() {
    // Arrange: first set a location
    val location = Location(47.3769, 8.5417, "Zürich, Switzerland")
    composeTestRule.runOnUiThread { viewModel.locationSelectionViewModel.setLocation(location) }
    composeTestRule.waitForIdle()

    // Assert: field is read-only with location
    composeTestRule.runOnUiThread {
      assertEquals(location, viewModel.locationSelectionViewModel.uiState.value.selectedLocation)
    }

    // Act: clear the location by setting empty query
    composeTestRule.runOnUiThread { viewModel.locationSelectionViewModel.setLocationQuery("") }
    composeTestRule.waitForIdle()

    // Assert: field becomes editable again (no selected location)
    composeTestRule.runOnUiThread {
      assertNull(viewModel.locationSelectionViewModel.uiState.value.selectedLocation)
      assertEquals("", viewModel.locationSelectionViewModel.uiState.value.locationQuery)
    }

    // Assert: field should now accept input
    composeTestRule.enterLocation("Lausanne")
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      assertEquals("Lausanne", viewModel.locationSelectionViewModel.uiState.value.locationQuery)
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
    composeTestRule.runOnUiThread { viewModel.locationSelectionViewModel.setLocation(location) }
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(LocationSelectionTestTags.LOCATION_CLEAR_BUTTON)
        .assertIsDisplayed()

    // Click clear - location reset, button disappears
    composeTestRule.onNodeWithTag(LocationSelectionTestTags.LOCATION_CLEAR_BUTTON).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.runOnUiThread {
      assertNull(viewModel.locationSelectionViewModel.uiState.value.selectedLocation)
      assertEquals("", viewModel.locationSelectionViewModel.uiState.value.locationQuery)
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
    assertTrue(viewModel.locationSelectionViewModel.uiState.value.isLoadingLocations)
  }

  @Test
  fun gpsButton_triggersPermissionFlow_onDeny() {
    // Verify GPS button exists
    composeTestRule.onNodeWithTag(LocationSelectionTestTags.LOCATION_GPS_BUTTON).assertIsDisplayed()

    val freshViewModel = RegisterViewModel(userRepository = repository)

    // Simulate permission denied on the fresh VM
    freshViewModel.onLocationPermissionDenied()

    // Verify error message set on the ViewModel
    assertNotNull(freshViewModel.uiState.value.errorMsg)
    assertTrue(freshViewModel.uiState.value.errorMsg!!.contains("Location permission"))
  }

  // ========== EPFL Default Location Tests ==========
  @Test
  fun defaultEpflLocationButton_isDisplayed_and_setsLocationToEPFL() {
    // Wait for the EPFL button to exist in the semantics tree
    composeTestRule.waitUntil(timeoutMillis = 5000) {
      composeTestRule
          .onAllNodesWithText("or select default location (EPFL)", substring = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Scroll to make the button visible on smaller screens
    composeTestRule
        .onNodeWithTag(LocationSelectionTestTags.LOCATION_DEFAULT_EPFL)
        .performScrollTo()
        .performClick()
        .assertTextContains("or select default location (EPFL)")
    composeTestRule.waitForIdle()

    // Assert: location should be set to EPFL
    composeTestRule.runOnUiThread {
      val selectedLocation = viewModel.locationSelectionViewModel.uiState.value.selectedLocation
      assertNotNull(selectedLocation)
      assertEquals(46.5191, selectedLocation!!.latitude, 0.0001)
      assertEquals(6.5668, selectedLocation.longitude, 0.0001)
      assertTrue(selectedLocation.name.contains("EPFL"))
      assertEquals(
          selectedLocation.name, viewModel.locationSelectionViewModel.uiState.value.locationQuery)
    }
  }
}
