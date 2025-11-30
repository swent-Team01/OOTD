package com.android.ootd.ui.post

import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.image.ImageCompressor
import com.android.ootd.model.items.ImageData
import com.android.ootd.model.items.Item
import com.android.ootd.model.items.ItemsRepository
import com.android.ootd.model.post.OutfitPostRepository
import com.android.ootd.model.user.UserRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OutfitPreviewViewModelTest {

  private val itemsRepository: ItemsRepository = mockk(relaxed = true)
  private val postRepository: OutfitPostRepository = mockk(relaxed = true)
  private val userRepository: UserRepository = mockk(relaxed = true)
  private val accountService: AccountService = mockk(relaxed = true)
  private val imageCompressor: ImageCompressor = mockk(relaxed = true)

  @Test
  fun `removeItemFromPost updates item and state without deleting`() = runTest {
    val item =
        Item(
            itemUuid = "item-1",
            postUuids = listOf("post-1", "other"),
            image = ImageData("img", "url"),
            category = "Top",
            type = "Shirt",
            ownerId = "user")
    val viewModel =
        OutfitPreviewViewModel(
            itemsRepository = itemsRepository,
            postRepository = postRepository,
            userRepository = userRepository,
            accountService = accountService,
            imageCompressor = imageCompressor)

    val stateField = OutfitPreviewViewModel::class.java.getDeclaredField("_uiState")
    stateField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val stateFlow = stateField.get(viewModel) as MutableStateFlow<PreviewUIState>
    stateFlow.value = stateFlow.value.copy(postUuid = "post-1", items = listOf(item))

    viewModel.removeItemFromPost("item-1")

    coVerify {
      itemsRepository.editItem(
          "item-1", withArg { updated -> assertTrue(updated.postUuids.none { it == "post-1" }) })
    }
    assertTrue(viewModel.uiState.value.items.isEmpty())
  }

  @Test
  fun `removeItemFromPost surfaces errors but still updates state`() = runTest {
    val item =
        Item(
            itemUuid = "item-1",
            postUuids = listOf("post-1"),
            image = ImageData("img", "url"),
            category = "Top",
            type = "Shirt",
            ownerId = "user")
    val viewModel =
        OutfitPreviewViewModel(
            itemsRepository = itemsRepository,
            postRepository = postRepository,
            userRepository = userRepository,
            accountService = accountService,
            imageCompressor = imageCompressor)

    val stateField = OutfitPreviewViewModel::class.java.getDeclaredField("_uiState")
    stateField.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val stateFlow = stateField.get(viewModel) as MutableStateFlow<PreviewUIState>
    stateFlow.value = stateFlow.value.copy(postUuid = "post-1", items = listOf(item))

    coEvery { itemsRepository.editItem(any(), any()) } throws IllegalStateException("fail edit")

    viewModel.removeItemFromPost("item-1")
    advanceUntilIdle()

    coVerify { itemsRepository.editItem(any(), any()) }
    assertEquals(emptyList<Item>(), viewModel.uiState.value.items)
  }
}
