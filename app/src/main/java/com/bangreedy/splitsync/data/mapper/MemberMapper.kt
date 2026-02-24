package com.bangreedy.splitsync.data.mapper

import com.bangreedy.splitsync.data.local.entity.MemberEntity
import com.bangreedy.splitsync.domain.model.Member

fun MemberEntity.toDomain(): Member =
    Member(
        id = id,
        groupId = groupId,
        displayName = displayName,
        userId = userId,
        email = email,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deleted = deleted
    )
