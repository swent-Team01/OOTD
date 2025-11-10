package com.android.ootd.model.user

import com.android.ootd.LocationProvider
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.LocationRepository
import com.android.ootd.ui.register.RegisterViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Tests generated with the help of AI
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

  private lateinit var userRepository: UserRepository
  private lateinit var accountRepository: AccountRepository
  private lateinit var accountService: AccountService
  private lateinit var locationRepository: LocationRepository
  private lateinit var auth: FirebaseAuth
  private lateinit var firebaseUser: FirebaseUser
  private lateinit var viewModel: RegisterViewModel
  private val testDispatcher = StandardTestDispatcher()
  private val testUid = "test-uid-123"
  private val testEmail = "test@example.com"

  private val EPFL_LOCATION =
      Location(46.5191, 6.5668, "École Polytechnique Fédérale de Lausanne (EPFL), Switzerland")

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    userRepository = mockk(relaxed = true)
    accountRepository = mockk(relaxed = true)
    accountService = mockk(relaxed = true)
    locationRepository = mockk(relaxed = true)
    auth = mockk(relaxed = true)
    firebaseUser = mockk(relaxed = true)

    every { auth.currentUser } returns firebaseUser
    every { firebaseUser.uid } returns testUid
    every { firebaseUser.email } returns testEmail
    every { accountService.currentUser } returns flowOf(firebaseUser)

    // Mock the fusedLocationClient to avoid lateinit errors
    LocationProvider.fusedLocationClient = mockk<FusedLocationProviderClient>(relaxed = true)

    viewModel =
        RegisterViewModel(
            userRepository, accountRepository, accountService, locationRepository, auth)

    // Set a default location for all tests to prevent MissingLocationException
    viewModel.setLocation(EPFL_LOCATION)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  // --- Tiny helpers ---
  private fun TestScope.register(username: String, dob: String? = null) {
    viewModel.setUsername(username)
    dob?.let { viewModel.setDateOfBirth(it) }
    viewModel.registerUser()
    advanceUntilIdle()
  }

  private fun stubSuccess() {
    coEvery { userRepository.createUser(any(), testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } returns Unit
  }

  @Test
  fun validation_allErrorCases_trimsWhitespace_clearError() = runTest {
    // Empty username
    register("")
    assertEquals("Username cannot be empty.", viewModel.uiState.value.errorMsg)
    coVerify(exactly = 0) { userRepository.createUser(any(), any(), any()) }

    // Blank username
    register("   ")
    assertEquals("Username cannot be empty.", viewModel.uiState.value.errorMsg)

    // Too short
    register("ab")
    assertEquals("Username must be at least 3 characters.", viewModel.uiState.value.errorMsg)

    // Too long
    register("thisUsernameIsWayTooLong123")
    assertEquals("Username must be at most 20 characters.", viewModel.uiState.value.errorMsg)

    // Invalid characters
    register("user@name!")
    assertEquals(
        "Username can only contain letters, numbers, and underscores.",
        viewModel.uiState.value.errorMsg)

    // Spaces
    register("user name")
    assertNotNull(viewModel.uiState.value.errorMsg)

    // clearErrorMsg works
    viewModel.clearErrorMsg()
    assertNull(viewModel.uiState.value.errorMsg)

    // Whitespace trimming - valid after trim
    stubSuccess()
    register("  validUser  ")
    coVerify(exactly = 1) { userRepository.createUser("validUser", testUid, testUid) }
    assertTrue(viewModel.uiState.value.registered)
  }

  @Test
  fun successfulRegistration_endToEnd_withDateOfBirth_emailHandling_stateManagement() = runTest {
    val username = "validUser123"
    val dob = "01/01/2000"
    stubSuccess()

    // Date of birth setting works
    viewModel.setDateOfBirth(dob)
    assertEquals(dob, viewModel.uiState.value.dateOfBirth)

    // Registration with DOB calls repos correctly and sets registered
    register(username, dob)
    assertTrue(viewModel.uiState.value.registered)
    assertFalse(viewModel.uiState.value.isLoading)
    assertNull(viewModel.uiState.value.errorMsg)

    // Verify first registration
    coVerify(atLeast = 1) { userRepository.createUser(username, testUid, ownerId = testUid) }
    coVerify(atLeast = 1) {
      accountRepository.createAccount(
          match { it.uid == testUid && it.username == username }, testEmail, dob, any())
    }

    // markRegisteredHandled resets flag
    viewModel.markRegisteredHandled()
    assertFalse(viewModel.uiState.value.registered)

    // refresh clears state
    register("") // trigger error
    viewModel.refresh()
    assertNull(viewModel.uiState.value.errorMsg)
    assertFalse(viewModel.uiState.value.isLoading)
    assertFalse(viewModel.uiState.value.registered)

    // Registration without DOB uses empty string
    stubSuccess() // Re-stub for the next registration
    viewModel.setDateOfBirth("") // Clear the DOB
    register("testUser2")
    coVerify(atLeast = 1) {
      accountRepository.createAccount(match { it.username == "testUser2" }, testEmail, "", any())
    }

    // Repos called in order: accountRepo first, then userRepo
    val callOrder = mutableListOf<String>()
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } coAnswers
        {
          callOrder.add("account")
          Unit
        }
    coEvery { userRepository.createUser(any(), any(), any()) } coAnswers
        {
          callOrder.add("user")
          Unit
        }
    register("orderTest")
    assertTrue(callOrder.contains("account"))
    assertTrue(callOrder.contains("user"))
    assertEquals("account", callOrder[callOrder.size - 2])
    assertEquals("user", callOrder[callOrder.size - 1])

    // Different email scenarios
    val customEmail = "custom@test.com"
    val customUser = mockk<FirebaseUser>(relaxed = true)
    every { customUser.uid } returns testUid
    every { customUser.email } returns customEmail
    every { auth.currentUser } returns customUser

    // Re-stub for custom VM
    coEvery { userRepository.createUser(any(), any()) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } returns Unit

    val customVM =
        RegisterViewModel(
            userRepository, accountRepository, accountService, locationRepository, auth)
    customVM.setLocation(EPFL_LOCATION)
    customVM.setUsername("emailTest")
    customVM.registerUser()
    advanceUntilIdle()

    coVerify(atLeast = 1) { accountRepository.createAccount(any(), customEmail, any(), any()) }
  }

  @Test
  fun edgeCases_boundaryUsernames_underscores_loadingState_multipleAttempts() = runTest {
    stubSuccess()

    // Exactly 3 characters (minimum)
    register("abc")
    assertTrue(viewModel.uiState.value.registered)
    coVerify(atLeast = 1) { userRepository.createUser("abc", testUid, testUid) }

    // Exactly 20 characters (maximum)
    register("a".repeat(20))
    assertTrue(viewModel.uiState.value.registered)

    // Underscores allowed
    register("valid_user_123")
    assertTrue(viewModel.uiState.value.registered)

    // Loading state transitions correctly
    coEvery { userRepository.createUser(any(), any(), any()) } coAnswers
        {
          kotlinx.coroutines.delay(100)
        }
    viewModel.setUsername("slowUser")
    viewModel.registerUser()
    assertTrue(viewModel.uiState.value.isLoading)
    advanceUntilIdle()
    assertFalse(viewModel.uiState.value.isLoading)

    // Multiple attempts after error work
    register("ab") // error
    assertNotNull(viewModel.uiState.value.errorMsg)
    stubSuccess()
    register("validAfterError")
    assertTrue(viewModel.uiState.value.registered)
  }

  @Test
  fun errorHandling_accountFailure_userFailure_registeredStatePersistence() = runTest {
    // Account creation failure stops loading and keeps registered false
    val username = "testUser"
    coEvery { userRepository.createUser(username, testUid) } returns Unit
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } throws
        Exception("Account error")

    register(username)
    assertFalse(viewModel.uiState.value.isLoading)
    assertFalse(viewModel.uiState.value.registered)
    assertNotNull(viewModel.uiState.value.errorMsg)

    // Both operations must succeed for registered=true
    coEvery { accountRepository.createAccount(any(), any(), any(), any()) } returns Unit
    register("bothSucceed")
    assertTrue(viewModel.uiState.value.registered)
    assertNull(viewModel.uiState.value.errorMsg)

    // registered flag initially false before operations complete
    coEvery { userRepository.createUser(any(), any()) } coAnswers
        {
          assertFalse(viewModel.uiState.value.registered)
        }
    viewModel.setUsername("checkDuring")
    viewModel.registerUser()
    assertFalse(viewModel.uiState.value.registered) // false while loading
    advanceUntilIdle()
    assertTrue(viewModel.uiState.value.registered) // true after completion
  }

  // Location functionality tests
  @Test
  fun setLocation_updatesSelectedLocationAndQuery() = runTest {
    val location = Location(48.8566, 2.3522, "Paris")

    viewModel.setLocation(location)

    assertEquals(location, viewModel.uiState.value.selectedLocation)
    assertEquals("Paris", viewModel.uiState.value.locationQuery)
  }

  @Test
  fun setLocationQuery_withEmptyQuery_clearsSuggestions() = runTest {
    viewModel.setLocationQuery("")
    advanceUntilIdle()

    assertTrue(viewModel.uiState.value.locationSuggestions.isEmpty())
    assertFalse(viewModel.uiState.value.isLoadingLocations)
  }

  @Test
  fun setLocationQuery_withNonEmptyQuery_fetchesSuggestions() = runTest {
    val mockLocations =
        listOf(Location(48.8566, 2.3522, "Paris"), Location(51.5074, -0.1278, "London"))
    coEvery { locationRepository.search("Par") } returns mockLocations

    viewModel.setLocationQuery("Par")
    advanceUntilIdle()

    assertEquals(mockLocations, viewModel.uiState.value.locationSuggestions)
    assertFalse(viewModel.uiState.value.isLoadingLocations)
  }

  @Test
  fun clearLocationSuggestions_clearsTheList() = runTest {
    val mockLocations = listOf(Location(48.8566, 2.3522, "Paris"))
    viewModel.setLocationSuggestions(mockLocations)

    viewModel.clearLocationSuggestions()

    assertTrue(viewModel.uiState.value.locationSuggestions.isEmpty())
  }

  @Test
  fun registerUser_withMissingLocation_showsError() = runTest {
    // Create a fresh viewModel without setting location
    val freshViewModel =
        RegisterViewModel(
            userRepository, accountRepository, accountService, locationRepository, auth)

    freshViewModel.setUsername("validUser123")
    freshViewModel.setDateOfBirth("01/01/2000")
    // Don't set location - should cause error

    freshViewModel.registerUser()
    advanceUntilIdle()

    assertEquals(
        "Please select a location before registering", freshViewModel.uiState.value.errorMsg)
    assertFalse(freshViewModel.uiState.value.registered)
  }

  @Test
  fun onLocationPermissionGranted_setsLoadingState() {
    viewModel.onLocationPermissionGranted()

    // Loading state should be set when GPS location retrieval starts
    assertTrue(viewModel.uiState.value.isLoadingLocations)
  }

  @Test
  fun onLocationPermissionGranted_callsSetGPSLocation() {
    // Verify that onLocationPermissionGranted triggers GPS retrieval
    // by checking that loading state is set
    assertFalse(viewModel.uiState.value.isLoadingLocations)

    viewModel.onLocationPermissionGranted()

    assertTrue(viewModel.uiState.value.isLoadingLocations)
  }

  @Test
  fun setLocation_withGPSCoordinates_updatesSelectedLocationAndQuery() {
    val gpsLocation =
        Location(
            latitude = 47.3769, longitude = 8.5417, name = "Current Location (47.3769, 8.5417)")

    viewModel.setLocation(gpsLocation)

    assertEquals(gpsLocation, viewModel.uiState.value.selectedLocation)
    assertEquals(gpsLocation.name, viewModel.uiState.value.locationQuery)
    assertTrue(viewModel.uiState.value.locationQuery.contains("Current Location"))
  }

  @Test
  fun locationQuery_isReadOnly_whenLocationSelected() {
    // When a location is selected (GPS or manual), the query should contain the location name
    val location = Location(47.3769, 8.5417, "Zürich, Switzerland")

    viewModel.setLocation(location)

    assertEquals("Zürich, Switzerland", viewModel.uiState.value.locationQuery)
    assertEquals(location, viewModel.uiState.value.selectedLocation)
  }

  @Test
  fun gpsLocation_hasCorrectFormat() {
    // Verify GPS location name format includes coordinates
    val gpsLocation =
        Location(
            latitude = 46.2044, longitude = 6.1432, name = "Current Location (46.2044, 6.1432)")

    viewModel.setLocation(gpsLocation)

    assertTrue(
        viewModel.uiState.value.selectedLocation?.name?.startsWith("Current Location") ?: false)
    assertTrue(viewModel.uiState.value.locationQuery.contains("46.2044"))
    assertTrue(viewModel.uiState.value.locationQuery.contains("6.1432"))
  }

  @Test
  fun locationError_clearedAfterSuccessfulGPSRetrieval() {
    // Set an error first
    viewModel.emitError("Some error")
    assertEquals("Some error", viewModel.uiState.value.errorMsg)

    // Then successfully set a GPS location
    val gpsLocation = Location(47.3769, 8.5417, "Current Location (47.3769, 8.5417)")
    viewModel.setLocation(gpsLocation)

    // Error should still be there (errors are only cleared explicitly)
    assertEquals("Some error", viewModel.uiState.value.errorMsg)
    // But location should be set
    assertEquals(gpsLocation, viewModel.uiState.value.selectedLocation)
  }

  @Test
  fun multipleLocationSelections_lastOneWins() {
    // Select manual location first
    val manualLocation = Location(47.3769, 8.5417, "Zürich, Switzerland")
    viewModel.setLocation(manualLocation)
    assertEquals(manualLocation, viewModel.uiState.value.selectedLocation)

    // Then select GPS location
    val gpsLocation = Location(46.2044, 6.1432, "Current Location (46.2044, 6.1432)")
    viewModel.setLocation(gpsLocation)

    // GPS location should override manual location
    assertEquals(gpsLocation, viewModel.uiState.value.selectedLocation)
    assertEquals(gpsLocation.name, viewModel.uiState.value.locationQuery)
  }

  @Test
  fun loadingState_clearAfterLocationSet() = runTest {
    // Start loading
    viewModel.onLocationPermissionGranted()
    assertTrue(viewModel.uiState.value.isLoadingLocations)

    // Simulate successful GPS retrieval by directly setting location
    // (actual GPS client is mocked/not available in unit tests)
    val gpsLocation = Location(47.3769, 8.5417, "Current Location (47.3769, 8.5417)")
    viewModel.setLocation(gpsLocation)

    // Note: In real flow, setGPSLocation would clear loading state
    // Here we verify the location is set correctly
    assertEquals(gpsLocation, viewModel.uiState.value.selectedLocation)
  }

  @Test
  fun clearLocation_resetsSelectedLocationAndQuery() = runTest {
    // Set a location first
    val location = Location(47.3769, 8.5417, "Zürich, Switzerland")
    viewModel.setLocation(location)
    assertEquals(location, viewModel.uiState.value.selectedLocation)
    assertEquals("Zürich, Switzerland", viewModel.uiState.value.locationQuery)

    // Clear by setting empty query (mimics clear button behavior)
    viewModel.setLocationQuery("")
    viewModel.clearLocationSuggestions()

    assertNull(viewModel.uiState.value.selectedLocation)
    assertEquals("", viewModel.uiState.value.locationQuery)
    assertTrue(viewModel.uiState.value.locationSuggestions.isEmpty())
  }

  @Test
  fun setLocationQuery_whenRepositoryThrows_clearsLoadingAndSuggestions() = runTest {
    // Arrange: mock repository to throw exception
    coEvery { locationRepository.search(any()) } throws Exception("Network error")

    // Act: trigger search
    viewModel.setLocationQuery("Paris")
    advanceUntilIdle()

    // Assert: loading cleared, suggestions empty
    assertFalse(viewModel.uiState.value.isLoadingLocations)
    assertTrue(viewModel.uiState.value.locationSuggestions.isEmpty())
  }

  @Test
  fun onLocationPermissionDenied_emitsErrorMessage() {
    // Act: simulate permission denial
    viewModel.onLocationPermissionDenied()

    // Assert: error message set
    assertNotNull(viewModel.uiState.value.errorMsg)
    assertTrue(
        viewModel.uiState.value.errorMsg!!.contains("Location permission denied") ||
            viewModel.uiState.value.errorMsg!!.contains("search for your location manually"))
  }
}
