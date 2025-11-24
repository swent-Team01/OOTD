package com.android.ootd.ui.notifications

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.android.ootd.MainActivity
import com.android.ootd.NOTIFICATION_CLICK_ACTION
import org.junit.Test

class MainActivityNotificationTest {

  @Test
  fun testMainActivityReceivesNotificationIntent() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    val intent =
        Intent(context, MainActivity::class.java).apply { action = NOTIFICATION_CLICK_ACTION }

    val scenario = ActivityScenario.launch<MainActivity>(intent)

    scenario.onActivity { activity -> assert(activity.intent?.action == NOTIFICATION_CLICK_ACTION) }
  }
}
