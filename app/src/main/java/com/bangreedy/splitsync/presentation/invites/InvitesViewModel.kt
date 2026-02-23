package com.bangreedy.splitsync.presentation.invites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangreedy.splitsync.data.remote.firestore.FirestoreUserProfileDataSource
import com.bangreedy.splitsync.domain.model.Invite
import com.bangreedy.splitsync.domain.usecase.AcceptInviteUseCase
import com.bangreedy.splitsync.domain.usecase.DeclineInviteUseCase
import com.bangreedy.splitsync.domain.usecase.ObserveInvitesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InvitesUiState(
    val invites: List<Invite> = emptyList(),
    val inviterNames: Map<String, String> = emptyMap(), // inviterUid -> displayName
    val error: String? = null
)


class InvitesViewModel(
    private val myUid: String,
    private val observeInvites: ObserveInvitesUseCase,
    private val acceptInvite: AcceptInviteUseCase,
    private val declineInvite: DeclineInviteUseCase,
    private val userProfileRemote: FirestoreUserProfileDataSource
) : ViewModel() {

    private val _state = MutableStateFlow(InvitesUiState())
    val state: StateFlow<InvitesUiState> = _state.asStateFlow()

    // in-memory cache to avoid refetch
    private val requested = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            observeInvites(myUid).collect { list ->
                _state.update { it.copy(invites = list, error = null) }
                resolveInviterNames(list)
            }
        }
    }

    private fun resolveInviterNames(invites: List<Invite>) {
        val inviterUids = invites.map { it.inviterUid }.distinct()

        inviterUids.forEach { uid ->
            if (uid.isBlank()) return@forEach
            if (uid in requested) return@forEach
            requested.add(uid)

            viewModelScope.launch {
                runCatching {
                    val data = userProfileRemote.getUserProfile(uid)
                    (data?.get("displayName") as? String)?.takeIf { it.isNotBlank() }
                }.onSuccess { displayName ->
                    if (displayName != null) {
                        _state.update { st ->
                            st.copy(inviterNames = st.inviterNames + (uid to displayName))
                        }
                    }
                }
            }
        }
    }

    fun accept(invite: Invite) = viewModelScope.launch {
        runCatching {
            acceptInvite(
                myUid = myUid,
                inviteId = invite.inviteId,
                groupId = invite.groupId,
                inviterUid = invite.inviterUid,
                groupName = invite.groupName
            )
        }.onFailure { e -> _state.update { it.copy(error = e.message ?: "Failed") } }
    }

    fun decline(inviteId: String) = viewModelScope.launch {
        runCatching { declineInvite(myUid, inviteId) }
            .onFailure { e -> _state.update { it.copy(error = e.message ?: "Failed") } }
    }
}