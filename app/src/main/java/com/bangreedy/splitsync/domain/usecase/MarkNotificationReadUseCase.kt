package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.repository.NotificationRepository

class MarkNotificationReadUseCase(private val repo: NotificationRepository) {
    suspend operator fun invoke(uid: String, id: String) = repo.markRead(uid, id)
}