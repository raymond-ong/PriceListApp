package com.example.pricelistapp.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pricelistapp.data.UserRepository
import com.example.pricelistapp.data.local.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LoginMode {
    CREATE_NEW,
    SELECT_EXISTING
}

enum class UserRole(val displayName: String) {
    CLIENT("Client"),
    MASTER("Master");

    companion object {
        fun fromString(value: String?): UserRole {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true)
            } ?: CLIENT
        }
    }
}

data class LoginUiState(
    val mode: LoginMode = LoginMode.SELECT_EXISTING,
    val newUserName: String = "",
    val selectedUser: User? = null,
    val activeUsers: List<User> = emptyList(),
    val errorMessage: String? = null,
    val loggedInUser: User? = null,
    val userRole: UserRole = UserRole.CLIENT,
    val isLoading: Boolean = false
)

class LoginViewModel(private val repository: UserRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.activeUsers.collect { users ->
                val cache = repository.getLastUsernameRoleCache()
                val lastUsername = cache?.value
                val savedRole = UserRole.fromString(cache?.subvalue)

                val matchingUser = users.firstOrNull { it.userName.equals(lastUsername, ignoreCase = true) }

                _uiState.update { state ->
                    val selected = when {
                        state.selectedUser != null && users.any { it.id == state.selectedUser.id } -> state.selectedUser
                        matchingUser != null -> matchingUser
                        else -> users.firstOrNull()
                    }
                    state.copy(
                        activeUsers = users,
                        selectedUser = selected,
                        userRole = savedRole
                    )
                }
            }
        }
    }

    fun onModeChange(mode: LoginMode) {
        _uiState.update { it.copy(mode = mode, errorMessage = null) }
    }

    fun onNewUserNameChange(name: String) {
        _uiState.update { it.copy(newUserName = name, errorMessage = null) }
    }

    fun onSelectedUserChange(user: User?) {
        _uiState.update { it.copy(selectedUser = user, errorMessage = null) }
    }

    fun onRoleChange(role: UserRole) {
        _uiState.update { it.copy(userRole = role) }
        viewModelScope.launch {
            repository.saveLastLoggedInRole(role.name)
        }
    }

    fun onSubmit() {
        val currentState = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (currentState.mode) {
                LoginMode.CREATE_NEW -> {
                    val result = repository.createUser(currentState.newUserName)
                    result.fold(
                        onSuccess = { createdUser ->
                            val cache = repository.getLastUsernameRoleCache()
                            val role = UserRole.fromString(cache?.subvalue)
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    loggedInUser = createdUser,
                                    userRole = role,
                                    newUserName = ""
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = error.message ?: "Failed to create user."
                                )
                            }
                        }
                    )
                }
                LoginMode.SELECT_EXISTING -> {
                    val userToLogin = currentState.selectedUser
                    if (userToLogin == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Please select an existing user."
                            )
                        }
                    } else {
                        val loggedIn = repository.loginUser(userToLogin)
                        val cache = repository.getLastUsernameRoleCache()
                        val role = UserRole.fromString(cache?.subvalue)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                loggedInUser = loggedIn,
                                userRole = role
                            )
                        }
                    }
                }
            }
        }
    }

    fun onLogout() {
        _uiState.update {
            it.copy(
                loggedInUser = null,
                errorMessage = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

class LoginViewModelFactory(private val repository: UserRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
