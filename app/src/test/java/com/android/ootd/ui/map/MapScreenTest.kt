package com.android.ootd.ui.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.ootd.model.account.Account
import com.android.ootd.model.account.AccountRepository
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.map.Location
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * UI tests for MapScreen.
 *
 * Tests cover:
 * - UI component visibility
 * - Map display when loaded
 * - Back button interaction
 */
@RunWith(RobolectricTestRunner::class)
class MapScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  private lateinit var mockViewModel: MapViewModel
  private lateinit var mockAccountService: AccountService
  private lateinit var mockAccountRepository: AccountRepository
  private val testLocation = Location(46.5197, 6.6323, "Lausanne")

  @Before
  fun setUp() {
    mockAccountService = mockk(relaxed = true)
    mockAccountRepository = mockk(relaxed = true)

    coEvery { mockAccountService.currentUserId } returns "testUser"
    coEvery { mockAccountRepository.getAccount(any()) } returns
        Account(uid = "testUser", location = testLocation)

    mockViewModel = MapViewModel(mockAccountService, mockAccountRepository)
  }

  @Test
  fun mapScreen_displaysAllUIComponents() {
    composeTestRule.setContent { MapScreen(viewModel = mockViewModel) }

    composeTestRule.onNodeWithTag(MapScreenTestTags.SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.TOP_BAR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithText("MAP").assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.BACK_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.CONTENT_BOX).assertIsDisplayed()
  }

  @Test
  fun mapScreen_showsMap_whenNotLoading() {
    composeTestRule.setContent { MapScreen(viewModel = mockViewModel) }

    // Wait for loading to complete
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
  }

  @Test
  fun backButton_callsOnBack_whenClicked() {
    var backCalled = false
    val onBack = { backCalled = true }

    composeTestRule.setContent { MapScreen(viewModel = mockViewModel, onBack = onBack) }

    composeTestRule.onNodeWithTag(MapScreenTestTags.BACK_BUTTON).performClick()

    assert(backCalled)
  }
}
