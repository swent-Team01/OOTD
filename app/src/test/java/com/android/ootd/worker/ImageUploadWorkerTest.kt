package com.android.ootd.worker

import android.content.ContentResolver
import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.android.ootd.model.items.FirebaseImageUploader
import com.android.ootd.model.items.ImageData
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import java.io.ByteArrayInputStream
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImageUploadWorkerTest {

  private lateinit var context: Context
  private lateinit var workerParams: WorkerParameters

  @Before
  fun setup() {
    context = mockk(relaxed = true)
    workerParams = mockk(relaxed = true)

    mockkObject(FirebaseImageUploader)
    mockkStatic(FirebaseFirestore::class)
    mockkStatic("kotlinx.coroutines.tasks.TasksKt")
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun doWork_success() = runTest {
    val itemUuid = "item-123"
    val imageUriString = "content://media/external/images/media/1"
    val fileName = "image.jpg"

    every { workerParams.inputData } returns
        workDataOf("itemUuid" to itemUuid, "imageUri" to imageUriString, "fileName" to fileName)

    val contentResolver = mockk<ContentResolver>()
    every { context.contentResolver } returns contentResolver
    every { context.applicationContext } returns context
    every { contentResolver.openInputStream(any()) } returns ByteArrayInputStream(ByteArray(10))

    coEvery { FirebaseImageUploader.uploadImageSuspending(any(), any()) } returns
        ImageData(fileName, "https://url.com")

    val mockFirestore = mockk<FirebaseFirestore>()
    val mockCollection = mockk<CollectionReference>()
    val mockDoc = mockk<DocumentReference>()
    val mockTask = mockk<Task<Void>>()

    every { FirebaseFirestore.getInstance() } returns mockFirestore
    every { mockFirestore.collection(any()) } returns mockCollection
    every { mockCollection.document(any()) } returns mockDoc
    every { mockDoc.update(any<String>(), any()) } returns mockTask
    coEvery { mockTask.await() } returns mockk()

    val worker = ImageUploadWorker(context, workerParams)
    val result = worker.doWork()

    assertEquals(ListenableWorker.Result.success(), result)

    verify { mockDoc.update("image", ImageData(fileName, "https://url.com")) }
  }

  @Test
  fun doWork_failure_missingInput() = runTest {
    every { workerParams.inputData } returns workDataOf()

    val worker = ImageUploadWorker(context, workerParams)
    val result = worker.doWork()

    assertEquals(ListenableWorker.Result.failure(), result)
  }

  @Test
  fun doWork_retry_onException() = runTest {
    val itemUuid = "item-123"
    val imageUriString = "content://media/external/images/media/1"
    val fileName = "image.jpg"

    every { workerParams.inputData } returns
        workDataOf("itemUuid" to itemUuid, "imageUri" to imageUriString, "fileName" to fileName)

    val contentResolver = mockk<ContentResolver>()
    every { context.contentResolver } returns contentResolver
    every { context.applicationContext } returns context
    every { contentResolver.openInputStream(any()) } returns ByteArrayInputStream(ByteArray(10))

    coEvery { FirebaseImageUploader.uploadImageSuspending(any(), any()) } throws
        RuntimeException("Upload failed")

    every { workerParams.runAttemptCount } returns 0

    val worker = ImageUploadWorker(context, workerParams)
    val result = worker.doWork()

    assertEquals(ListenableWorker.Result.retry(), result)
  }
}
