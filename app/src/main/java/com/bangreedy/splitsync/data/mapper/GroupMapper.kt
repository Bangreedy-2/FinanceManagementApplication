package com.bangreedy.splitsync.data.mapper

import com.bangreedy.splitsync.data.local.entity.GroupEntity
import com.bangreedy.splitsync.domain.model.Group

fun GroupEntity.toDomain(): Group =
    Group(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deleted = deleted
    )
