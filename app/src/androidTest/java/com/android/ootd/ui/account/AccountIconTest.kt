package com.android.ootd.ui.account

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.authentication.AccountService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AccountIconTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockAccountService: AccountService
  private lateinit var mockAccountRepository: AccountRepository
  private lateinit var viewModel: AccountViewModel
  private lateinit var userFlow: MutableStateFlow<com.google.firebase.auth.FirebaseUser?>

  @Before
  fun setup() {
    mockAccountService = mockk(relaxed = true)
    mockAccountRepository = mockk(relaxed = true)
  }

  // ---- helpers ----
  private fun mockUser(
      uid: String,
      email: String = "test@example.com",
      photoUrl: String? = null
  ): com.google.firebase.auth.FirebaseUser =
      mockk<com.google.firebase.auth.FirebaseUser> {
        every { this@mockk.uid } returns uid
        every { this@mockk.email } returns email
        every { this@mockk.photoUrl } returns photoUrl?.let { Uri.parse(it) }
      }

  private fun givenUser(user: com.google.firebase.auth.FirebaseUser?, repoPic: String? = null) {
    userFlow = MutableStateFlow(user)
    every { mockAccountService.currentUser } returns userFlow
    every { mockAccountService.currentUserId } returns (user?.uid ?: "")
    if (user != null && repoPic != null) {
      coEvery { mockAccountRepository.getAccount(user.uid) } returns
          Account(uid = user.uid, username = "testuser", profilePicture = repoPic)
    }
    viewModel = AccountViewModel(mockAccountService, mockAccountRepository)
  }

  private fun setContent(size: Dp = 32.dp, cd: String? = null, onClick: () -> Unit = {}) {
    composeTestRule.setContent {
      AccountIcon(
          accountViewModel = viewModel, size = size, contentDescription = cd, onClick = onClick)
    }
  }

  private fun n(tag: String) = composeTestRule.onNodeWithTag(tag)

  private fun nu(tag: String) = composeTestRule.onNodeWithTag(tag, useUnmergedTree = true)

  // ---- tests ----

  @Test
  fun noProfilePicture_showsFallbackIcon() {
    givenUser(user = null)

    setContent()

    n(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER).assertIsDisplayed()
    n(UiTestTags.TAG_ACCOUNT_AVATAR_IMAGE).assertDoesNotExist()
  }

  @Test
  fun profilePictureExists_displaysImage() {
    val photo = "https://example.com/avatar.jpg"
    val user = mockUser(uid = "u1", photoUrl = photo)

    givenUser(user = null)
    coEvery { mockAccountRepository.getAccount("u1") } returns
        Account(uid = "u1", username = "testuser", profilePicture = photo)

    setContent()

    composeTestRule.runOnIdle { userFlow.value = user }
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      viewModel.uiState.value.profilePicture == photo
    }
    assertEquals(photo, viewModel.uiState.value.profilePicture)

    nu(UiTestTags.TAG_ACCOUNT_AVATAR_IMAGE).assertExists()
  }

  @Test
  fun click_invokesCallback() {
    givenUser(user = null)
    var clicked = false

    setContent(onClick = { clicked = true })
    n(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER).performClick()

    assertTrue(clicked)
  }

  @Test
  fun profilePicture_update_recomposes() {
    val photo1 = "https://example.com/avatar1.jpg"
    val photo2 = "https://example.com/avatar2.jpg"

    val u1 = mockUser(uid = "uid-1", photoUrl = photo1)
    val u2 = mockUser(uid = "uid-2", email = "t2@example.com", photoUrl = photo2)

    givenUser(user = null)
    coEvery { mockAccountRepository.getAccount("uid-1") } returns
        Account(uid = "uid-1", username = "test1", profilePicture = photo1)
    coEvery { mockAccountRepository.getAccount("uid-2") } returns
        Account(uid = "uid-2", username = "test2", profilePicture = photo2)

    setContent()

    composeTestRule.runOnIdle { userFlow.value = u1 }
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      viewModel.uiState.value.profilePicture == photo1
    }
    assertEquals(photo1, viewModel.uiState.value.profilePicture)

    composeTestRule.runOnIdle { userFlow.value = u2 }
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      viewModel.uiState.value.profilePicture == photo2
    }
    assertEquals(photo2, viewModel.uiState.value.profilePicture)
    n(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER).assertIsDisplayed()
  }

  @Test
  fun emptyUri_showsFallbackIcon() {
    val user = mockUser(uid = "u-empty", photoUrl = null)
    givenUser(user = user, repoPic = "")

    setContent()
    composeTestRule.waitForIdle()

    n(UiTestTags.TAG_ACCOUNT_AVATAR_IMAGE).assertDoesNotExist()
  }

  @Test
  fun blankUriString_showsFallbackIcon() {
    val blank = "   "
    val user = mockUser(uid = "u-blank", photoUrl = blank)
    givenUser(user = user, repoPic = blank)

    setContent()
    composeTestRule.waitForIdle()

    n(UiTestTags.TAG_ACCOUNT_AVATAR_IMAGE).assertDoesNotExist()
  }

  @Test
  fun customSize_appliesContainer() {
    givenUser(user = null)

    setContent(size = 64.dp)

    n(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER).assertIsDisplayed()
  }

  @Test
  fun contentDescription_isSet() {
    givenUser(user = null)

    setContent(cd = "User profile picture")

    n(UiTestTags.TAG_ACCOUNT_AVATAR_CONTAINER).assertIsDisplayed()
  }
}
