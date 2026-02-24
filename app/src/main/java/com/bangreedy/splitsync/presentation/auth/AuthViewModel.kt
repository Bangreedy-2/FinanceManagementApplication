package com.bangreedy.splitsync.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangreedy.splitsync.domain.usecase.SignInUseCase
import com.bangreedy.splitsync.domain.usecase.SignUpUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class AuthViewModel(
    private val signIn: SignInUseCase,
    private val signUp: SignUpUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onEmailChange(v: String) = _state.update { it.copy(email = v, error = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, error = null) }

    fun signIn() = viewModelScope.launch {
        val s = _state.value
        _state.update { it.copy(isLoading = true, error = null) }
        runCatching { signIn(s.email, s.password) }
            .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "Sign in failed") } }
            .onSuccess { _state.update { it.copy(isLoading = false) } }
    }

    fun signUp() = viewModelScope.launch {
        val s = _state.value
        _state.update { it.copy(isLoading = true, error = null) }
        runCatching { signUp(s.email, s.password) }
            .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message ?: "Sign up failed") } }
            .onSuccess { _state.update { it.copy(isLoading = false) } }
    }
}
