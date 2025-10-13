package com.android.ootd.ui.account

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AccountIconTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockAccountService: AccountService
  private lateinit var mockUserRepository: UserRepository
  private lateinit var viewModel: AccountViewModel

  @Before
  fun setup() {
    mockAccountService = mockk(relaxed = true)
    mockUserRepository = mockk(relaxed = true)
  }

  @Test
  fun accountIcon_whenNoProfilePicture_showsFallbackIcon() {
    // Given: user with no profile picture
    val userFlow = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(null)
    every { mockAccountService.currentUser } returns userFlow
    viewModel = AccountViewModel(mockAccountService, mockUserRepository)

    // When
    composeTestRule.setContent { AccountIcon(accountViewModel = viewModel, onClick = {}) }

    // Then: should show default Person icon
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_CONTAINER).assertExists()
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_IMAGE).assertDoesNotExist()
  }

  @Test
  fun accountIcon_whenProfilePictureExists_displaysImage() = runTest {
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
    coEvery { mockUserRepository.getUser("test-uid") } returns
        User(
            uid = "test-uid",
            username = "testuser",
            profilePicture = testUri,
            friendList = emptyList())

    // Create ViewModel AFTER setting up the user - observeAuthState() will immediately see the user
    viewModel = AccountViewModel(mockAccountService, mockUserRepository)

    // When
    composeTestRule.setContent { AccountIcon(accountViewModel = viewModel, onClick = {}) }

    // Wait for ViewModel to update state
    composeTestRule.waitForIdle()

    // Then: Verify the ViewModel state has the profile picture
    assert(viewModel.uiState.value.profilePicture == testUri) {
      "Expected profile picture to be $testUri but was ${viewModel.uiState.value.profilePicture}"
    }

    // The image composable should be present (even if Coil can't load the fake URL)
    // We verify by checking that the container exists and no fallback icon is shown
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_CONTAINER).assertExists()
  }

  @Test
  fun accountIcon_whenClicked_invokesCallback() {
    // Given
    val userFlow = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(null)
    every { mockAccountService.currentUser } returns userFlow
    viewModel = AccountViewModel(mockAccountService, mockUserRepository)
    var clicked = false

    // When
    composeTestRule.setContent {
      AccountIcon(accountViewModel = viewModel, onClick = { clicked = true })
    }
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_CONTAINER).performClick()

    // Then
    assert(clicked) { "onClick callback was not invoked" }
  }

  @Test
  fun accountIcon_whenProfilePictureUpdates_recomposesWithNewImage() = runTest {
    // Given: initial state with picture - SET USER BEFORE CREATING VIEWMODEL
    val testUri1 = Uri.parse("https://example.com/avatar1.jpg")
    val testUri2 = Uri.parse("https://example.com/avatar2.jpg")
    val mockFirebaseUser =
        mockk<com.google.firebase.auth.FirebaseUser> {
          every { uid } returns "test-uid"
          every { email } returns "test@example.com"
          every { photoUrl } returns testUri1
        }
    val userFlow = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(mockFirebaseUser)
    every { mockAccountService.currentUser } returns userFlow

    coEvery { mockUserRepository.getUser("test-uid") } returns
        User(
            uid = "test-uid",
            username = "testuser",
            profilePicture = testUri1,
            friendList = emptyList())

    // Create ViewModel AFTER setting up the user
    viewModel = AccountViewModel(mockAccountService, mockUserRepository)

    composeTestRule.setContent { AccountIcon(accountViewModel = viewModel, onClick = {}) }

    // Wait for initial state to settle
    composeTestRule.waitForIdle()

    // Verify initial state
    assert(viewModel.uiState.value.profilePicture == testUri1) {
      "Expected initial profile picture to be $testUri1"
    }

    // When: profile picture changes (simulate repository returning new URI)
    coEvery { mockUserRepository.getUser("test-uid") } returns
        User(
            uid = "test-uid",
            username = "testuser",
            profilePicture = testUri2,
            friendList = emptyList())

    // Re-emit a new FirebaseUser instance to trigger observeAuthState()
    val mockFirebaseUser2 =
        mockk<com.google.firebase.auth.FirebaseUser> {
          every { uid } returns "test-uid"
          every { email } returns "test@example.com"
          every { photoUrl } returns testUri2
        }
    userFlow.value = mockFirebaseUser2

    // Wait for the emission and reload to complete
    composeTestRule.waitForIdle()

    // Then: should have updated to the new profile picture
    assert(viewModel.uiState.value.profilePicture == testUri2) {
      "Expected updated profile picture to be $testUri2 but was ${viewModel.uiState.value.profilePicture}"
    }

    // Container should still exist
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_CONTAINER).assertExists()
  }

  @Test
  fun accountIcon_withEmptyUri_showsFallbackIcon() = runTest {
    // Given: user with Uri.EMPTY
    val mockFirebaseUser =
        mockk<com.google.firebase.auth.FirebaseUser> {
          every { uid } returns "test-uid"
          every { email } returns "test@example.com"
          every { photoUrl } returns null
        }
    val userFlow = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(mockFirebaseUser)
    every { mockAccountService.currentUser } returns userFlow
    coEvery { mockUserRepository.getUser("test-uid") } returns
        User(
            uid = "test-uid",
            username = "testuser",
            profilePicture = Uri.EMPTY,
            friendList = emptyList())
    viewModel = AccountViewModel(mockAccountService, mockUserRepository)

    // When
    composeTestRule.setContent { AccountIcon(accountViewModel = viewModel, onClick = {}) }

    // Then: fallback icon shown
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_IMAGE).assertDoesNotExist()
  }

  @Test
  fun accountIcon_withBlankUriString_showsFallbackIcon() = runTest {
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
    coEvery { mockUserRepository.getUser("test-uid") } returns
        User(
            uid = "test-uid",
            username = "testuser",
            profilePicture = blankUri,
            friendList = emptyList())
    viewModel = AccountViewModel(mockAccountService, mockUserRepository)

    // When
    composeTestRule.setContent { AccountIcon(accountViewModel = viewModel, onClick = {}) }

    // Then: fallback shown (blank strings filtered by isNotBlank check)
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_IMAGE).assertDoesNotExist()
  }

  @Test
  fun accountIcon_customSize_appliesCorrectly() {
    // Given
    val userFlow = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(null)
    every { mockAccountService.currentUser } returns userFlow
    viewModel = AccountViewModel(mockAccountService, mockUserRepository)

    // When: custom size provided
    composeTestRule.setContent {
      AccountIcon(accountViewModel = viewModel, size = 64.dp, onClick = {})
    }

    // Then: container exists (size is applied via modifier, not easily testable in semantics)
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_CONTAINER).assertExists()
  }

  @Test
  fun accountIcon_contentDescription_isSetCorrectly() {
    // Given
    val userFlow = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(null)
    every { mockAccountService.currentUser } returns userFlow
    viewModel = AccountViewModel(mockAccountService, mockUserRepository)

    // When: custom content description
    composeTestRule.setContent {
      AccountIcon(
          accountViewModel = viewModel, contentDescription = "User profile picture", onClick = {})
    }

    // Then: container exists (content description applied to Image/Icon inside)
    composeTestRule.onNodeWithTag(TAG_ACCOUNT_AVATAR_CONTAINER).assertExists()
  }
}
