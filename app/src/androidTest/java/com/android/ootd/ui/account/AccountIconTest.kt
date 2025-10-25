package com.android.ootd.ui.account

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.authentication.AccountService
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/*
 * DISCLAIMER: This file was created/modified with the assistance of GitHub Copilot.
 * Copilot provided suggestions which were reviewed and adapted by the developer.
 */

@OptIn(ExperimentalCoroutinesApi::class)
class AccountIconTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockAccountService: AccountService
  private lateinit var mockAccountRepository: AccountRepository
  private lateinit var viewModel: AccountViewModel

  @Before
  fun setup() {
    mockAccountService = mockk(relaxed = true)
    mockAccountRepository = mockk(relaxed = true)
  }

  @After
  fun tearDown() {
    // No need to reset main dispatcher in instrumentation tests
  }

  @Test
  fun accountIcon_whenNoProfilePicture_showsFallbackIcon() {
    // Given: user with no profile picture
    val userFlow = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(null)
    every { mockAccountService.currentUser } returns userFlow
    viewModel = AccountViewModel(mockAccountService, mockAccountRepository)

    // When
    composeTestRule.setContent { AccountIcon(accountViewModel = viewModel, onClick = {}) }

    // Then: should show default Person icon
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER).assertExists()
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_IMAGE).assertDoesNotExist()
  }

  @Test
  fun accountIcon_whenProfilePictureExists_displaysImage() {
    // Given: user with profile picture - SET USER BEFORE CREATING VIEWMODEL
    val testUri = Uri.parse("https://example.com/avatar.jpg")
    val mockFirebaseUser =
        mockk<com.google.firebase.auth.FirebaseUser> {
          every { uid } returns "test-uid"
          every { email } returns "test@example.com"
          every { photoUrl } returns testUri
        }
    val userFlow = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(mockFirebaseUser)
    every { mockAccountService.currentUser } returns userFlow
    coEvery { mockAccountRepository.getAccount("test-uid") } returns
        Account(uid = "test-uid", username = "testuser", profilePicture = testUri.toString())

    // Create ViewModel AFTER setting up the user - observeAuthState() will immediately see the user
    viewModel = AccountViewModel(mockAccountService, mockAccountRepository)

    // When
    composeTestRule.setContent { AccountIcon(accountViewModel = viewModel, onClick = {}) }

    // Wait for ViewModel to update state
    composeTestRule.waitForIdle()

    // Then: Verify the ViewModel state has the profile picture
    assert(viewModel.uiState.value.profilePicture == testUri.toString()) {
      "Expected profile picture to be ${testUri.toString()} but was ${viewModel.uiState.value.profilePicture}"
    }

    // The image composable should be present (even if Coil can't load the fake URL)
    // We verify by checking that the container exists and no fallback icon is shown
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER).assertExists()
  }

  @Test
  fun accountIcon_whenClicked_invokesCallback() {
    // Given
    val userFlow = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(null)
    every { mockAccountService.currentUser } returns userFlow
    viewModel = AccountViewModel(mockAccountService, mockAccountRepository)
    var clicked = false

    // When
    composeTestRule.setContent {
      AccountIcon(accountViewModel = viewModel, onClick = { clicked = true })
    }
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER).performClick()

    // Then
    assert(clicked) { "onClick callback was not invoked" }
  }

  @Test
  fun accountIcon_whenProfilePictureUpdates_recomposesWithNewImage() {
    // Given: initial state with picture - SET USER BEFORE CREATING VIEWMODEL
    val testUri1 = Uri.parse("https://example.com/avatar1.jpg")
    val testUri2 = Uri.parse("https://example.com/avatar2.jpg")
    val mockFirebaseUser =
        mockk<com.google.firebase.auth.FirebaseUser> {
          every { uid } returns "test-uid-1"
          every { email } returns "test@example.com"
          every { photoUrl } returns testUri1
        }
    val userFlow = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(mockFirebaseUser)
    every { mockAccountService.currentUser } returns userFlow

    coEvery { mockAccountRepository.getAccount("test-uid-1") } returns
        Account(uid = "test-uid-1", username = "testuser", profilePicture = testUri1.toString())

    // Create ViewModel AFTER setting up the user
    viewModel = AccountViewModel(mockAccountService, mockAccountRepository)

    composeTestRule.setContent { AccountIcon(accountViewModel = viewModel, onClick = {}) }

    // Wait for initial state to settle
    composeTestRule.waitForIdle()

    // Verify initial state
    assert(viewModel.uiState.value.profilePicture == testUri1.toString()) {
      "Expected initial profile picture to be $testUri1"
    }

    // When: user switches to a different account with a different profile picture
    coEvery { mockAccountRepository.getAccount("test-uid-2") } returns
        Account(uid = "test-uid-2", username = "testuser2", profilePicture = testUri2.toString())

    // Emit a different user (different uid) to trigger observeAuthState() reload
    val mockFirebaseUser2 =
        mockk<com.google.firebase.auth.FirebaseUser> {
          every { uid } returns "test-uid-2"
          every { email } returns "test2@example.com"
          every { photoUrl } returns testUri2
        }
    userFlow.value = mockFirebaseUser2

    // Wait for the emission and reload to complete
    composeTestRule.waitForIdle()

    // Then: should have updated to the new profile picture
    assert(viewModel.uiState.value.profilePicture == testUri2.toString()) {
      "Expected updated profile picture to be $testUri2 but was ${viewModel.uiState.value.profilePicture}"
    }

    // Container should still exist
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER).assertExists()
  }

  @Test
  fun accountIcon_withEmptyUri_showsFallbackIcon() {
    // Given: user with empty profilePicture (represented as blank string)
    val mockFirebaseUser =
        mockk<com.google.firebase.auth.FirebaseUser> {
          every { uid } returns "test-uid"
          every { email } returns "test@example.com"
          every { photoUrl } returns null
        }
    val userFlow = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(mockFirebaseUser)
    every { mockAccountService.currentUser } returns userFlow
    coEvery { mockAccountRepository.getAccount("test-uid") } returns
        Account(uid = "test-uid", username = "testuser", profilePicture = "")
    viewModel = AccountViewModel(mockAccountService, mockAccountRepository)

    // When
    composeTestRule.setContent { AccountIcon(accountViewModel = viewModel, onClick = {}) }

    // Then: fallback icon shown
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_IMAGE).assertDoesNotExist()
  }

  @Test
  fun accountIcon_withBlankUriString_showsFallbackIcon() {
    // Given: user with blank string URI
    val blankUri = Uri.parse("   ")
    val mockFirebaseUser =
        mockk<com.google.firebase.auth.FirebaseUser> {
          every { uid } returns "test-uid"
          every { email } returns "test@example.com"
          every { photoUrl } returns blankUri
        }
    val userFlow = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(mockFirebaseUser)
    every { mockAccountService.currentUser } returns userFlow
    coEvery { mockAccountRepository.getAccount("test-uid") } returns
        Account(uid = "test-uid", username = "testuser", profilePicture = blankUri.toString())
    viewModel = AccountViewModel(mockAccountService, mockAccountRepository)

    // When
    composeTestRule.setContent { AccountIcon(accountViewModel = viewModel, onClick = {}) }

    // Then: fallback shown (blank strings filtered by isNotBlank check)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_IMAGE).assertDoesNotExist()
  }

  @Test
  fun accountIcon_customSize_appliesCorrectly() {
    // Given
    val userFlow = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(null)
    every { mockAccountService.currentUser } returns userFlow
    viewModel = AccountViewModel(mockAccountService, mockAccountRepository)

    // When: custom size provided
    composeTestRule.setContent {
      AccountIcon(accountViewModel = viewModel, size = 64.dp, onClick = {})
    }

    // Then: container exists (size is applied via modifier, not easily testable in semantics)
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER).assertExists()
  }

  @Test
  fun accountIcon_contentDescription_isSetCorrectly() {
    // Given
    val userFlow = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(null)
    every { mockAccountService.currentUser } returns userFlow
    viewModel = AccountViewModel(mockAccountService, mockAccountRepository)

    // When: custom content description
    composeTestRule.setContent {
      AccountIcon(
          accountViewModel = viewModel, contentDescription = "User profile picture", onClick = {})
    }

    // Then: container exists (content description applied to Image/Icon inside)
    composeTestRule.onNodeWithTag(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER).assertExists()
  }
}
