package com.bangreedy.splitsync.domain.repository

import com.bangreedy.splitsync.domain.model.Group
import kotlinx.coroutines.flow.Flow

interface GroupRepository {
    fun observeGroups(): Flow<List<Group>>
    suspend fun createGroup(name: String): String
}
