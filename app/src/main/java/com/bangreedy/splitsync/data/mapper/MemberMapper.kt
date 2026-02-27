package com.bangreedy.splitsync.data.mapper

import com.bangreedy.splitsync.data.local.dao.MemberJoinedRow
import com.bangreedy.splitsync.domain.model.Member

fun MemberJoinedRow.toDomain(): Member =
    Member(
        uid = uid,
        groupId = groupId,
        displayName = displayName,
        username = username,
        email = email,
        photoUrl = photoUrl
    )
