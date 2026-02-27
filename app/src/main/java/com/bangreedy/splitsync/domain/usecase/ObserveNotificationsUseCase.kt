package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.repository.NotificationRepository

class ObserveNotificationsUseCase(private val repo: NotificationRepository) {
    operator fun invoke(uid: String) = repo.observeNotifications(uid)
}