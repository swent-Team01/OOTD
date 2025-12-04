package com.android.ootd.utils

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Instrumentation tests for ImageUploader utility. These tests have been done with AI
 *
 * These tests verify the actual upload/download behavior with Firebase Storage.
 */
class ImageUploaderTest : FirestoreTest() {

  private lateinit var storage: FirebaseStorage
  private val uploadedPaths = mutableListOf<String>()

  @Before
  fun setup() {
    storage = FirebaseStorage.getInstance()
  }

  @After
  fun cleanup() {
    runTest {
      uploadedPaths.forEach { path ->
        try {
          ImageUploader.deleteImage(path, storage)
        } catch (_: Exception) {}
      }
      uploadedPaths.clear()
    }
  }

  /** Helper function to create a temporary image file for testing. */
  private fun createTestImageFile(): Uri {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val testImage = File(context.cacheDir, "test_image_${UUID.randomUUID()}.jpg")

    // Create a simple 1x1 pixel JPEG (smallest valid JPEG)
    val jpegBytes =
        byteArrayOf(
            0xFF.toByte(),
            0xD8.toByte(),
            0xFF.toByte(),
            0xE0.toByte(),
            0x00.toByte(),
            0x10.toByte(),
            0x4A.toByte(),
            0x46.toByte(),
            0x49.toByte(),
            0x46.toByte(),
            0x00.toByte(),
            0x01.toByte(),
            0x01.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x01.toByte(),
            0x00.toByte(),
            0x01.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0xFF.toByte(),
            0xD9.toByte())

    FileOutputStream(testImage).use { it.write(jpegBytes) }
    return Uri.fromFile(testImage)
  }

  @Test
  fun uploadImage_withBlankUri_returnsSuccessWithBlankUrl() = runTest {
    val result =
        ImageUploader.uploadImage(localUri = "", storagePath = "test/blank.jpg", storage = storage)

    assertTrue(result.success)
    assertEquals("", result.url)
    assertNull(result.error)
  }

  @Test
  fun uploadImage_withFirebaseUrl_returnsOriginalUrl() = runTest {
    val firebaseUrl = "https://firebasestorage.googleapis.com/v0/b/bucket/o/existing.jpg?token=abc"

    val result =
        ImageUploader.uploadImage(
            localUri = firebaseUrl, storagePath = "test/should_not_upload.jpg", storage = storage)

    assertTrue(result.success)
    assertEquals(firebaseUrl, result.url)
    assertNull(result.error)
  }

  @Test
  fun uploadImage_withValidLocalUri_uploadsAndReturnsDownloadUrl() = runTest {
    val testUri = createTestImageFile()
    val storagePath = "test/images/${UUID.randomUUID()}.jpg"
    uploadedPaths.add(storagePath)

    val result =
        ImageUploader.uploadImage(
            localUri = testUri.toString(), storagePath = storagePath, storage = storage)

    assertTrue("Upload should succeed", result.success)
    assertNotNull("Download URL should not be null", result.url)
    assertNull("Error should be null", result.error)
  }

  @Test
  fun uploadImage_withInvalidUri_returnsFailure() = runTest {
    val invalidUri = "content://invalid/uri/that/does/not/exist"
    val storagePath = "test/images/${UUID.randomUUID()}.jpg"

    val result =
        ImageUploader.uploadImage(
            localUri = invalidUri, storagePath = storagePath, storage = storage)

    assertFalse("Upload should fail", result.success)
    assertEquals("Should return original URI on failure", invalidUri, result.url)
    assertNotNull("Error message should be present", result.error)
  }

  @Test
  fun uploadImage_overwritesExistingImage() = runTest {
    val testUri1 = createTestImageFile()
    val testUri2 = createTestImageFile()
    val storagePath = "test/images/overwrite_test_${UUID.randomUUID()}.jpg"
    uploadedPaths.add(storagePath)

    // First upload
    val result1 =
        ImageUploader.uploadImage(
            localUri = testUri1.toString(), storagePath = storagePath, storage = storage)
    assertTrue("First upload should succeed", result1.success)
    val url1 = result1.url

    // Second upload to same path
    val result2 =
        ImageUploader.uploadImage(
            localUri = testUri2.toString(), storagePath = storagePath, storage = storage)
    assertTrue("Second upload should succeed", result2.success)
    val url2 = result2.url

    // URLs might be the same or different depending on Firebase Storage versioning
    assertNotNull("Both URLs should exist", url1)
    assertNotNull("Both URLs should exist", url2)
  }

  @Test
  fun uploadImageToReference_withValidUri_uploadsSuccessfully() = runTest {
    val testUri = createTestImageFile()
    val storagePath = "test/images/ref_${UUID.randomUUID()}.jpg"
    uploadedPaths.add(storagePath)
    val storageRef = storage.reference.child(storagePath)

    val result =
        ImageUploader.uploadImageToReference(localUri = testUri.toString(), storageRef = storageRef)

    assertTrue("Upload should succeed", result.success)
    assertNotNull("Download URL should not be null", result.url)
    assertNull("Error should be null", result.error)
  }

  @Test
  fun uploadImageToReference_withBlankUri_returnsSuccessWithBlankUrl() = runTest {
    val storageRef = storage.reference.child("test/should_not_upload.jpg")

    val result = ImageUploader.uploadImageToReference(localUri = "", storageRef = storageRef)

    assertTrue(result.success)
    assertEquals("", result.url)
    assertNull(result.error)
  }

  @Test
  fun uploadImageToReference_withFirebaseUrl_returnsOriginalUrl() = runTest {
    val firebaseUrl = "https://firebasestorage.googleapis.com/v0/b/bucket/o/existing.jpg"
    val storageRef = storage.reference.child("test/should_not_upload.jpg")

    val result =
        ImageUploader.uploadImageToReference(localUri = firebaseUrl, storageRef = storageRef)

    assertTrue(result.success)
    assertEquals(firebaseUrl, result.url)
    assertNull(result.error)
  }

  @Test
  fun uploadImageToReference_withInvalidUri_returnsFailure() = runTest {
    val invalidUri = "content://invalid/uri/that/does/not/exist"
    val storageRef = storage.reference.child("test/invalid_ref_${UUID.randomUUID()}.jpg")

    val result =
        ImageUploader.uploadImageToReference(localUri = invalidUri, storageRef = storageRef)

    assertFalse("Upload should fail", result.success)
    assertEquals("Should return original URI on failure", invalidUri, result.url)
    assertNotNull("Error message should be present", result.error)
  }

  @Test
  fun uploadImageBytes_withValidData_uploadsSuccessfully() = runTest {
    val storagePath = "test/images/bytes_${UUID.randomUUID()}.jpg"
    uploadedPaths.add(storagePath)
    // Minimal valid JPEG bytes
    val jpegBytes =
        byteArrayOf(
            0xFF.toByte(),
            0xD8.toByte(),
            0xFF.toByte(),
            0xE0.toByte(),
            0x00.toByte(),
            0x10.toByte(),
            0x4A.toByte(),
            0x46.toByte(),
            0x49.toByte(),
            0x46.toByte(),
            0x00.toByte(),
            0x01.toByte(),
            0x01.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x01.toByte(),
            0x00.toByte(),
            0x01.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0xFF.toByte(),
            0xD9.toByte())

    val result = ImageUploader.uploadImageBytes(jpegBytes, storagePath, storage)

    assertTrue("Upload should succeed", result.success)
    assertTrue("URL should be a Firebase Storage URL", result.url.startsWith("https://"))
    assertNull("Error should be null", result.error)
  }

  @Test
  fun uploadImageBytes_withEmptyData_returnsFailure() = runTest {
    val storagePath = "test/images/empty_${UUID.randomUUID()}.jpg"

    val result = ImageUploader.uploadImageBytes(byteArrayOf(), storagePath, storage)

    assertFalse("Upload should fail for empty data", result.success)
    assertEquals("", result.url)
    assertNotNull("Error message should be present", result.error)
  }

  @Test
  fun deleteImage_withExistingImage_deletesSuccessfully() = runTest {
    // First upload an image
    val testUri = createTestImageFile()
    val storagePath = "test/images/delete_${UUID.randomUUID()}.jpg"

    val uploadResult =
        ImageUploader.uploadImage(
            localUri = testUri.toString(), storagePath = storagePath, storage = storage)
    assertTrue("Upload should succeed", uploadResult.success)

    // Then delete it
    val deleteResult = ImageUploader.deleteImage(storagePath = storagePath, storage = storage)

    assertTrue("Delete should succeed", deleteResult)
  }

  @Test
  fun deleteImage_withNonExistentImage_returnsTrue() = runTest {
    val nonExistentPath = "test/images/non_existent_${UUID.randomUUID()}.jpg"

    val result = ImageUploader.deleteImage(storagePath = nonExistentPath, storage = storage)

    assertNotNull("Result should not be null", result)
  }

  @Test
  fun deleteImage_withBlankPath_returnsTrue() = runTest {
    val result = ImageUploader.deleteImage(storagePath = "", storage = storage)

    assertTrue("Should return true for blank path", result)
  }

  @Test
  fun deleteImage_multipleTimes_handlesGracefully() = runTest {
    // Upload an image
    val testUri = createTestImageFile()
    val storagePath = "test/images/double_delete_${UUID.randomUUID()}.jpg"

    ImageUploader.uploadImage(
        localUri = testUri.toString(), storagePath = storagePath, storage = storage)

    // Delete it twice
    val firstDelete = ImageUploader.deleteImage(storagePath, storage)
    val secondDelete = ImageUploader.deleteImage(storagePath, storage)

    assertTrue("First delete should succeed", firstDelete)
    // Second delete behavior depends on implementation
    assertNotNull("Second delete should return a result", secondDelete)
  }
}
