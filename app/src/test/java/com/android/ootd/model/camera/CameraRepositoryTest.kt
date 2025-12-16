package com.android.ootd.model.camera

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CameraRepositoryTest {

  private val repository = CameraRepositoryImplementation()
  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Test
  fun `saveBitmap saves bitmap to cache directory`() {
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

    val uri = repository.saveBitmap(context, bitmap)

    val file = File(uri.path!!)
    assertTrue(file.exists())
    assertTrue(file.parent == context.cacheDir.path)
    assertTrue(file.name.startsWith("OOTD_"))
    assertTrue(file.name.endsWith(".jpg"))
  }
}
