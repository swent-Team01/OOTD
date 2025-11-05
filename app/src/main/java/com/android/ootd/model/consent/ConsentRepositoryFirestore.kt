package com.android.ootd.model.consent

import android.util.Log
import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await

const val CONSENT_COLLECTION_PATH = "consents"
const val USER_ID_FIELD = "userId"

/**
 * Data Transfer Object for Firestore serialization/deserialization.
 *
 * @Keep annotation prevents ProGuard from obfuscating this class during minification.
 */
@Keep
private data class ConsentDto(
    val consentUuid: String = "",
    val userId: String = "",
    val timestamp: Long = 0L,
    val version: String = "1.0"
)

/** Convert domain Consent to DTO */
private fun Consent.toDto(): ConsentDto {
  return ConsentDto(
      consentUuid = this.consentUuid,
      userId = this.userId,
      timestamp = this.timestamp,
      version = this.version)
}

/** Convert DTO to domain Consent */
private fun ConsentDto.toDomain(): Consent {
  return Consent(
      consentUuid = this.consentUuid,
      userId = this.userId,
      timestamp = this.timestamp,
      version = this.version)
}

class ConsentRepositoryFirestore(private val db: FirebaseFirestore) : ConsentRepository {

  /** Helper method to validate consent data */
  private fun checkConsentData(consent: Consent): Consent? {
    if (consent.consentUuid.isBlank() || consent.userId.isBlank()) {
      Log.e("ConsentRepositoryFirestore", "Invalid consent data: UUID or userId is blank")
      return null
    }
    return consent
  }

  /** Helper method to transform a Firestore document into a Consent object */
  private fun transformConsentDocument(document: DocumentSnapshot): Consent? {
    return try {
      val consentDto = document.toObject<ConsentDto>()
      if (consentDto == null) {
        Log.e(
            "ConsentRepositoryFirestore",
            "Failed to deserialize document ${document.id} to Consent object. Data: ${document.data}")
        return null
      }
      val consent = consentDto.toDomain()
      checkConsentData(consent)
    } catch (e: Exception) {
      Log.e(
          "ConsentRepositoryFirestore",
          "Error transforming document ${document.id}: ${e.message}",
          e)
      return null
    }
  }

  override fun getNewConsentId(): String {
    return db.collection(CONSENT_COLLECTION_PATH).document().id
  }

  override suspend fun addConsent(consent: Consent) {
    val validConsent = checkConsentData(consent)
    if (validConsent == null) {
      Log.e("ConsentRepositoryFirestore", "Cannot add invalid consent")
      return
    }

    try {
      db.collection(CONSENT_COLLECTION_PATH)
          .document(consent.consentUuid)
          .set(consent.toDto())
          .await()
      Log.d("ConsentRepositoryFirestore", "Consent added successfully: ${consent.consentUuid}")
    } catch (e: Exception) {
      Log.e("ConsentRepositoryFirestore", "Error adding consent: ${e.message}", e)
      throw e
    }
  }

  override suspend fun getConsentByUserId(userId: String): Consent? {
    if (userId.isBlank()) {
      Log.e("ConsentRepositoryFirestore", "Cannot query with blank userId")
      return null
    }

    return try {
      val snapshot =
          db.collection(CONSENT_COLLECTION_PATH)
              .whereEqualTo(USER_ID_FIELD, userId)
              .limit(1)
              .get()
              .await()

      if (snapshot.documents.isEmpty()) {
        Log.d("ConsentRepositoryFirestore", "No consent found for userId: $userId")
        null
      } else {
        transformConsentDocument(snapshot.documents.first())
      }
    } catch (e: Exception) {
      Log.e(
          "ConsentRepositoryFirestore",
          "Error fetching consent for userId $userId: ${e.message}",
          e)
      null
    }
  }

  override suspend fun getConsentById(consentUuid: String): Consent? {
    if (consentUuid.isBlank()) {
      Log.e("ConsentRepositoryFirestore", "Cannot query with blank consentUuid")
      return null
    }

    return try {
      val document = db.collection(CONSENT_COLLECTION_PATH).document(consentUuid).get().await()

      if (!document.exists()) {
        Log.d("ConsentRepositoryFirestore", "No consent found with UUID: $consentUuid")
        null
      } else {
        transformConsentDocument(document)
      }
    } catch (e: Exception) {
      Log.e("ConsentRepositoryFirestore", "Error fetching consent $consentUuid: ${e.message}", e)
      null
    }
  }

  override suspend fun hasUserConsented(userId: String): Boolean {
    return getConsentByUserId(userId) != null
  }

  override suspend fun deleteConsentByUserId(userId: String) {
    if (userId.isBlank()) {
      Log.e("ConsentRepositoryFirestore", "Cannot delete with blank userId")
      return
    }

    try {
      val snapshot =
          db.collection(CONSENT_COLLECTION_PATH).whereEqualTo(USER_ID_FIELD, userId).get().await()

      val batch = db.batch()
      snapshot.documents.forEach { batch.delete(it.reference) }
      batch.commit().await()

      Log.d(
          "ConsentRepositoryFirestore",
          "Deleted ${snapshot.size()} consent record(s) for userId: $userId")
    } catch (e: Exception) {
      Log.e(
          "ConsentRepositoryFirestore",
          "Error deleting consent for userId $userId: ${e.message}",
          e)
      throw e
    }
  }

  override suspend fun getAllConsents(): List<Consent> {
    return try {
      val snapshot = db.collection(CONSENT_COLLECTION_PATH).get().await()
      snapshot.documents.mapNotNull { transformConsentDocument(it) }
    } catch (e: Exception) {
      Log.e("ConsentRepositoryFirestore", "Error fetching all consents: ${e.message}", e)
      emptyList()
    }
  }
}
