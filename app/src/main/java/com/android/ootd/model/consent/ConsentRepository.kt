package com.android.ootd.model.consent

/** Repository interface for managing user consent records in Firebase. */
interface ConsentRepository {

  /**
   * Generates a new unique ID for a consent record.
   *
   * @return A new unique consent UUID
   */
  fun getNewConsentId(): String

  /**
   * Records a new consent agreement in Firestore.
   *
   * @param consent The Consent object to save
   */
  suspend fun addConsent(consent: Consent)

  /**
   * Retrieves the consent record for a specific user.
   *
   * @param userId The Firebase user ID
   * @return The user's Consent record, or null if not found
   */
  suspend fun getConsentByUserId(userId: String): Consent?

  /**
   * Retrieves a consent record by its UUID.
   *
   * @param consentUuid The unique identifier of the consent record
   * @return The Consent record, or null if not found
   */
  suspend fun getConsentById(consentUuid: String): Consent?

  /**
   * Checks if a user has given consent.
   *
   * @param userId The Firebase user ID
   * @return True if the user has consented, false otherwise
   */
  suspend fun hasUserConsented(userId: String): Boolean

  /**
   * Deletes a user's consent record. Useful for testing or if a user wants to withdraw consent.
   *
   * @param userId The Firebase user ID
   */
  suspend fun deleteConsentByUserId(userId: String)

  /**
   * Retrieves all consent records (admin/testing purposes).
   *
   * @return List of all Consent records
   */
  suspend fun getAllConsents(): List<Consent>
}
