package com.bangreedy.splitsync.presentation.invites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangreedy.splitsync.domain.usecase.SendInviteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SendInviteState(
    val input: String = "",
    val isSending: Boolean = false,
    val error: String? = null
)

class SendInviteViewModel(
    private val sendInvite: SendInviteUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(SendInviteState())
    val state: StateFlow<SendInviteState> = _state.asStateFlow()

    fun onInput(v: String) = _state.update { it.copy(input = v, error = null) }

    fun send(groupId: String, groupName: String, inviterUid: String, inviterName: String?, onDone: () -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, error = null) }
            runCatching {
                sendInvite(groupId, groupName, inviterUid, inviterName, s.input)
            }.onSuccess {
                _state.update { it.copy(isSending = false, input = "") }
                onDone()
            }.onFailure { e ->
                _state.update { it.copy(isSending = false, error = e.message ?: "Failed") }
            }
        }
    }
}
