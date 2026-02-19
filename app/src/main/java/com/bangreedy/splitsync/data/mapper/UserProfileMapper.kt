package com.bangreedy.splitsync.data.mapper

import com.bangreedy.splitsync.data.local.entity.UserProfileEntity
import com.bangreedy.splitsync.domain.model.UserProfile

fun UserProfileEntity.toDomain(): UserProfile =
    UserProfile(
        uid = uid,
        username = username,
        displayName = displayName,
        email = email
    )
