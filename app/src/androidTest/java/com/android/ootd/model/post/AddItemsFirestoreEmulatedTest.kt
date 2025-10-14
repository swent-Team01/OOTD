package com.android.ootd.model.post

import android.net.Uri
import com.android.ootd.model.Item
import com.android.ootd.model.ItemsRepository
import com.android.ootd.model.Material
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

@OptIn(ExperimentalCoroutinesApi::class)
class AddItemsViewModelTest {

  @get:Rule val mainRule = MainDispatcherRule()

  private val item1 =
      Item(
          uuid = "0",
          image = Uri.parse("file://item1.jpg"),
          category = "clothes",
          type = "t-shirt",
          brand = "Mango",
          price = 0.0,
          material = listOf(),
          link = "https://example.com/item1")

  private val item2 =
      Item(
          uuid = "1",
          image = Uri.parse("file://item2.jpg"),
          category = "shoes",
          type = "high heels",
          brand = "Zara",
          price = 30.0,
          material = listOf(),
          link = "https://example.com/item2")

  private val item3 =
      Item(
          uuid = "2",
          image = Uri.parse("file://item3.jpg"),
          category = "bags",
          type = "handbag",
          brand = "Vakko",
          price = 0.0,
          material = listOf(),
          link = "https://example.com/item3")

  private val item4 =
      Item(
          uuid = "3",
          image = Uri.parse("file://item4.jpg"),
          category = "accessories",
          type = "sunglasses",
          brand = "Ray-Ban",
          price = 100.0,
          material =
              listOf(
                  Material(name = "Plastic", percentage = 80.0),
                  Material(name = "Metal", percentage = 20.0)),
          link = "https://example.com/item4")

  @Test
  fun getNewItemIdReturnsUniqueIDs() = runTest {
    val repo = FakeItemsRepository()
    val n = 100
    val ids = (0 until n).map { repo.getNewItemId() }.toSet()
    assertEquals(n, ids.size)
  }

  @Test
  fun addItemWithTheCorrectID() = runTest {
    val repo = FakeItemsRepository()
    repo.addItem(item1)

    assertEquals(1, repo.getAllItems().size)
    val stored = repo.getItemById(item1.uuid)
    assertEquals(item1, stored)
  }

  @Test
  fun canAddAndRetrieveMultipleItems() = runTest {
    val repo = FakeItemsRepository()
    repo.addItem(item1)
    repo.addItem(item2)
    repo.addItem(item3)

    val all = repo.getAllItems()
    assertEquals(3, all.size)
    assertEquals(item3, repo.getItemById(item3.uuid))
    assertEquals(item2, repo.getItemById(item2.uuid))
  }

  @Test
  fun checkUidsAreUniqueInTheCollection_lastWriteWins() = runTest {
    val repo = FakeItemsRepository()
    val uid = "duplicate"
    val a = item1.copy(uuid = uid)
    val b = item2.copy(uuid = uid)

    repo.addItem(a)
    repo.addItem(b)

    val items = repo.getAllItems()
    assertEquals(1, items.size)
    assertEquals(uid, items.first().uuid)
    assertEquals(b, items.first())
  }

  @Test
  fun deleteItemById() = runTest {
    val repo = FakeItemsRepository()
    repo.addItem(item1)
    repo.addItem(item2)
    repo.addItem(item3)

    repo.deleteItem(item2.uuid)
    val items = repo.getAllItems()
    assertEquals(2, items.size)
    assertTrue(items.contains(item1))
    assertTrue(items.contains(item3))

    // Deleting non-existing must not throw
    repo.deleteItem(item2.uuid)
    assertEquals(2, repo.getAllItems().size)
  }

  @Test
  fun editItemById() = runTest {
    val repo = FakeItemsRepository()
    repo.addItem(item4)

    val updated =
        item4.copy(
            type = "wayfarer",
            brand = "RB",
            price = 120.0,
            material = listOf(Material(name = "Carbon", percentage = 100.0)))

    repo.editItem(item4.uuid, updated)
    assertEquals(1, repo.getAllItems().size)
    assertEquals(updated.copy(uuid = item4.uuid), repo.getItemById(item4.uuid))

    // Editing an item that does not exist should throw
    val nonExisting = item2.copy(uuid = "nonExisting")
    assertThrows(Exception::class.java) { runTest { repo.editItem(nonExisting.uuid, nonExisting) } }
    assertEquals(1, repo.getAllItems().size)
  }

  @Test(expected = Exception::class)
  fun getItemByIdThrowsWhenItemNotFound() = runTest {
    val repo = FakeItemsRepository()
    repo.getItemById("missing")
  }

  // ------------ inline test helpers (same trick) ------------

  @OptIn(ExperimentalCoroutinesApi::class)
  class MainDispatcherRule(private val dispatcher: TestDispatcher = StandardTestDispatcher()) :
      TestRule {
    override fun apply(base: Statement, description: Description): Statement {
      return object : Statement() {
        override fun evaluate() {
          Dispatchers.setMain(dispatcher)
          try {
            base.evaluate()
          } finally {
            Dispatchers.resetMain()
          }
        }
      }
    }
  }

  private class FakeItemsRepository : ItemsRepository {
    private val store = linkedMapOf<String, Item>()
    private var idSeq = 0

    override fun getNewItemId(): String = "test-${idSeq++}"

    override suspend fun getAllItems(): List<Item> = store.values.toList()

    override suspend fun getItemById(uuid: String): Item =
        store[uuid] ?: throw Exception("Item $uuid not found")

    override suspend fun addItem(item: Item) {
      store[item.uuid] = item // upsert
    }

    override suspend fun editItem(itemUUID: String, newItem: Item) {
      if (!store.containsKey(itemUUID)) throw Exception("Item $itemUUID not found")
      store[itemUUID] = newItem.copy(uuid = itemUUID)
    }

    override suspend fun deleteItem(uuid: String) {
      store.remove(uuid)
    }
  }
}
