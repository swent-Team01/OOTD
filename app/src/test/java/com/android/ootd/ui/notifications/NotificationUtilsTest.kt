import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.android.ootd.createNotificationChannel
import io.mockk.*
import org.junit.Before
import org.junit.Test

class NotificationUtilsTest {

  private lateinit var context: Context
  private lateinit var manager: NotificationManagerCompat

  @Before
  fun setup() {
    MockKAnnotations.init(this)
    context = mockk()

    // Mock NotificationManagerCompat.from(context)
    mockkStatic(NotificationManagerCompat::class)
    manager = mockk(relaxed = true) // relaxed = no need to stub notify
    every { NotificationManagerCompat.from(context) } returns manager
  }

  @Test
  fun `createNotificationChannel creates the correct channel`() {
    // Mock context + NotificationManager
    val context = mockk<Context>()
    val notificationManager = mockk<NotificationManager>(relaxed = true)

    every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager

    // Capture the argument
    val channelSlot = slot<NotificationChannel>()

    every { notificationManager.createNotificationChannel(capture(channelSlot)) } just Runs

    // Run function
    createNotificationChannel(context)
  }
}
