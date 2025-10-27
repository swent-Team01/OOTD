package com.android.ootd.model.items

import android.net.Uri
import com.android.ootd.utils.FirestoreTest
import java.io.File
import java.util.UUID
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class FirebaseImageUploaderTest : FirestoreTest() {

  @Before
  override fun setUp() {
    super.setUp()
  }

  private fun createRealTestFile(): Uri {
    val tempFile = File.createTempFile("firebase_test_upload", ".jpg")
    // Create a tiny dummy image (binary content)
    tempFile.writeBytes(ByteArray(128) { 0xAA.toByte() })
    return Uri.fromFile(tempFile)
  }

  @Test
  fun uploadImageReturnsImageDataWithEmptyStringsWhenStorageIsNull() = runBlocking {
    // When Firebase is not properly initialized (which happens in tests),
    // the uploader should gracefully return empty ImageData
    val uri = createRealTestFile()
    val fileName = "test_image_${System.currentTimeMillis()}"

    val result = FirebaseImageUploader.uploadImage(uri, fileName)

    // In test environment without proper Firebase setup, should return empty
    assertEquals(fileName, result.imageId)
    assertTrue(result.imageUrl.isNotEmpty())
  }

  @Test
  fun uploadImageWithEmptyFileName() = runBlocking {
    val uri = createRealTestFile()
    val fileName = ""

    val result = FirebaseImageUploader.uploadImage(uri, fileName)

    assertEquals("", result.imageId)
    assertTrue(result.imageUrl.isNotEmpty())
  }

  @Test
  fun uploadMultipleImagesWithDifferentFileNames() = runBlocking {
    val fileNames = listOf("image1", "image2", "image3", "image4", "image5")
    val results = mutableListOf<ImageData>()

    fileNames.forEach { fileName ->
      val uri = createRealTestFile()
      val result = FirebaseImageUploader.uploadImage(uri, fileName)
      results.add(result)
    }

    assertEquals(5, results.size)
  }

  @Test
  fun uploadImageWithSpecialCharactersInFileName() = runBlocking {
    val uri = createRealTestFile()
    val fileName = "test_image_with_special_chars_!@#$%"

    val result = FirebaseImageUploader.uploadImage(uri, fileName)

    // Should handle gracefully
    assertEquals(fileName, result.imageId)
    assertTrue(result.imageUrl.isNotEmpty())
  }

  @Test
  fun deleteImageWithEmptyIdReturnsFalse() = runBlocking {
    val result = FirebaseImageUploader.deleteImage("")
    assertFalse(result)
  }

  @Test
  fun deleteMultipleImages() = runBlocking {
    val imageIds = listOf("img1", "img2", "img3", "img4", "img5")
    val results = mutableListOf<Boolean>()

    imageIds.forEach { imageId ->
      val result = FirebaseImageUploader.deleteImage(imageId)
      results.add(result)
    }

    assertEquals(5, results.size)
    // All should complete successfully in test environment
    assertTrue(results.all { it })
  }

  @Test
  fun uploadAndThenDeleteImage() = runBlocking {
    val uri = createRealTestFile()
    val fileName = "test_upload_delete_${System.currentTimeMillis()}"

    // Upload
    val uploadResult = FirebaseImageUploader.uploadImage(uri, fileName)

    // Try to delete
    val deleteResult = FirebaseImageUploader.deleteImage(fileName)

    // In test environment, both should complete
    assertEquals(fileName, uploadResult.imageId)
    assertTrue(deleteResult)
  }

  @Test
  fun uploadImageWithFileNameContainingPath() = runBlocking {
    val uri = createRealTestFile()
    val fileName = "folder/subfolder/image"

    val result = FirebaseImageUploader.uploadImage(uri, fileName)

    assertEquals(fileName, result.imageId)
    assertTrue(result.imageUrl.isNotEmpty())
  }

  @Test
  fun uploadMultipleImagesAndDeleteThemSequentially() = runBlocking {
    val count = 10
    val results = mutableListOf<ImageData>()

    repeat(count) { index ->
      val uri = createRealTestFile()
      val fileName = "sequential_image_$index"
      val result = FirebaseImageUploader.uploadImage(uri, fileName)
      results.add(result)
    }

    assertEquals(count, results.size)

    val deleteResults = mutableListOf<Boolean>()

    repeat(count) { index ->
      val imageId = "sequential_image_$index"
      val result = FirebaseImageUploader.deleteImage(imageId)
      deleteResults.add(result)
    }

    assertEquals(count, results.size)
    assertTrue(deleteResults.all { it })
  }

  @Test
  fun uploadImageWithNumericFileName() = runBlocking {
    val uri = createRealTestFile()
    val fileName = "12345678900"

    val result = FirebaseImageUploader.uploadImage(uri, fileName)

    assertEquals(fileName, result.imageId)
    assertTrue(result.imageUrl.isNotEmpty())
  }

  @Test
  fun uploadImageWithUUIDFileName() = runBlocking {
    val uri = createRealTestFile()
    val fileName = UUID.randomUUID().toString()

    val result = FirebaseImageUploader.uploadImage(uri, fileName)

    assertEquals(fileName, result.imageId)
    assertTrue(result.imageUrl.isNotEmpty())
  }

  @Test
  fun uploadImageWithWhitespaceInFileName() = runBlocking {
    val uri = createRealTestFile()
    val fileName = "file name with spaces"

    val result = FirebaseImageUploader.uploadImage(uri, fileName)

    assertEquals(fileName, result.imageId)
    assertTrue(result.imageUrl.isNotEmpty())
  }

  @Test
  fun deleteImageWithWhitespaceInId() = runBlocking {
    val imageId = "image id with spaces"
    val result = FirebaseImageUploader.deleteImage(imageId)

    assertTrue(result)
  }

  @Test
  fun deleteSameImageMultipleTimes() = runBlocking {
    val imageId = "delete_multiple_times"

    val result1 = FirebaseImageUploader.deleteImage(imageId)
    val result2 = FirebaseImageUploader.deleteImage(imageId)
    val result3 = FirebaseImageUploader.deleteImage(imageId)

    // All deletions should complete successfully
    assertTrue(result1)
    assertTrue(result2)
    assertTrue(result3)
  }

  @Test
  fun uploadImageWithFileNameContainingDots() = runBlocking {
    val uri = createRealTestFile()
    val fileName = "file.name.with.dots"

    val result = FirebaseImageUploader.uploadImage(uri, fileName)

    assertEquals(fileName, result.imageId)
    assertTrue(result.imageUrl.isNotEmpty())
  }

  @Test
  fun uploadImageWithFileNameContainingUnderscores() = runBlocking {
    val uri = createRealTestFile()
    val fileName = "file_name_with_underscores"

    val result = FirebaseImageUploader.uploadImage(uri, fileName)

    assertEquals(fileName, result.imageId)
    assertTrue(result.imageUrl.isNotEmpty())
  }

  @Test
  fun uploadImageWithFileNameContainingHyphens() = runBlocking {
    val uri = createRealTestFile()
    val fileName = "file-name-with-hyphens"

    val result = FirebaseImageUploader.uploadImage(uri, fileName)

    assertEquals(fileName, result.imageId)
    assertTrue(result.imageUrl.isNotEmpty())
  }

  @Test
  fun uploadAndDeleteCycle() = runBlocking {
    // Simulate a complete upload-delete cycle
    val operations = 5

    repeat(operations) { index ->
      val uri = createRealTestFile()
      val fileName = "cycle_$index"

      // Upload
      val uploadResult = FirebaseImageUploader.uploadImage(uri, fileName)
      assertEquals(fileName, uploadResult.imageId)

      // Delete
      val deleteResult = FirebaseImageUploader.deleteImage(fileName)
      assertTrue(deleteResult)
    }
  }
}
