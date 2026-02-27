package com.bangreedy.splitsync.data.mapper

import com.bangreedy.splitsync.domain.model.NotificationPrefs
import com.bangreedy.splitsync.data.local.entity.UserProfileEntity
import com.bangreedy.splitsync.domain.model.UserProfile

fun UserProfileEntity.toDomain(): UserProfile =
    UserProfile(
        uid = uid,
        username = username,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl,
        defaultCurrency = defaultCurrency,
        notificationPrefs = NotificationPrefs(
            pushEnabled = notifyPush,
            emailEnabled = notifyEmail,
            invitePush = notifyInvitePush,
            inviteEmail = notifyInviteEmail,
            settlementPush = notifySettlementPush,
            settlementEmail = notifySettlementEmail
        )
    )
