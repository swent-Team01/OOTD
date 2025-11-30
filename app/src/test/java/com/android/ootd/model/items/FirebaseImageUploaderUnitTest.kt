package com.android.ootd.model.items

import android.net.Uri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import com.google.firebase.storage.ktx.storage
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.io.File
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FirebaseImageUploaderUnitTest {

  @Before
  fun setup() {
    mockkStatic(FirebaseStorage::class)
    mockkStatic("com.google.firebase.storage.ktx.StorageKt")
    mockkStatic("kotlinx.coroutines.tasks.TasksKt")
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun uploadImageSuspending_success() = runTest {
    val mockStorage = mockk<FirebaseStorage>()
    val mockRef = mockk<StorageReference>()
    val mockImageRef = mockk<StorageReference>()
    val mockUploadTask = mockk<UploadTask.TaskSnapshot>()
    val mockDownloadUrl = mockk<Uri>()

    every { Firebase.storage } returns mockStorage
    every { mockStorage.reference } returns mockRef
    every { mockRef.child(any()) } returns mockImageRef

    // Mock await for putBytes
    coEvery { mockImageRef.putBytes(any()).await() } returns mockUploadTask

    // Mock await for downloadUrl
    coEvery { mockImageRef.downloadUrl.await() } returns mockDownloadUrl
    every { mockDownloadUrl.toString() } returns "https://example.com/image.jpg"

    val result = FirebaseImageUploader.uploadImageSuspending(ByteArray(10), "test.jpg")

    assertEquals("test.jpg", result.imageId)
    assertEquals("https://example.com/image.jpg", result.imageUrl)
  }

  @Test
  fun uploadImage_offline_fallbackToContentUri() = runTest {
    // Simulate Firebase not initialized
    every { Firebase.storage } throws IllegalStateException("Not initialized")

    val localUri = Uri.parse("content://com.android.ootd.provider/images/test.jpg")

    val result = FirebaseImageUploader.uploadImage(ByteArray(10), "test.jpg", localUri)

    assertEquals("test.jpg", result.imageId)
    assertEquals(localUri.toString(), result.imageUrl)
  }

  @Test
  fun uploadImage_offline_fallbackToFileUri_exists() = runTest {
    every { Firebase.storage } throws IllegalStateException("Not initialized")

    val tempFile = File.createTempFile("test", ".jpg")
    val localUri = Uri.fromFile(tempFile)

    val result = FirebaseImageUploader.uploadImage(ByteArray(10), "test.jpg", localUri)

    assertEquals("test.jpg", result.imageId)
    assertEquals(localUri.toString(), result.imageUrl)

    tempFile.delete()
  }

  @Test
  fun uploadImage_offline_fallbackToFileUri_notExists() = runTest {
    every { Firebase.storage } throws IllegalStateException("Not initialized")

    val localUri = Uri.parse("file:///non/existent/path.jpg")

    val result = FirebaseImageUploader.uploadImage(ByteArray(10), "test.jpg", localUri)

    assertEquals("", result.imageId)
    assertEquals("", result.imageUrl)
  }
}
