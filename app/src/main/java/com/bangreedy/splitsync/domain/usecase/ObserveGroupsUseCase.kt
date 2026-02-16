package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.model.Group
import com.bangreedy.splitsync.domain.repository.GroupRepository
import kotlinx.coroutines.flow.Flow

class ObserveGroupsUseCase(
    private val repo: GroupRepository
) {
    operator fun invoke(): Flow<List<Group>> = repo.observeGroups()
}
