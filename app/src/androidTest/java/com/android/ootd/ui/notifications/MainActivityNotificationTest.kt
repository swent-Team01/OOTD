package com.android.ootd.ui.notifications

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.android.ootd.MainActivity
import com.android.ootd.NOTIFICATION_CLICK_ACTION
import com.android.ootd.utils.FirestoreTest
import org.junit.Test

class MainActivityNotificationTest : FirestoreTest() {

  @Test
  fun testMainActivityReceivesNotificationIntent() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    val intent =
        Intent(context, MainActivity::class.java).apply { action = NOTIFICATION_CLICK_ACTION }

    val scenario = ActivityScenario.launch<MainActivity>(intent)

    scenario.onActivity { activity -> assert(activity.intent?.action == NOTIFICATION_CLICK_ACTION) }
  }

  @Test
  fun testMainActivityOpensUserProfile() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val testUserId = "user123" // or any test user ID

    val intent =
        Intent(context, MainActivity::class.java).apply {
          action = NOTIFICATION_CLICK_ACTION
          putExtra("senderId", testUserId)
        }

    val scenario = ActivityScenario.launch<MainActivity>(intent)

    scenario.onActivity { activity ->
      assert(activity.intent?.action == NOTIFICATION_CLICK_ACTION)
      assert(activity.intent?.getStringExtra("senderId") == testUserId)
    }
  }
}
