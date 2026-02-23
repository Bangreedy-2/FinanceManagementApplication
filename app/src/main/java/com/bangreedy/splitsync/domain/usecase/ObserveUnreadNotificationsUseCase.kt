package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.repository.NotificationRepository

class ObserveUnreadNotificationsCountUseCase(private val repo: NotificationRepository) {
    operator fun invoke(uid: String) = repo.observeUnreadCount(uid)
}