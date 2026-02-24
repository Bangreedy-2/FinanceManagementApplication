package com.bangreedy.splitsync.presentation.groups

import com.bangreedy.splitsync.domain.model.Group

data class GroupsUiState(
    val groups: List<Group> = emptyList(),
    val error: String? = null
)
