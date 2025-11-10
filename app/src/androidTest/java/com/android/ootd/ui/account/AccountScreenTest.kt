package com.android.ootd.ui.account

/*
 * DISCLAIMER: This file was created/modified with the assistance of GitHub Copilot.
 * Copilot provided suggestions which were reviewed and adapted by the developers.
 */
import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.credentials.CredentialManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.LocationProvider
import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.map.Location
import com.android.ootd.model.map.LocationRepository
import com.android.ootd.model.user.UserRepository
import com.android.ootd.ui.map.LocationSelectionTestTags
import com.android.ootd.ui.theme.OOTDTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.firebase.auth.FirebaseUser
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var mockAccountService: AccountService
  private lateinit var mockAccountRepository: AccountRepository
  private lateinit var mockUserRepository: UserRepository
  private lateinit var mockLocationRepository: LocationRepository
  private lateinit var mockCredentialManager: CredentialManager
  private lateinit var mockFirebaseUser: FirebaseUser
  private lateinit var viewModel: AccountViewModel

  private val userFlow = MutableStateFlow<FirebaseUser?>(null)

  @Before
  fun setup() {
    mockAccountService = mockk(relaxed = true)
    mockAccountRepository = mockk(relaxed = true)
    mockUserRepository = mockk(relaxed = true)
    mockLocationRepository = mockk(relaxed = true)
    mockCredentialManager = mockk(relaxed = true)
    mockFirebaseUser = mockk(relaxed = true)

    every { mockAccountService.currentUser } returns userFlow
    every { mockFirebaseUser.uid } returns "test-uid"
    every { mockFirebaseUser.email } returns "user1@google.com"
    every { mockFirebaseUser.photoUrl } returns null

    coEvery { mockAccountRepository.getAccount("test-uid") } returns
        Account(
            uid = "test-uid",
            ownerId = "test-uid",
            username = "user1",
            profilePicture = "",
            isPrivate = false)

    coEvery { mockLocationRepository.search(any()) } returns emptyList()

    // Mock the fusedLocationClient to avoid lateinit errors
    LocationProvider.fusedLocationClient = mockk<FusedLocationProviderClient>(relaxed = true)

    viewModel =
        AccountViewModel(
            mockAccountService, mockAccountRepository, mockUserRepository, mockLocationRepository)
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  // --- Helpers ---
  private fun signIn(user: FirebaseUser?) {
    userFlow.value = user
    composeTestRule.waitForIdle()
  }

  private fun waitForLoadingToComplete() {
    composeTestRule.waitUntil(timeoutMillis = 5000) { !viewModel.uiState.value.isLoading }
    composeTestRule.waitForIdle()
  }

  private fun setContent(onBack: (() -> Unit)? = null, onSignOut: (() -> Unit)? = null) {
    composeTestRule.setContent {
      OOTDTheme {
        AccountScreen(
            accountViewModel = viewModel,
            credentialManager = mockCredentialManager,
            onBack = onBack ?: {},
            onSignOut = onSignOut ?: {})
      }
    }
    composeTestRule.waitForIdle()
  }

  private fun selectTestTag(tag: String) = composeTestRule.onNodeWithTag(tag)

  // --- Tests (fewer, but comprehensive) ---

  @Test
  fun rendersBasic_withoutAvatar_andShowsUserInfo_andReadOnlyState() {
    signIn(mockFirebaseUser)
    setContent()

    // Core chrome
    selectTestTag(UiTestTags.TAG_ACCOUNT_BACK).assertIsDisplayed()
    selectTestTag(UiTestTags.TAG_ACCOUNT_TITLE).assertIsDisplayed()
    selectTestTag(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER).assertIsDisplayed()

    // No photo -> letter avatar shown, image absent
    selectTestTag(UiTestTags.TAG_ACCOUNT_AVATAR_LETTER).assertIsDisplayed()
    selectTestTag(UiTestTags.TAG_ACCOUNT_AVATAR_IMAGE).assertDoesNotExist()

    // Actions and fields
    selectTestTag(UiTestTags.TAG_ACCOUNT_EDIT).assertIsDisplayed()
    selectTestTag(UiTestTags.TAG_USERNAME_FIELD).assertIsDisplayed().assertTextContains("user1")
    selectTestTag(UiTestTags.TAG_GOOGLE_FIELD)
        .assertIsDisplayed()
        .assertTextContains("user1@google.com")
    selectTestTag(UiTestTags.TAG_SIGNOUT_BUTTON).assertExists()

    // Read-only (not editing)
    selectTestTag(UiTestTags.TAG_USERNAME_EDIT).assertIsDisplayed()
    selectTestTag(UiTestTags.TAG_USERNAME_CANCEL).assertDoesNotExist()
    selectTestTag(UiTestTags.TAG_USERNAME_SAVE).assertDoesNotExist()
  }

  @Test
  fun showsAvatarImage_whenProfilePictureExists() {
    val avatarUri = Uri.parse("https://example.com/avatar.jpg")
    every { mockFirebaseUser.photoUrl } returns avatarUri
    coEvery { mockAccountRepository.getAccount("test-uid") } returns
        Account(
            uid = "test-uid",
            ownerId = "test-uid",
            username = "user1",
            profilePicture = avatarUri.toString())

    signIn(mockFirebaseUser)
    setContent()

    selectTestTag(UiTestTags.TAG_ACCOUNT_AVATAR_IMAGE).assertIsDisplayed()
  }

  @Test
  fun usernameEdit_flow_saveWithoutChange_staysEditing_thenCancel_restores() {
    signIn(mockFirebaseUser)
    setContent()

    // Open edit
    selectTestTag(UiTestTags.TAG_USERNAME_EDIT).performClick()
    selectTestTag(UiTestTags.TAG_USERNAME_CANCEL).assertIsDisplayed()
    selectTestTag(UiTestTags.TAG_USERNAME_SAVE).assertIsDisplayed()

    // Save without change -> stays editing (no-op)
    selectTestTag(UiTestTags.TAG_USERNAME_SAVE).performClick()
    selectTestTag(UiTestTags.TAG_USERNAME_EDIT).assertDoesNotExist()
    selectTestTag(UiTestTags.TAG_USERNAME_CANCEL).assertIsDisplayed()
    selectTestTag(UiTestTags.TAG_USERNAME_SAVE).assertIsDisplayed()

    // Cancel -> back to normal, original name remains
    selectTestTag(UiTestTags.TAG_USERNAME_CANCEL).performClick()
    selectTestTag(UiTestTags.TAG_USERNAME_EDIT).assertIsDisplayed()
    selectTestTag(UiTestTags.TAG_USERNAME_FIELD).assertTextContains("user1")
  }

  @Test
  fun privacyToggle_click_updatesLabel_andCallsRepository() {
    coEvery { mockAccountRepository.togglePrivacy("test-uid") } returns true

    signIn(mockFirebaseUser)
    setContent()

    // Initially Public
    composeTestRule.onNodeWithText("Public").assertIsDisplayed()

    val switchMatcher = isToggleable() and hasAnyAncestor(hasTestTag(UiTestTags.TAG_PRIVACY_TOGGLE))
    composeTestRule.onNode(switchMatcher).performClick()

    // Updated and repo called
    composeTestRule.onNodeWithText("Private").assertIsDisplayed()
    coVerify(exactly = 1) { mockAccountRepository.togglePrivacy("test-uid") }
  }

  @Test
  fun helpIcon_showsAndDismissesPopup() {
    signIn(mockFirebaseUser)
    setContent()

    selectTestTag(UiTestTags.TAG_PRIVACY_HELP_MENU).assertDoesNotExist()
    selectTestTag(UiTestTags.TAG_PRIVACY_HELP_ICON).performClick()
    selectTestTag(UiTestTags.TAG_PRIVACY_HELP_MENU).assertExists()
    selectTestTag(UiTestTags.TAG_PRIVACY_HELP_MENU).performClick()
    selectTestTag(UiTestTags.TAG_PRIVACY_HELP_MENU).assertDoesNotExist()
  }

  @Test
  fun backButton_invokesCallback() {
    val onBack = mockk<() -> Unit>(relaxed = true)
    signIn(mockFirebaseUser)
    setContent(onBack)

    selectTestTag(UiTestTags.TAG_ACCOUNT_BACK).performClick()
    verify { onBack() }
  }

  @Test
  fun deleteProfilePicture_buttonAppearsOnlyWithPicture_andCallsViewModel() {
    // Setup account with profile picture
    val avatarUri = "https://example.com/avatar.jpg"
    coEvery { mockAccountRepository.getAccount("test-uid") } returns
        Account(
            uid = "test-uid",
            ownerId = "test-uid",
            username = "user1",
            profilePicture = avatarUri,
            isPrivate = false)

    every { mockAccountService.currentUserId } returns "test-uid"
    coEvery { mockAccountRepository.deleteProfilePicture("test-uid") } returns Unit
    coEvery { mockUserRepository.deleteProfilePicture("test-uid") } returns Unit

    signIn(mockFirebaseUser)
    setContent()

    // Delete button should be visible when there's a profile picture
    selectTestTag(UiTestTags.TAG_ACCOUNT_DELETE).assertIsDisplayed()
    selectTestTag(UiTestTags.TAG_ACCOUNT_EDIT).assertIsDisplayed()
    composeTestRule.onNodeWithText("Edit").assertIsDisplayed()

    // Click delete button
    selectTestTag(UiTestTags.TAG_ACCOUNT_DELETE).performClick()
    composeTestRule.waitForIdle()

    // Verify both repositories were called
    coVerify(exactly = 1) { mockAccountRepository.deleteProfilePicture("test-uid") }
    coVerify(exactly = 1) { mockUserRepository.deleteProfilePicture("test-uid") }
  }

  @Test
  fun deleteProfilePicture_buttonHiddenWhenNoProfilePictureAndShowsUpload() {
    // Account without profile picture (default setup)
    signIn(mockFirebaseUser)
    setContent()

    // Delete button should not exist when there's no profile picture
    selectTestTag(UiTestTags.TAG_ACCOUNT_DELETE).assertDoesNotExist()
    // Upload button should be shown instead of Edit
    composeTestRule.onNodeWithText("Upload").assertIsDisplayed()
  }

  @Test
  fun handlePickedProfileImage_coversSuccessErrorAndBlank() {
    val ctx: Context = ApplicationProvider.getApplicationContext()

    // Success: edit is called with expected URL
    run {
      var editedCalled = false
      var editedUrl: String? = null
      val upload: (String, (String) -> Unit, (Throwable) -> Unit) -> Unit = { _, onSuccess, _ ->
        onSuccess("https://cdn.example.com/avatar.jpg")
      }

      composeTestRule.runOnUiThread {
        handlePickedProfileImage(
            localPath = "/local/path.jpg",
            upload = upload,
            editProfilePicture = {
              editedCalled = true
              editedUrl = it
            },
            context = ctx)
      }

      assertTrue(editedCalled)
      assertTrue(editedUrl == "https://cdn.example.com/avatar.jpg")
    }

    // Error: edit is not called
    run {
      var editedCalled = false
      val upload: (String, (String) -> Unit, (Throwable) -> Unit) -> Unit = { _, _, onError ->
        onError(IllegalStateException("upload failed"))
      }

      composeTestRule.runOnUiThread {
        handlePickedProfileImage(
            localPath = "/local/path.jpg",
            upload = upload,
            editProfilePicture = { editedCalled = true },
            context = ctx)
      }

      assertFalse(editedCalled)
    }

    // Blank path: no-op (upload not invoked, edit not called)
    run {
      var uploadInvoked = false
      var editedCalled = false
      val upload: (String, (String) -> Unit, (Throwable) -> Unit) -> Unit = { _, _, _ ->
        uploadInvoked = true
      }

      composeTestRule.runOnUiThread {
        handlePickedProfileImage(
            localPath = "",
            upload = upload,
            editProfilePicture = { editedCalled = true },
            context = ctx)
      }

      assertFalse(uploadInvoked)
      assertFalse(editedCalled)
    }
  }

  // --- Location field tests ---

  @Test
  fun locationField_displaysSavedLocation_whenAccountHasLocation() {
    // Setup account with saved location
    val savedLocation = Location(46.5191, 6.5668, "EPFL, Lausanne")
    coEvery { mockAccountRepository.getAccount("test-uid") } returns
        Account(
            uid = "test-uid",
            ownerId = "test-uid",
            username = "user1",
            profilePicture = "",
            isPrivate = false,
            location = savedLocation)

    signIn(mockFirebaseUser)
    setContent()
    waitForLoadingToComplete()

    // Location field should display the saved location - use the correct test tag
    selectTestTag(LocationSelectionTestTags.INPUT_LOCATION)
        .assertIsDisplayed()
        .assertTextContains("EPFL, Lausanne")
  }

  @Test
  fun locationField_clearButton_makesFieldEditable() {
    // Setup account with saved location
    val savedLocation = Location(46.5191, 6.5668, "EPFL, Lausanne")
    coEvery { mockAccountRepository.getAccount("test-uid") } returns
        Account(
            uid = "test-uid",
            ownerId = "test-uid",
            username = "user1",
            profilePicture = "",
            isPrivate = false,
            location = savedLocation)

    signIn(mockFirebaseUser)
    setContent()
    waitForLoadingToComplete()

    // Verify location is displayed - use the correct test tag
    selectTestTag(LocationSelectionTestTags.INPUT_LOCATION).assertTextContains("EPFL, Lausanne")

    // Click clear button
    selectTestTag(LocationSelectionTestTags.LOCATION_CLEAR_BUTTON).performClick()
    composeTestRule.waitForIdle()

    // Field should now be editable (text cleared)
    selectTestTag(LocationSelectionTestTags.INPUT_LOCATION).performTextInput("Zurich")
    composeTestRule.waitForIdle()

    // Verify text was entered (field is editable)
    selectTestTag(LocationSelectionTestTags.INPUT_LOCATION).assertTextContains("Zurich")
  }

  @Test
  fun locationField_gpsButton_exists() {
    signIn(mockFirebaseUser)
    setContent()

    // GPS button should be visible
    selectTestTag(LocationSelectionTestTags.LOCATION_GPS_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithText("Update Location (GPS)").assertIsDisplayed()
  }
}
