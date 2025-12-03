package com.android.ootd.utils

import android.net.Uri
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ImageUploader utility with mocked Firebase Storage.
 *
 * These tests verify the logic without actual Firebase operations. For integration tests with real
 * Firebase Storage, see the androidTest version.
 */
class ImageUploaderTest {

  private lateinit var mockStorage: FirebaseStorage
  private lateinit var mockStorageRef: StorageReference
  private lateinit var mockFileRef: StorageReference
  private lateinit var mockUploadTask: UploadTask
  private lateinit var mockDownloadUrlTask: Task<Uri>

  @Before
  fun setup() {
    mockStorage = mockk(relaxed = true)
    mockStorageRef = mockk(relaxed = true)
    mockFileRef = mockk(relaxed = true)
    mockUploadTask = mockk(relaxed = true)
    mockDownloadUrlTask = mockk(relaxed = true)

    mockkStatic(Uri::class)
  }

  @After
  fun teardown() {
    unmockkAll()
  }

  // ============= UploadResult Data Class Tests =============

  @Test
  fun `uploadResult with success true and url`() {
    val result =
        ImageUploader.UploadResult(
            success = true, url = "https://firebasestorage.googleapis.com/test.jpg", error = null)

    assertTrue(result.success)
    assertEquals("https://firebasestorage.googleapis.com/test.jpg", result.url)
    assertNull(result.error)
  }

  @Test
  fun `uploadResult with success false and error`() {
    val result =
        ImageUploader.UploadResult(
            success = false, url = "content://local/uri", error = "Network error")

    assertFalse(result.success)
    assertEquals("content://local/uri", result.url)
    assertEquals("Network error", result.error)
  }

  @Test
  fun `uploadResult with default error value`() {
    val result = ImageUploader.UploadResult(success = true, url = "https://test.com/image.jpg")

    assertTrue(result.success)
    assertNull(result.error)
  }

  // ============= uploadImage() Tests =============

  @Test
  fun `uploadImage returns success with blank URI`() = runTest {
    val result =
        ImageUploader.uploadImage(
            localUri = "", storagePath = "test/path.jpg", storage = mockStorage)

    assertTrue(result.success)
    assertEquals("", result.url)
    assertNull(result.error)

    // Verify no Firebase operations were called
    verify(exactly = 0) { mockStorage.reference }
  }

  @Test
  fun `uploadImage returns success with whitespace-only URI`() = runTest {
    val result =
        ImageUploader.uploadImage(
            localUri = "   ", storagePath = "test/path.jpg", storage = mockStorage)

    assertTrue(result.success)
    assertEquals("   ", result.url)
    assertNull(result.error)
  }

  @Test
  fun `uploadImage returns original URL when already a Firebase Storage URL`() = runTest {
    val firebaseUrl = "https://firebasestorage.googleapis.com/v0/b/bucket/o/image.jpg"

    val result =
        ImageUploader.uploadImage(
            localUri = firebaseUrl, storagePath = "test/path.jpg", storage = mockStorage)

    assertTrue(result.success)
    assertEquals(firebaseUrl, result.url)
    assertNull(result.error)

    // Verify no upload was attempted
    verify(exactly = 0) { mockStorage.reference }
  }

  @Test
  fun `uploadImage with valid URI successfully uploads and returns download URL`() = runTest {
    val localUri = "content://media/external/images/media/123"
    val downloadUrl = "https://firebasestorage.googleapis.com/v0/b/bucket/o/uploaded.jpg"
    val storagePath = "test/path.jpg"

    val mockUri = mockk<Uri>()
    every { Uri.parse(localUri) } returns mockUri
    every { Uri.parse(downloadUrl) } returns mockUri

    every { mockStorage.reference } returns mockStorageRef
    every { mockStorageRef.child(storagePath) } returns mockFileRef
    every { mockFileRef.putFile(mockUri) } returns mockUploadTask

    // Mock successful upload
    every { mockUploadTask.addOnSuccessListener(any()) } answers
        {
          val listener = firstArg<OnSuccessListener<UploadTask.TaskSnapshot>>()
          listener.onSuccess(mockk())
          mockUploadTask
        }
    every { mockUploadTask.addOnFailureListener(any()) } returns mockUploadTask

    // Mock download URL retrieval
    every { mockFileRef.downloadUrl } returns mockDownloadUrlTask
    every { mockDownloadUrlTask.addOnSuccessListener(any()) } answers
        {
          val listener = firstArg<OnSuccessListener<Uri>>()
          listener.onSuccess(mockUri)
          mockDownloadUrlTask
        }
    every { mockDownloadUrlTask.addOnFailureListener(any()) } returns mockDownloadUrlTask
    every { mockUri.toString() } returns downloadUrl

    val result =
        ImageUploader.uploadImage(
            localUri = localUri, storagePath = storagePath, storage = mockStorage)

    assertTrue(result.success)
    assertEquals(downloadUrl, result.url)
    assertNull(result.error)

    verify { mockStorage.reference }
    verify { mockStorageRef.child(storagePath) }
    verify { mockFileRef.putFile(mockUri) }
  }

  @Test
  fun `uploadImage returns failure with error on upload exception`() = runTest {
    val localUri = "content://invalid/uri"
    val storagePath = "test/path.jpg"
    val errorMessage = "Upload failed"

    val mockUri = mockk<Uri>()
    every { Uri.parse(localUri) } returns mockUri

    every { mockStorage.reference } returns mockStorageRef
    every { mockStorageRef.child(storagePath) } returns mockFileRef
    every { mockFileRef.putFile(mockUri) } throws Exception(errorMessage)

    val result =
        ImageUploader.uploadImage(
            localUri = localUri, storagePath = storagePath, storage = mockStorage)

    assertFalse(result.success)
    assertEquals(localUri, result.url) // Returns original URI on failure
    assertNotNull(result.error)
    assertTrue(result.error!!.contains(errorMessage))
  }

  // ============= uploadImageBytes() Tests =============

  @Test
  fun `uploadImageBytes successfully uploads and returns download URL`() = runTest {
    val imageBytes = byteArrayOf(1, 2, 3, 4, 5)
    val downloadUrl = "https://firebasestorage.googleapis.com/v0/b/bucket/o/uploaded.jpg"
    val storagePath = "test/bytes.jpg"

    val mockUri = mockk<Uri>()
    every { mockUri.toString() } returns downloadUrl

    every { mockStorage.reference } returns mockStorageRef
    every { mockStorageRef.child(storagePath) } returns mockFileRef
    every { mockFileRef.putBytes(imageBytes) } returns mockUploadTask

    every { mockUploadTask.addOnSuccessListener(any()) } answers
        {
          val listener = firstArg<OnSuccessListener<UploadTask.TaskSnapshot>>()
          listener.onSuccess(mockk())
          mockUploadTask
        }
    every { mockUploadTask.addOnFailureListener(any()) } returns mockUploadTask

    every { mockFileRef.downloadUrl } returns mockDownloadUrlTask
    every { mockDownloadUrlTask.addOnSuccessListener(any()) } answers
        {
          val listener = firstArg<OnSuccessListener<Uri>>()
          listener.onSuccess(mockUri)
          mockDownloadUrlTask
        }
    every { mockDownloadUrlTask.addOnFailureListener(any()) } returns mockDownloadUrlTask

    val result =
        ImageUploader.uploadImageBytes(
            imageData = imageBytes, storagePath = storagePath, storage = mockStorage)

    assertTrue(result.success)
    assertEquals(downloadUrl, result.url)
    assertNull(result.error)
  }

  @Test
  fun `uploadImageBytes with empty array handles gracefully`() = runTest {
    val emptyBytes = byteArrayOf()
    val downloadUrl = "https://firebasestorage.googleapis.com/v0/b/bucket/o/empty.jpg"
    val storagePath = "test/empty.jpg"

    val mockUri = mockk<Uri>()
    every { mockUri.toString() } returns downloadUrl

    every { mockStorage.reference } returns mockStorageRef
    every { mockStorageRef.child(storagePath) } returns mockFileRef
    every { mockFileRef.putBytes(emptyBytes) } returns mockUploadTask

    every { mockUploadTask.addOnSuccessListener(any()) } answers
        {
          val listener = firstArg<OnSuccessListener<UploadTask.TaskSnapshot>>()
          listener.onSuccess(mockk())
          mockUploadTask
        }
    every { mockUploadTask.addOnFailureListener(any()) } returns mockUploadTask

    every { mockFileRef.downloadUrl } returns mockDownloadUrlTask
    every { mockDownloadUrlTask.addOnSuccessListener(any()) } answers
        {
          val listener = firstArg<OnSuccessListener<Uri>>()
          listener.onSuccess(mockUri)
          mockDownloadUrlTask
        }
    every { mockDownloadUrlTask.addOnFailureListener(any()) } returns mockDownloadUrlTask

    val result =
        ImageUploader.uploadImageBytes(
            imageData = emptyBytes, storagePath = storagePath, storage = mockStorage)

    assertTrue(result.success)
    assertEquals(downloadUrl, result.url)
  }

  @Test
  fun `uploadImageBytes returns failure on exception`() = runTest {
    val imageBytes = byteArrayOf(1, 2, 3)
    val storagePath = "test/fail.jpg"
    val errorMessage = "Network error"

    every { mockStorage.reference } returns mockStorageRef
    every { mockStorageRef.child(storagePath) } returns mockFileRef
    every { mockFileRef.putBytes(imageBytes) } throws Exception(errorMessage)

    val result =
        ImageUploader.uploadImageBytes(
            imageData = imageBytes, storagePath = storagePath, storage = mockStorage)

    assertFalse(result.success)
    assertEquals("", result.url)
    assertNotNull(result.error)
    assertTrue(result.error!!.contains(errorMessage))
  }

  // ============= uploadImageToReference() Tests =============

  @Test
  fun `uploadImageToReference with blank URI returns success`() = runTest {
    val result = ImageUploader.uploadImageToReference(localUri = "", storageRef = mockFileRef)

    assertTrue(result.success)
    assertEquals("", result.url)
    assertNull(result.error)

    verify(exactly = 0) { mockFileRef.putFile(any()) }
  }

  @Test
  fun `uploadImageToReference with Firebase URL returns original URL`() = runTest {
    val firebaseUrl = "https://firebasestorage.googleapis.com/v0/b/bucket/o/existing.jpg"

    val result =
        ImageUploader.uploadImageToReference(localUri = firebaseUrl, storageRef = mockFileRef)

    assertTrue(result.success)
    assertEquals(firebaseUrl, result.url)
    assertNull(result.error)

    verify(exactly = 0) { mockFileRef.putFile(any()) }
  }

  @Test
  fun `uploadImageToReference successfully uploads with custom reference`() = runTest {
    val localUri = "content://media/external/images/media/456"
    val downloadUrl = "https://firebasestorage.googleapis.com/v0/b/bucket/o/custom.jpg"

    val mockUri = mockk<Uri>()
    every { Uri.parse(localUri) } returns mockUri
    every { mockUri.toString() } returns downloadUrl

    every { mockFileRef.putFile(mockUri) } returns mockUploadTask

    every { mockUploadTask.addOnSuccessListener(any()) } answers
        {
          val listener = firstArg<OnSuccessListener<UploadTask.TaskSnapshot>>()
          listener.onSuccess(mockk())
          mockUploadTask
        }
    every { mockUploadTask.addOnFailureListener(any()) } returns mockUploadTask

    every { mockFileRef.downloadUrl } returns mockDownloadUrlTask
    every { mockDownloadUrlTask.addOnSuccessListener(any()) } answers
        {
          val listener = firstArg<OnSuccessListener<Uri>>()
          listener.onSuccess(mockUri)
          mockDownloadUrlTask
        }
    every { mockDownloadUrlTask.addOnFailureListener(any()) } returns mockDownloadUrlTask

    val result = ImageUploader.uploadImageToReference(localUri = localUri, storageRef = mockFileRef)

    assertTrue(result.success)
    assertEquals(downloadUrl, result.url)
    assertNull(result.error)
  }

  @Test
  fun `uploadImageToReference returns failure on exception`() = runTest {
    val localUri = "content://invalid"
    val errorMessage = "Storage error"

    val mockUri = mockk<Uri>()
    every { Uri.parse(localUri) } returns mockUri
    every { mockFileRef.putFile(mockUri) } throws Exception(errorMessage)

    val result = ImageUploader.uploadImageToReference(localUri = localUri, storageRef = mockFileRef)

    assertFalse(result.success)
    assertEquals(localUri, result.url)
    assertNotNull(result.error)
    assertTrue(result.error!!.contains(errorMessage))
  }

  // ============= deleteImage() Tests =============

  @Test
  fun `deleteImage with blank path returns true`() = runTest {
    val result = ImageUploader.deleteImage(storagePath = "", storage = mockStorage)

    assertTrue(result)
    verify(exactly = 0) { mockStorage.reference }
  }

  @Test
  fun `deleteImage successfully deletes existing image`() = runTest {
    val storagePath = "test/delete.jpg"

    every { mockStorage.reference } returns mockStorageRef
    every { mockStorageRef.child(storagePath) } returns mockFileRef

    val deleteTask = mockk<Task<Void>>()
    every { mockFileRef.delete() } returns deleteTask
    every { deleteTask.addOnSuccessListener(any()) } answers
        {
          val listener = firstArg<OnSuccessListener<Void>>()
          listener.onSuccess(null)
          deleteTask
        }
    every { deleteTask.addOnFailureListener(any()) } returns deleteTask

    val result = ImageUploader.deleteImage(storagePath = storagePath, storage = mockStorage)

    assertTrue(result)
    verify { mockFileRef.delete() }
  }

  @Test
  fun `deleteImage returns false on exception`() = runTest {
    val storagePath = "test/error.jpg"

    every { mockStorage.reference } returns mockStorageRef
    every { mockStorageRef.child(storagePath) } returns mockFileRef
    every { mockFileRef.delete() } throws Exception("Delete failed")

    val result = ImageUploader.deleteImage(storagePath = storagePath, storage = mockStorage)

    assertFalse(result)
  }
}
