package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.repository.GroupRepository

class CreateGroupUseCase(
    private val repo: GroupRepository
) {
    suspend operator fun invoke(name: String): String = repo.createGroup(name.trim())
}
