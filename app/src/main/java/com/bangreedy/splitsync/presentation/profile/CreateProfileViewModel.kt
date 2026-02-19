package com.bangreedy.splitsync.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangreedy.splitsync.domain.usecase.CheckUsernameAvailabilityUseCase
import com.bangreedy.splitsync.domain.usecase.ClaimUsernameUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UsernameStatus { Idle, Invalid, Checking, Available, Taken, Error }

data class CreateProfileState(
    val username: String = "",
    val displayName: String = "",
    val usernameStatus: UsernameStatus = UsernameStatus.Idle,
    val isSaving: Boolean = false,
    val error: String? = null
)



class CreateProfileViewModel(
    private val claimUsername: ClaimUsernameUseCase,
    private val checkUsernameAvailability: CheckUsernameAvailabilityUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CreateProfileState())
    val state: StateFlow<CreateProfileState> = _state.asStateFlow()

    private var usernameJob: Job? = null

    fun onUsername(v: String) {
        _state.update { it.copy(username = v, error = null) }

        val trimmed = v.trim()
        val isValid = isUsernameValid(trimmed)
        if (!isValid) {
            usernameJob?.cancel()
            _state.update { it.copy(usernameStatus = if (trimmed.isBlank()) UsernameStatus.Idle else UsernameStatus.Invalid) }
            return
        }

        usernameJob?.cancel()
        usernameJob = viewModelScope.launch {
            _state.update { it.copy(usernameStatus = UsernameStatus.Checking) }
            delay(400) // debounce

            runCatching { checkUsernameAvailability(trimmed) }
                .onSuccess { available ->
                    _state.update {
                        it.copy(usernameStatus = if (available) UsernameStatus.Available else UsernameStatus.Taken)
                    }
                }
                .onFailure {
                    _state.update { it.copy(usernameStatus = UsernameStatus.Error) }
                }
        }
    }

    fun onDisplayName(v: String) =
        _state.update { it.copy(displayName = v, error = null) }

    fun save(uid: String, email: String?, onDone: () -> Unit) {
        val s = _state.value

        if (s.usernameStatus != UsernameStatus.Available) {
            _state.update { it.copy(error = "Pick an available username") }
            return
        }
        if (s.displayName.trim().isBlank()) {
            _state.update { it.copy(error = "Display name required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            runCatching {
                claimUsername(uid, s.username, s.displayName, email)
            }.onSuccess {
                _state.update { it.copy(isSaving = false) }
                onDone()
            }.onFailure { e ->
                _state.update { it.copy(isSaving = false, error = e.message ?: "Failed") }
            }
        }
    }

    private fun isUsernameValid(u: String): Boolean {
        if (u.length !in 3..20) return false
        return u.all { it.isLetterOrDigit() || it == '_' }
    }
}