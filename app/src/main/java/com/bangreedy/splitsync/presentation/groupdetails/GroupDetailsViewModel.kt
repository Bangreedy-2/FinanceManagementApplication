package com.bangreedy.splitsync.presentation.groupdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangreedy.splitsync.data.sync.SyncCoordinator
import com.bangreedy.splitsync.domain.model.Group
import com.bangreedy.splitsync.domain.model.Member
import com.bangreedy.splitsync.domain.usecase.AddMemberUseCase
import com.bangreedy.splitsync.domain.usecase.ObserveGroupUseCase
import com.bangreedy.splitsync.domain.usecase.ObserveMembersUseCase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupDetailsUiState(
    val group: Group? = null,
    val members: List<Member> = emptyList(),
    val error: String? = null
)

class GroupDetailsViewModel(
    private val groupId: String,
    private val observeGroup: ObserveGroupUseCase,
    private val observeMembers: ObserveMembersUseCase,
    private val addMember: AddMemberUseCase,
    private val syncCoordinator: SyncCoordinator,
) : ViewModel() {

    private val _state = MutableStateFlow(GroupDetailsUiState())
    val state: StateFlow<GroupDetailsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeGroup(groupId)
                .catch { e -> _state.update { it.copy(error = e.message) } }
                .collect { g -> _state.update { it.copy(group = g) } }
        }
        viewModelScope.launch {
            observeMembers(groupId)
                .catch { e -> _state.update { it.copy(error = e.message) } }
                .collect { m -> _state.update { it.copy(members = m) } }
        }
    }

    fun onAddMember(name: String, email: String?) {
        viewModelScope.launch {
            runCatching { addMember(groupId, name, email) }
                .onSuccess {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) syncCoordinator.pushNow(uid)
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.message ?: "Failed to add member") }
                }
        }
    }

}
