package com.android.ootd.model.authentication

import androidx.credentials.Credential
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.Flow

/**
 * Service interface for managing user authentication and account state.
 *
 * Provides access to the current user, user ID, and methods for checking authentication status,
 * signing in with Google, and signing out.
 */
interface AccountService {
  /** Emits the current Firebase user or null if not signed in. */
  val currentUser: Flow<FirebaseUser?>

  /** The current user's unique ID, or empty if not signed in. */
  val currentUserId: String

  /** Returns true if a user is currently signed in. */
  suspend fun hasUser(): Boolean

  /** Signs in the user using a Google ID token. */
  suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser>

  /** Signs out the current user. */
  fun signOut(): Result<Unit>
}
