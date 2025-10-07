package com.android.ootd.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.android.ootd.ui.register.RegisterScreen
import com.android.ootd.ui.register.RegisterScreenTestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RegisterScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Before
  fun setUp() {
    composeTestRule.setContent { RegisterScreen() }
  }

  @Test
  fun displayAllComponents() {
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE)
        .assertTextContains("Save", substring = true, ignoreCase = true)
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.INPUT_REGISTER_UNAME).assertIsDisplayed()
    // composeTestRule.onNodeWithTag(SignInScreenTestTags.INPUT_SIGNIN_DATE).assertIsDisplayed()
    // composeTestRule.onNodeWithTag(SignInScreenTestTags.APP_LOGO).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsNotDisplayed()
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

  //    @Test
  //    fun canEnterDate() {
  //        val text = "01/01/2000"
  //        composeTestRule.enterDate(text)
  //        composeTestRule
  //            .onNodeWithTag(SignInScreenTestTags.INPUT_SIGNIN_DATE)
  //            .assertTextContains(text)
  //        composeTestRule
  //            .onNodeWithTag(SignInScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
  //            .assertDoesNotExist()
  //    }

  @Test
  fun usernameError_whenEmpty_showsError_andDisablesButton() {
    composeTestRule.enterUsername("")
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE)
        .assertIsDisplayed()
        .assertIsNotEnabled()
  }

  @Test
  fun usernameError_whenBlankSpaces_showsError_andDisablesButton() {
    composeTestRule.enterUsername("   ")
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE)
        .assertIsDisplayed()
        .assertIsNotEnabled()
  }

  @Test
  fun buttonVisible_whenNoErrors() {
    composeTestRule.enterUsername("validUser")
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertDoesNotExist()
    composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE).assertIsDisplayed()
  }

  @Test
  fun buttonVisible_whenNoErrors_isEnabled() {
    composeTestRule.enterUsername("validUser")
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertDoesNotExist()
    composeTestRule
        .onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE)
        .assertIsDisplayed()
        .assertIsEnabled()
  }
}
