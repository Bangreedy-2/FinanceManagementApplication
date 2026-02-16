package com.bangreedy.splitsync.presentation.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangreedy.splitsync.data.sync.SyncCoordinator
import com.bangreedy.splitsync.domain.repository.AuthRepository
import com.bangreedy.splitsync.domain.usecase.CreateGroupUseCase
import com.bangreedy.splitsync.domain.usecase.ObserveGroupsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth

class GroupsViewModel(
    private val observeGroups: ObserveGroupsUseCase,
    private val createGroup: CreateGroupUseCase,
    private val authRepository: AuthRepository,
    private val syncCoordinator: SyncCoordinator
) : ViewModel() {

    private val _state = MutableStateFlow(GroupsUiState())
    val state: StateFlow<GroupsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeGroups()
                .catch { e -> _state.update { it.copy(error = e.message ?: "Unknown error") } }
                .collect { groups -> _state.update { it.copy(groups = groups, error = null) } }
        }
    }

    fun onCreateGroup(name: String) {
        viewModelScope.launch {
            runCatching {
                createGroup(name) // writes DIRTY to Room
            }.onSuccess {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    syncCoordinator.pushNow(uid)
                }
            }
        }
    }


}
