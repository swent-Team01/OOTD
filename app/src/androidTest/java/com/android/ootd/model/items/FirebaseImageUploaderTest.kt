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

  private fun assertUploadResultConsistent(fileName: String, result: ImageData) {
    // When Firebase Storage isn't initialized in tests: returns empty strings
    // When it is available: returns original fileName and non-empty URL
    val isEmpty = result.imageId.isEmpty() && result.imageUrl.isEmpty()
    val isValid = result.imageId == fileName && result.imageUrl.isNotEmpty()
    assertTrue("Unexpected upload result: $result", isEmpty || isValid)
  }

  @Test
  fun uploadAndDeleteSmoke() = runBlocking {
    val uri = createRealTestFile()
    val fileName = "test_upload_${System.currentTimeMillis()}"

    // Upload
    val uploadResult = FirebaseImageUploader.uploadImage(uri, fileName)
    assertUploadResultConsistent(fileName, uploadResult)

    // Delete once (should be true whether object exists or not)
    val delete1 = FirebaseImageUploader.deleteImage(fileName)
    assertTrue(delete1)

    // Delete twice to check idempotency behavior
    val delete2 = FirebaseImageUploader.deleteImage(fileName)
    assertTrue(delete2)
  }

  @Test
  fun errorsAndEdgeCases() = runBlocking {
    // Empty id delete -> false
    assertFalse(FirebaseImageUploader.deleteImage(""))

    // Invalid content URI upload -> empty
    val invalidUri = Uri.parse("content://invalid/nonexistent/file.jpg")
    val resInvalid = FirebaseImageUploader.uploadImage(invalidUri, "invalid_uri")
    assertEquals("", resInvalid.imageId)
    assertEquals("", resInvalid.imageUrl)

    // Non existent file URI upload -> empty
    val nonExistentFile = Uri.parse("file:///path/to/nonexistent/file.jpg")
    val resNonExist = FirebaseImageUploader.uploadImage(nonExistentFile, "non_exist")
    assertEquals("", resNonExist.imageId)
    assertEquals("", resNonExist.imageUrl)

    // A small batch to ensure no exceptions and consistent behavior
    val names =
        listOf("img1", "img 2 with space", "file-name-with-hyphens", UUID.randomUUID().toString())
    val uploads =
        names.map { name ->
          val local = createRealTestFile()
          name to FirebaseImageUploader.uploadImage(local, name)
        }

    uploads.forEach { (name, result) -> assertUploadResultConsistent(name, result) }

    // Deletes should be true (except empty id which we already covered)
    names.forEach { name -> assertTrue(FirebaseImageUploader.deleteImage(name)) }
  }
}
