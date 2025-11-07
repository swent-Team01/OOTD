package com.android.ootd.model.consent

import org.junit.Assert.*
import org.junit.Test

class ConsentTest {

  @Test
  fun `Consent creates with all parameters`() {
    // Given
    val consentUuid = "uuid-123"
    val userId = "user-456"
    val timestamp = 1234567890L
    val version = "1.0"

    // When
    val consent = Consent(consentUuid, userId, timestamp, version)

    // Then
    assertEquals(consentUuid, consent.consentUuid)
    assertEquals(userId, consent.userId)
    assertEquals(timestamp, consent.timestamp)
    assertEquals(version, consent.version)
  }

  @Test
  fun `Consent creates with default values`() {
    // When
    val consent = Consent()

    // Then
    assertEquals("", consent.consentUuid)
    assertEquals("", consent.userId)
    assertEquals(0L, consent.timestamp)
    assertEquals("1.0", consent.version)
  }

  @Test
  fun `Consent equality works correctly`() {
    // Given
    val consent1 = Consent("uuid-1", "user-1", 123L, "1.0")
    val consent2 = Consent("uuid-1", "user-1", 123L, "1.0")
    val consent3 = Consent("uuid-2", "user-1", 123L, "1.0")

    // Then
    assertEquals(consent1, consent2)
    assertNotEquals(consent1, consent3)
  }

  @Test
  fun `Consent copy works correctly`() {
    // Given
    val original = Consent("uuid-1", "user-1", 123L, "1.0")

    // When
    val copy = original.copy(userId = "user-2")

    // Then
    assertEquals("uuid-1", copy.consentUuid)
    assertEquals("user-2", copy.userId)
    assertEquals(123L, copy.timestamp)
    assertEquals("1.0", copy.version)
  }

  @Test
  fun `Consent hashCode is consistent`() {
    // Given
    val consent1 = Consent("uuid-1", "user-1", 123L, "1.0")
    val consent2 = Consent("uuid-1", "user-1", 123L, "1.0")

    // Then
    assertEquals(consent1.hashCode(), consent2.hashCode())
  }

  @Test
  fun `Consent toString contains all fields`() {
    // Given
    val consent = Consent("uuid-1", "user-1", 123L, "1.0")

    // When
    val string = consent.toString()

    // Then
    assertTrue(string.contains("uuid-1"))
    assertTrue(string.contains("user-1"))
    assertTrue(string.contains("123"))
    assertTrue(string.contains("1.0"))
  }
}
