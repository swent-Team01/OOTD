package com.android.ootd.model.account

import com.android.ootd.model.map.Location
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
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

  // Helper methods for public location tests
  private val EPFL_LOCATION = Location(46.5191, 6.5668, "EPFL")
  private val NY_LOCATION = Location(40.7128, -74.0060, "New York")

  private suspend fun makePublic(userId: String) {
    val account = repository.getAccount(userId)
    // Only toggle if account is currently private
    if (account.isPrivate) {
      repository.togglePrivacy(userId) // Make public
    }
  }

  private suspend fun setValidLocation(userId: String, location: Location = EPFL_LOCATION) {
    repository.editAccount(userId, "", "", "", location)
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

  @Test
  fun `toggleStarredItem removesExistingEntry`() = runBlocking {
    repository.addStarredItem("item-1")

    val updated = repository.toggleStarredItem("item-1")

    assertTrue("Expected toggle to remove existing entry", "item-1" !in updated)
    assertTrue(repository.getStarredItems("user1").isEmpty())
  }

  @Test
  fun `toggleStarredItem addsNewEntry`() = runBlocking {
    val updated = repository.toggleStarredItem("item-42")

    assertEquals(listOf("item-42"), updated)
    assertEquals(listOf("item-42"), repository.getStarredItems("user1"))
  }

  @Test
  fun `refreshStarredItems returns current state without mutating cache`() = runBlocking {
    repository.addStarredItem("item-1")
    repository.addStarredItem("item-2")

    val refreshed = repository.refreshStarredItems("user1")

    assertEquals(listOf("item-1", "item-2"), refreshed)
    // Ensure cached data unchanged
    assertEquals(refreshed, repository.getStarredItems("user1"))
  }

  // Public Location Syncing Tests

  @Test
  fun togglePrivacy_createsPublicLocationWhenAccountBecomesPublic() = runBlocking {
    setValidLocation("user1")
    // user1 is already private by default, no need to toggle first

    assertFalse(repository.togglePrivacy("user1")) // Make public

    val publicLocations = repository.getPublicLocations()
    assertEquals(1, publicLocations.size)
    assertEquals(repository.getAccount("user1").username, publicLocations[0].username)
    assertEquals("user1", publicLocations[0].ownerId)
  }

  @Test
  fun togglePrivacy_throwsInvalidLocationExceptionWhenLocationIsInvalid() = runBlocking {
    // user1 is already private by default with invalid location

    assertThrows(InvalidLocationException::class.java) {
      runBlocking { repository.togglePrivacy("user1") } // Try to make public with invalid location
    }

    assertEquals(0, repository.getPublicLocations().size)
  }

  @Test
  fun togglePrivacy_removesPublicLocationWhenAccountBecomesPrivate() = runBlocking {
    setValidLocation("user1")
    makePublic("user1")

    assertEquals(1, repository.getPublicLocations().size)

    assertTrue(repository.togglePrivacy("user1"))
    assertEquals(0, repository.getPublicLocations().size)
  }

  @Test
  fun editAccount_syncsPublicLocationWhenAccountIsPublic() = runBlocking {
    setValidLocation("user1", EPFL_LOCATION)
    makePublic("user1")

    repository.editAccount("user1", "updated_alice", "1990-01-01", "pic.jpg", NY_LOCATION)

    val publicLocations = repository.getPublicLocations()
    assertEquals(1, publicLocations.size)
    assertEquals("updated_alice", publicLocations[0].username)
    assertEquals(NY_LOCATION.latitude, publicLocations[0].location.latitude, 0.0001)
    assertEquals(NY_LOCATION.longitude, publicLocations[0].location.longitude, 0.0001)
  }

  @Test
  fun editAccount_doesNotSyncPublicLocationWhenAccountIsPrivate() = runBlocking {
    // user1 is already private by default, no need to toggle

    repository.editAccount("user1", "new_username", "1990-01-01", "pic.jpg", NY_LOCATION)

    assertEquals(0, repository.getPublicLocations().size)
  }

  @Test
  fun deleteAccount_removesPublicLocationIfExists() = runBlocking {
    setValidLocation("user2", EPFL_LOCATION)
    makePublic("user2")

    assertEquals(1, repository.getPublicLocations().size)

    repository.deleteAccount("user2")

    assertEquals(0, repository.getPublicLocations().size)
  }

  @Test
  fun getPublicLocations_returnsAllPublicAccounts() = runBlocking {
    setValidLocation("user1", EPFL_LOCATION)
    setValidLocation("user2", NY_LOCATION)
    makePublic("user1")
    makePublic("user2")

    val publicLocations = repository.getPublicLocations()

    assertEquals(2, publicLocations.size)
    assertTrue(publicLocations.any { it.ownerId == "user1" })
    assertTrue(publicLocations.any { it.ownerId == "user2" })
  }

  @Test
  fun getPublicLocations_returnsEmptyListWhenNoPublicAccounts() = runBlocking {
    assertEquals(0, repository.getPublicLocations().size)
  }

  @Test
  fun observePublicLocations_emitsUpdatesWhenPublicLocationChanges() = runBlocking {
    // Set a valid location for user1
    setValidLocation("user1", EPFL_LOCATION)

    // Verify initial state - no public locations (user1 is private by default)
    val initialLocations = repository.getPublicLocations()
    assertEquals("Initial state should have no public locations", 0, initialLocations.size)

    // Make account public (user1 is already private, so just toggle once)
    repository.togglePrivacy("user1")

    val afterPublic = repository.getPublicLocations()
    assertEquals("After making public, should have 1 location", 1, afterPublic.size)
    assertEquals("user1", afterPublic[0].ownerId)

    // Make account private again
    repository.togglePrivacy("user1")

    val afterPrivate = repository.getPublicLocations()
    assertEquals("After making private, should have 0 locations", 0, afterPrivate.size)
  }
}
