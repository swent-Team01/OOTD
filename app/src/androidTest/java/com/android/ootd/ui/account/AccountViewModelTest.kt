import android.net.Uri
import androidx.credentials.CredentialManager
import com.android.ootd.model.authentication.AccountService
import com.android.ootd.model.user.User
import com.android.ootd.model.user.UserRepository
import com.android.ootd.ui.account.AccountViewModel
import com.google.firebase.auth.FirebaseUser
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

  private lateinit var accountService: AccountService
  private lateinit var userRepository: UserRepository
  private lateinit var credentialManager: CredentialManager
  private lateinit var viewModel: AccountViewModel
  private lateinit var mockFirebaseUser: FirebaseUser

  private val testDispatcher = StandardTestDispatcher()
  private val userFlow = MutableStateFlow<FirebaseUser?>(null)

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    accountService = mockk(relaxed = true)
    userRepository = mockk(relaxed = true)
    credentialManager = mockk(relaxed = true)
    mockFirebaseUser = mockk(relaxed = true)

    every { accountService.currentUser } returns userFlow
    every { mockFirebaseUser.uid } returns "test-uid"
    every { mockFirebaseUser.email } returns "test@example.com"
    every { mockFirebaseUser.photoUrl } returns null

    coEvery { userRepository.getUser("test-uid") } returns
        User(uid = "test-uid", username = "testuser", profilePicture = Uri.EMPTY)

    coEvery { credentialManager.clearCredentialState(any()) } just Runs
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    clearAllMocks()
  }

  @Test
  fun uiState_initializes_with_empty_values() {
    viewModel = AccountViewModel(accountService, userRepository)

    val state = viewModel.uiState.value
    assertEquals("", state.username)
    assertEquals("", state.googleAccountName)
    assertNull(state.profilePicture)
    assertNull(state.errorMsg)
    assertFalse(state.signedOut)
    assertFalse(state.isLoading)
  }

  @Test
  fun observeAuthState_updates_uiState_when_user_is_signed_in() = runTest {
    viewModel = AccountViewModel(accountService, userRepository)

    userFlow.value = mockFirebaseUser
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("testuser", state.username)
    assertEquals("test@example.com", state.googleAccountName)
    assertNull(state.errorMsg)
  }

  @Test
  fun uiState_uses_Google_photo_when_Firestore_has_no_profile_picture() = runTest {
    val googlePhotoUri = Uri.parse("https://google.com/photo.jpg")
    every { mockFirebaseUser.photoUrl } returns googlePhotoUri

    viewModel = AccountViewModel(accountService, userRepository)

    userFlow.value = mockFirebaseUser
    advanceUntilIdle()

    assertEquals(googlePhotoUri, viewModel.uiState.value.profilePicture)
  }

  @Test
  fun uiState_prefers_Firestore_profile_picture_over_Google_photo() = runTest {
    val googlePhotoUri = Uri.parse("https://google.com/photo.jpg")
    val firestorePhotoUri = Uri.parse("https://firestore.com/photo.jpg")

    every { mockFirebaseUser.photoUrl } returns googlePhotoUri
    coEvery { userRepository.getUser("test-uid") } returns
        User(uid = "test-uid", username = "testuser", profilePicture = firestorePhotoUri)

    viewModel = AccountViewModel(accountService, userRepository)

    userFlow.value = mockFirebaseUser
    advanceUntilIdle()

    assertEquals(firestorePhotoUri, viewModel.uiState.value.profilePicture)
  }

  @Test
  fun observeAuthState_handles_null_user() = runTest {
    viewModel = AccountViewModel(accountService, userRepository)

    userFlow.value = null
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("", state.username)
    assertEquals("", state.googleAccountName)
    assertNull(state.profilePicture)
  }

  @Test
  fun clearErrorMsg_clears_error_message() = runTest {
    coEvery { userRepository.getUser("test-uid") } throws Exception("Error")

    viewModel = AccountViewModel(accountService, userRepository)
    userFlow.value = mockFirebaseUser
    advanceUntilIdle()

    assertNotNull(viewModel.uiState.value.errorMsg)

    viewModel.clearErrorMsg()

    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun observeAuthState_reactively_updates_when_user_data_changes() = runTest {
    viewModel = AccountViewModel(accountService, userRepository)

    userFlow.value = mockFirebaseUser
    advanceUntilIdle()

    // Verify initial state
    assertEquals("testuser", viewModel.uiState.value.username)

    // Change the mock to return different data
    coEvery { userRepository.getUser("test-uid") } returns
        User(uid = "test-uid", username = "updateduser", profilePicture = Uri.EMPTY)

    // Re-emit the FirebaseUser to trigger observeAuthState() again
    val updatedMockUser =
        mockk<FirebaseUser> {
          every { uid } returns "test-uid"
          every { email } returns "test@example.com"
          every { photoUrl } returns null
        }
    userFlow.value = updatedMockUser
    advanceUntilIdle()

    val state = viewModel.uiState.value
    assertEquals("updateduser", state.username)
  }
}
