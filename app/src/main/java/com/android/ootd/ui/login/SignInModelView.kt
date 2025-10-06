package com.android.ootd.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.ootd.model.user.TakenUsernameException
import com.android.ootd.model.user.UserRepository
import com.android.ootd.model.user.UserRepositoryFirestore
import com.android.ootd.model.user.UserRepositoryProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class User(
    val uid: String = "",
    val username: String = "",
    val errorMsg: String? = "",
    val isLoading: Boolean = false
//    val referral: String = "" TODO if we want to implement, just an idea
)

class SignInModelView(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val repository: UserRepository = UserRepositoryProvider.repository,
): ViewModel(){
    private val _uiState = MutableStateFlow(User())
    val uiState: StateFlow<User> = _uiState.asStateFlow()

    init {
        refresh()
    }
    fun clearErrorMsg() = update { it.copy(errorMsg = null) }

    fun showLoading(v: Boolean) = setLoading(v)

    fun emitError(msg: String) = setErrorMsg(msg)

    fun setUsername(uname: String) = update { it.copy(username = uname) }

    fun refresh(){
//        val user = auth.currentUser
        clearErrorMsg()
        update {
            it.copy(isLoading = false)
        }
    }
    private fun setErrorMsg(msg: String) = update { it.copy(errorMsg = msg) }

    private fun setLoading(v: Boolean) = update { it.copy(isLoading = v) }

    fun registerUser(){
        val uname = _uiState.value.username
        //TODO handle case for age and location
        loadUser(uname)
        clearErrorMsg()
    }

    private fun loadUser(username: String){
        viewModelScope.launch {
            try {
                repository.createUser(username)
            }catch (e : Exception){
                when(e){
                    is TakenUsernameException -> {
                        Log.e("SignInViewModel", "Username taken", e)
                        setErrorMsg("This username has already been taken")
                        update { it.copy(username = "") }
                    }
                    else -> {
                        Log.e("SignInViewModel", "Error registering user", e)
                        setErrorMsg("Failed to register user: ${e.message}")
                    }
                }

            }
        }

    }

    private inline fun update(block: (User) -> User){
        _uiState.value = block(_uiState.value)
    }

}
