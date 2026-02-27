package com.bangreedy.splitsync.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangreedy.splitsync.domain.model.AppNotification
import com.bangreedy.splitsync.domain.usecase.MarkNotificationReadUseCase
import com.bangreedy.splitsync.domain.usecase.ObserveNotificationsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val items: List<AppNotification> = emptyList(),
    val error: String? = null
)

class NotificationsViewModel(
    private val uid: String,
    private val observeNotifications: ObserveNotificationsUseCase,
    private val markRead: MarkNotificationReadUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(NotificationsUiState())
    val state: StateFlow<NotificationsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeNotifications(uid).collect { items ->
                _state.update { it.copy(items = items, error = null) }
            }
        }
    }

    fun onOpen(id: String) = viewModelScope.launch {
        runCatching { markRead(uid, id) }
            .onFailure { e -> _state.update { it.copy(error = e.message ?: "Failed") } }
    }
}