package com.bangreedy.splitsync.data.mapper

import com.bangreedy.splitsync.data.local.entity.GroupEntity
import com.bangreedy.splitsync.data.local.entity.SyncState
import com.bangreedy.splitsync.domain.model.Group

fun GroupEntity.toDomain(): Group =
    Group(
        id = id,
        name = name,
        photoUrl = photoUrl,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deleted = deleted
    )

fun Group.toEntity(syncState: Int = SyncState.DIRTY): GroupEntity =
    GroupEntity(
        id = id,
        name = name,
        photoUrl = photoUrl,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deleted = deleted,
        syncState = syncState
    )

