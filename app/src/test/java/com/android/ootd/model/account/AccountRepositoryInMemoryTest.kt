package com.android.ootd.model.account

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AccountRepositoryInMemoryTest {

  private lateinit var repository: AccountRepositoryInMemory

  @Before
  fun setup() {
    repository = AccountRepositoryInMemory()
    repository.currentUser = "user1"
  }

  @Test
  fun `getStarredItems returns items added for user`() = runBlocking {
    repository.addStarredItem("item-1")
    repository.addStarredItem("item-2")

    val starred = repository.getStarredItems("user1")

    assertEquals(listOf("item-1", "item-2"), starred)
  }

  @Test
  fun `addStarredItem is idempotent`() = runBlocking {
    repository.addStarredItem("item-1")
    repository.addStarredItem("item-1")

    val starred = repository.getStarredItems("user1")

    assertEquals(listOf("item-1"), starred)
  }

  @Test
  fun `removeStarredItem updates list and ignores missing entries`() = runBlocking {
    repository.addStarredItem("item-1")
    repository.addStarredItem("item-2")

    assertTrue(repository.removeStarredItem("item-1"))
    // Removing again should still succeed and keep remaining items intact
    assertTrue(repository.removeStarredItem("item-1"))

    val starred = repository.getStarredItems("user1")

    assertEquals(listOf("item-2"), starred)
  }
}
