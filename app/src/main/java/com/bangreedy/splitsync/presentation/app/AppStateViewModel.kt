package com.bangreedy.splitsync.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangreedy.splitsync.domain.repository.AuthState
import com.bangreedy.splitsync.domain.usecase.ObserveAuthStateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppStateViewModel(
    private val observeAuthState: ObserveAuthStateUseCase
) : ViewModel() {

    private val _auth = MutableStateFlow<AuthState>(AuthState.SignedOut)
    val auth: StateFlow<AuthState> = _auth.asStateFlow()

    init {
        viewModelScope.launch {
            observeAuthState().collect { _auth.value = it }
        }
    }
}
