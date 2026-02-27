package com.bangreedy.splitsync.presentation.groupdetails

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangreedy.splitsync.domain.model.Group
import com.bangreedy.splitsync.domain.model.Member
import com.bangreedy.splitsync.domain.repository.GroupRepository
import com.bangreedy.splitsync.domain.usecase.ObserveGroupUseCase
import com.bangreedy.splitsync.domain.usecase.ObserveMembersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupDetailsViewModel(
    private val groupId: String,
    private val observeGroup: ObserveGroupUseCase,
    private val observeMembers: ObserveMembersUseCase,
    private val repo: GroupRepository // Injected
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
                .catch { /* handle error */ }
                .collect { members ->
                    _state.update { it.copy(members = members) }
                }
        }
    }

    fun updateGroupPhoto(uri: Uri) {
        val currentGroup = _state.value.group ?: return
        viewModelScope.launch {
            try {
                // optional: set loading state
                val url = repo.uploadGroupPhoto(groupId, uri)
                val updatedGroup = currentGroup.copy(photoUrl = url)
                repo.updateGroup(updatedGroup)
            } catch (e: Exception) {
                // handle error
                e.printStackTrace()
            }
        }
    }
}

data class GroupDetailsUiState(
    val group: Group? = null,
    val members: List<Member> = emptyList(),
    val error: String? = null
)
