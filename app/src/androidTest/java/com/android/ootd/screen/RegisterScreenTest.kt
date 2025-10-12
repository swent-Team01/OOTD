package com.android.ootd.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.android.ootd.model.user.UserRepository
import com.android.ootd.ui.register.RegisterScreen
import com.android.ootd.ui.register.RegisterScreenTestTags
import com.android.ootd.ui.register.RegisterViewModel
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RegisterScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var viewModel: RegisterViewModel
  private lateinit var repository: UserRepository

  @Before
  fun setUp() {
    repository = mockk(relaxed = true)
    viewModel = RegisterViewModel(repository)
    composeTestRule.setContent { RegisterScreen(viewModel = viewModel) }
  }

  @Test
  fun displayAllComponents() {
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE)
        .assertTextContains("Save", substring = true, ignoreCase = true)
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.APP_LOGO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.WELCOME_TITLE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsNotDisplayed()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_APP_SLOGAN).assertIsDisplayed()
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
  fun loadingCircle_not_visible_when_saving_invalid_user() {
    composeTestRule.enterUsername("  ")

    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE).performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_LOADING).assertDoesNotExist()
  }

  @Test
  fun registerScreen_showsLoadingIndicator_whenIsLoadingIsTrue() {
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_LOADING).assertDoesNotExist()
    viewModel.showLoading(true)

    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_LOADING).assertExists()
    composeTestRule.onNodeWithText("Saving…").assertExists()
    viewModel.showLoading(false)
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
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_DATE).performClick()
    composeTestRule.waitForIdle()
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME)
        .performClick() // Leave field

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertTextContains("Please enter a valid date")
  }

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
  fun registerButton_enabled_whenBothFieldsValid() {
    composeTestRule.enterUsername("validUser")
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.DATE_PICKER_ICON, useUnmergedTree = true)
        .performClick()

    composeTestRule.waitForIdle()
    composeTestRule.enterDate()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE).assertIsEnabled()
  }

  @Test
  fun inputFields_disabled_whenLoading() {
    viewModel.showLoading(true)

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).assertIsNotEnabled()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_DATE).assertIsNotEnabled()
  }
}
