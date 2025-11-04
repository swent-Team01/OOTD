package com.android.ootd.model.consent

import androidx.annotation.Keep

/**
 * Represents a user's consent agreement for the beta program.
 *
 * @property consentUuid Unique identifier for this consent record
 * @property userId Firebase user ID of the user who gave consent
 * @property timestamp Unix timestamp (in milliseconds) when consent was given
 * @property version Version of the terms and conditions agreed to (e.g., "1.0")
 */
@Keep
data class Consent(
    val consentUuid: String = "",
    val userId: String = "",
    val timestamp: Long = 0L,
    val version: String = "1.0"
)
