package com.bangreedy.splitsync.presentation.groups

import com.bangreedy.splitsync.domain.model.Group

data class GroupsUiState(
    val groups: List<Group> = emptyList(),
    val unreadNotifications: Int = 0,
    val error: String? = null
)