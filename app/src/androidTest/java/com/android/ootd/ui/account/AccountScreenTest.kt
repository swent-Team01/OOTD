package com.android.ootd.ui.account
/*
 * DISCLAIMER: This file was created/modified with the assistance of GitHub Copilot.
 * Copilot provided suggestions which were reviewed and adapted by the developer.
 */
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.credentials.CredentialManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import com.android.ootd.ui.theme.OOTDTheme
import com.google.firebase.auth.FirebaseUser
import io.mockk.clearAllMocks
import io.mockk.coEvery
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
  private lateinit var mockUserRepository: UserRepository
  private lateinit var mockCredentialManager: CredentialManager
  private lateinit var mockFirebaseUser: FirebaseUser
  private lateinit var viewModel: AccountViewModel

  private val userFlow = MutableStateFlow<FirebaseUser?>(null)

  @Before
  fun setup() {
    mockAccountService = mockk(relaxed = true)
    mockUserRepository = mockk(relaxed = true)
    mockCredentialManager = mockk(relaxed = true)
    mockFirebaseUser = mockk(relaxed = true)

    every { mockAccountService.currentUser } returns userFlow
    every { mockFirebaseUser.uid } returns "test-uid"
    every { mockFirebaseUser.email } returns "user1@google.com"
    every { mockFirebaseUser.photoUrl } returns null

    coEvery { mockUserRepository.getUser("test-uid") } returns
        User(uid = "test-uid", username = "user1", profilePicture = Uri.EMPTY)

    viewModel = AccountViewModel(mockAccountService, mockUserRepository)
  }

  @After
  fun tearDown() {
    clearAllMocks()
  }

  @Test
  fun accountScreen_showsAllMainElements_withoutAvatar() {
    userFlow.value = mockFirebaseUser

    composeTestRule.setContent {
      OOTDTheme {
        AccountScreen(accountViewModel = viewModel, credentialManager = mockCredentialManager)
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_BACK).assertIsDisplayed()
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_IMAGE).assertDoesNotExist()
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_EDIT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(UiTestTags.TAG_USERNAME_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(UiTestTags.TAG_USERNAME_CLEAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(UiTestTags.TAG_GOOGLE_FIELD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(UiTestTags.TAG_SIGNOUT_BUTTON).assertIsDisplayed()
  }

  @Test
  fun accountScreen_showsAvatarImage_whenProfilePictureExists() {
    val avatarUri = Uri.parse("https://example.com/avatar.jpg")
    every { mockFirebaseUser.photoUrl } returns avatarUri
    coEvery { mockUserRepository.getUser("test-uid") } returns
        User(uid = "test-uid", username = "user1", profilePicture = avatarUri)

    userFlow.value = mockFirebaseUser

    composeTestRule.setContent {
      OOTDTheme {
        AccountScreen(accountViewModel = viewModel, credentialManager = mockCredentialManager)
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_IMAGE).assertIsDisplayed()
  }

  @Test
  fun accountScreen_displaysCorrectUserInfo() {
    userFlow.value = mockFirebaseUser

    composeTestRule.setContent {
      OOTDTheme {
        AccountScreen(accountViewModel = viewModel, credentialManager = mockCredentialManager)
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(UiTestTags.TAG_USERNAME_FIELD).assertTextContains("user1")
    composeTestRule
        .onNodeWithTag(UiTestTags.TAG_GOOGLE_FIELD)
        .assertTextContains("user1@google.com")
  }

  @Test
  fun accountScreen_callsOnBack_whenBackButtonClicked() {
    val onBack = mockk<() -> Unit>(relaxed = true)
    userFlow.value = mockFirebaseUser

    composeTestRule.setContent {
      OOTDTheme {
        AccountScreen(
            accountViewModel = viewModel,
            credentialManager = mockCredentialManager,
            onBack = onBack)
      }
    }

    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_BACK).performClick()
    verify { onBack() }
  }

  @Test
  fun accountScreen_callsOnEditAvatar_whenEditButtonClicked() {
    val onEditAvatar = mockk<() -> Unit>(relaxed = true)
    userFlow.value = mockFirebaseUser

    composeTestRule.setContent {
      OOTDTheme {
        AccountScreen(
            accountViewModel = viewModel,
            credentialManager = mockCredentialManager,
            onEditAvatar = onEditAvatar)
      }
    }

    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_EDIT).performClick()
    verify { onEditAvatar() }
  }

  @Test
  fun accountScreen_showsLoadingIndicator_whenLoading() {
    userFlow.value = null

    composeTestRule.setContent {
      OOTDTheme {
        AccountScreen(accountViewModel = viewModel, credentialManager = mockCredentialManager)
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_LOADING).assertDoesNotExist()
  }
}
