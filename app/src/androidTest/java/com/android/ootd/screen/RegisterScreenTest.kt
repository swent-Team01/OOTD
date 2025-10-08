package com.android.ootd.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
        composeTestRule.onNodeWithTag(RegisterScreenTestTags.APP_LOGO).assertIsDisplayed()
        composeTestRule.onNodeWithTag(RegisterScreenTestTags.WELCOME_TITLE).assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(RegisterScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
            .assertIsNotDisplayed()
    }

    @Test
    fun canEnterUsername() {
        val text = "user1"
        composeTestRule.enterUsername(text)

        composeTestRule.waitForIdle()

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
    fun buttonVisible_whenNoErrors() {
        composeTestRule.enterUsername("validUser")

        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule
                .onAllNodesWithTag(RegisterScreenTestTags.REGISTER_SAVE)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule
            .onNodeWithTag(RegisterScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE).assertIsDisplayed()
    }

    @Test
    fun buttonVisible_whenNoErrors_isEnabled() {
        composeTestRule.enterUsername("validUser")

        composeTestRule.waitUntil(timeoutMillis = 3000) {
            val nodes = composeTestRule
                .onAllNodesWithTag(RegisterScreenTestTags.REGISTER_SAVE)
                .fetchSemanticsNodes()
            nodes.isNotEmpty()
        }

        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(RegisterScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithTag(RegisterScreenTestTags.REGISTER_SAVE)
            .assertIsDisplayed()
            .assertIsEnabled()
    }
}
