package com.bangreedy.splitsync.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangreedy.splitsync.data.sync.SyncCoordinator
import com.bangreedy.splitsync.domain.repository.AuthState
import com.bangreedy.splitsync.domain.usecase.ObserveAuthStateUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class AppStateViewModel(
    private val observeAuthState: ObserveAuthStateUseCase,
    private val syncCoordinator: SyncCoordinator
) : ViewModel() {

    private var startedForUserId: String? = null
    private val _auth = MutableStateFlow<AuthState>(AuthState.SignedOut)
    val auth: StateFlow<AuthState> = _auth.asStateFlow()

    init {
        viewModelScope.launch {
            observeAuthState().collect { state ->
                _auth.value = state

                if (state is AuthState.SignedIn) {
                    if (startedForUserId != state.userId) {
                        startedForUserId = state.userId
                        syncCoordinator.start(state.userId)
                    }
                } else {
                    startedForUserId = null
                }
            }
        }
    }
}
