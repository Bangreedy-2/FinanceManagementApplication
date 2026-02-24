package com.bangreedy.splitsync.presentation.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangreedy.splitsync.domain.model.Friend
import com.bangreedy.splitsync.domain.usecase.AcceptFriendRequestUseCase
import com.bangreedy.splitsync.domain.usecase.DeclineFriendRequestUseCase
import com.bangreedy.splitsync.domain.usecase.ObserveFriendsUseCase
import com.bangreedy.splitsync.domain.usecase.ObservePendingFriendsUseCase
import com.bangreedy.splitsync.domain.usecase.SendFriendRequestUseCase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FriendsUiState(
    val friends: List<Friend> = emptyList(),
    val pendingIncoming: List<Friend> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class FriendsViewModel(
    private val observeFriends: ObserveFriendsUseCase,
    private val observePending: ObservePendingFriendsUseCase,
    private val sendRequest: SendFriendRequestUseCase,
    private val acceptRequest: AcceptFriendRequestUseCase,
    private val declineRequest: DeclineFriendRequestUseCase,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow(FriendsUiState())
    val state: StateFlow<FriendsUiState> = _state.asStateFlow()

    private val myUid: String? get() = auth.currentUser?.uid

    init {
        val uid = myUid
        if (uid != null) {
            viewModelScope.launch {
                observeFriends(uid).collect { friends ->
                    _state.update { it.copy(friends = friends) }
                }
            }
            viewModelScope.launch {
                observePending(uid).collect { pending ->
                    _state.update { it.copy(pendingIncoming = pending) }
                }
            }
        }
    }

    fun sendFriendRequest(input: String) {
        val uid = myUid ?: return
        viewModelScope.launch {
            _state.update { it.copy(error = null) }
            try {
                sendRequest(uid, input)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to send request") }
            }
        }
    }

    fun acceptFriend(friendUid: String) {
        val uid = myUid ?: return
        viewModelScope.launch {
            try {
                acceptRequest(uid, friendUid)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun declineFriend(friendUid: String) {
        val uid = myUid ?: return
        viewModelScope.launch {
            try {
                declineRequest(uid, friendUid)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}


