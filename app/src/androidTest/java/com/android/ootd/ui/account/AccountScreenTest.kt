package com.android.ootd.ui.account

/*
 * DISCLAIMER: This file was created/modified with the assistance of GitHub Copilot.
 * Copilot provided suggestions which were reviewed and adapted by the developers.
 */
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
import androidx.credentials.CredentialManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.ui.theme.OOTDTheme
import com.google.firebase.auth.FirebaseUser
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var mockAccountService: AccountService
  private lateinit var mockAccountRepository: AccountRepository
  private lateinit var mockCredentialManager: CredentialManager
  private lateinit var mockFirebaseUser: FirebaseUser
  private lateinit var viewModel: AccountViewModel

  private val userFlow = MutableStateFlow<FirebaseUser?>(null)

  @Before
  fun setup() {
    mockAccountService = mockk(relaxed = true)
    mockAccountRepository = mockk(relaxed = true)
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

    viewModel = AccountViewModel(mockAccountService, mockAccountRepository)
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

  private fun n(tag: String) = composeTestRule.onNodeWithTag(tag)

  // --- Tests (fewer, but comprehensive) ---

  @Test
  fun rendersBasic_withoutAvatar_andShowsUserInfo_andReadOnlyState() {
    signIn(mockFirebaseUser)
    setContent()

    // Core chrome
    n(UiTestTags.TAG_ACCOUNT_BACK).assertIsDisplayed()
    n(UiTestTags.TAG_ACCOUNT_TITLE).assertIsDisplayed()
    n(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER).assertIsDisplayed()

    // No photo -> letter avatar shown, image absent
    n(UiTestTags.TAG_ACCOUNT_AVATAR_LETTER).assertIsDisplayed()
    n(UiTestTags.TAG_ACCOUNT_AVATAR_IMAGE).assertDoesNotExist()

    // Actions and fields
    n(UiTestTags.TAG_ACCOUNT_EDIT).assertIsDisplayed()
    n(UiTestTags.TAG_USERNAME_FIELD).assertIsDisplayed().assertTextContains("user1")
    n(UiTestTags.TAG_GOOGLE_FIELD).assertIsDisplayed().assertTextContains("user1@google.com")
    n(UiTestTags.TAG_SIGNOUT_BUTTON).assertExists()

    // Read-only (not editing)
    n(UiTestTags.TAG_USERNAME_EDIT).assertIsDisplayed()
    n(UiTestTags.TAG_USERNAME_CANCEL).assertDoesNotExist()
    n(UiTestTags.TAG_USERNAME_SAVE).assertDoesNotExist()
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

    n(UiTestTags.TAG_ACCOUNT_AVATAR_IMAGE).assertIsDisplayed()
  }

  @Test
  fun usernameEdit_flow_saveWithoutChange_staysEditing_thenCancel_restores() {
    signIn(mockFirebaseUser)
    setContent()

    // Open edit
    n(UiTestTags.TAG_USERNAME_EDIT).performClick()
    n(UiTestTags.TAG_USERNAME_CANCEL).assertIsDisplayed()
    n(UiTestTags.TAG_USERNAME_SAVE).assertIsDisplayed()

    // Save without change -> stays editing (no-op)
    n(UiTestTags.TAG_USERNAME_SAVE).performClick()
    n(UiTestTags.TAG_USERNAME_EDIT).assertDoesNotExist()
    n(UiTestTags.TAG_USERNAME_CANCEL).assertIsDisplayed()
    n(UiTestTags.TAG_USERNAME_SAVE).assertIsDisplayed()

    // Cancel -> back to normal, original name remains
    n(UiTestTags.TAG_USERNAME_CANCEL).performClick()
    n(UiTestTags.TAG_USERNAME_EDIT).assertIsDisplayed()
    n(UiTestTags.TAG_USERNAME_FIELD).assertTextContains("user1")
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

    n(UiTestTags.TAG_PRIVACY_HELP_MENU).assertDoesNotExist()
    n(UiTestTags.TAG_PRIVACY_HELP_ICON).performClick()
    n(UiTestTags.TAG_PRIVACY_HELP_MENU).assertExists()
    n(UiTestTags.TAG_PRIVACY_HELP_MENU).performClick()
    n(UiTestTags.TAG_PRIVACY_HELP_MENU).assertDoesNotExist()
  }

  @Test
  fun backButton_invokesCallback() {
    val onBack = mockk<() -> Unit>(relaxed = true)
    signIn(mockFirebaseUser)
    setContent(onBack)

    n(UiTestTags.TAG_ACCOUNT_BACK).performClick()
    verify { onBack() }
  }
}
