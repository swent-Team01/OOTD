package com.android.ootd

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class MainActivityTest : TestCase() {

  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()
  @get:Rule val secondActivityRule = ActivityScenarioRule(SecondActivity::class.java)

  @Test
  fun test() = run {
    step("Launch MainActivity and verify Compose root exists") {
      composeTestRule.onRoot().assertExists()
    }
    step("Launch SecondActivity and verify it starts") {
      var launched = false
      secondActivityRule.scenario.onActivity { launched = true }
      assertTrue("SecondActivity did not launch", launched)
    }
  }
}
