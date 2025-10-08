package com.android.ootd.model.authentication

import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * A Firebase implementation of [AccountService].
 *
 * Retrieves a Google ID token via Credential Manager and authenticates the user with Firebase. Also
 * handles sign-out and credential state clearing.
 *
 * Portions adapted from Bootcamp Week 3 Solutions (source:
 * https://github.com/swent-epfl/bootcamp-25-B3-Solution.git)
 *
 * @param context Used to launch the Credential Manager UI and load string resources.
 * @param credentialManager The [CredentialManager] used to retrieve credentials.
 * @param auth The [FirebaseAuth] instance for Firebase authentication.
 * @param helper A [GoogleSignInHelper] to extract Google ID token credentials and convert them to
 *   Firebase credentials.
 */
class AccountServiceFirebase(
    private val auth: FirebaseAuth = Firebase.auth,
    private val helper: GoogleSignInHelper = DefaultGoogleSignInHelper()
) : AccountService {
  override val currentUser: Flow<FirebaseUser?>
    get() = callbackFlow {
      val listener =
          FirebaseAuth.AuthStateListener { firebaseAuth -> trySend(firebaseAuth.currentUser) }
      auth.addAuthStateListener(listener)
      awaitClose { auth.removeAuthStateListener(listener) }
    }

  override val currentUserId: String
    get() = auth.currentUser?.uid.orEmpty()

  override suspend fun hasUser(): Boolean {
    return auth.currentUser != null
  }

  override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> {
    return try {
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val idToken = helper.extractIdTokenCredential(credential.data).idToken
        val firebaseCred = helper.toFirebaseCredential(idToken)

        // Sign in with Firebase
        val user =
            auth.signInWithCredential(firebaseCred).await().user
                ?: return Result.failure(
                    IllegalStateException("Login failed : Could not retrieve user information"))

        return Result.success(user)
      } else {
        return Result.failure(
            IllegalStateException("Login failed: Credential is not of type Google ID"))
      }
    } catch (e: Exception) {
      Result.failure(
          IllegalStateException("Login failed: ${e.localizedMessage ?: "Unexpected error."}"))
    }
  }

  /**
   * override fun signOut(): Result<Unit> { return try { // Firebase sign out auth.signOut()
   *
   * Result.success(Unit) } catch (e: Exception) { Result.failure( IllegalStateException("Logout
   * failed: ${e.localizedMessage ?: "Unexpected error."}")) }
   *
   * }
   */
}
