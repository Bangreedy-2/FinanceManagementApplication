package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.model.AppNotification
import com.bangreedy.splitsync.domain.model.Payment
import com.bangreedy.splitsync.domain.repository.NotificationRepository
import com.bangreedy.splitsync.domain.repository.PaymentRepository
import java.util.UUID

class CreatePaymentUseCase(
    private val repo: PaymentRepository,
    private val notifications: NotificationRepository
) {
    suspend operator fun invoke(
        groupId: String,
        fromMemberId: String,
        toMemberId: String,
        amountMinor: Long,
        currency: String
    ): String {
        require(fromMemberId != toMemberId) { "Cannot pay yourself" }
        require(amountMinor > 0) { "Amount must be > 0" }

        val now = System.currentTimeMillis()

        val payment = Payment(
            id = UUID.randomUUID().toString(),
            groupId = groupId,
            fromMemberId = fromMemberId,
            toMemberId = toMemberId,
            amountMinor = amountMinor,
            currency = currency,
            createdAt = now,
            updatedAt = now
        )

        val id = repo.createPayment(payment)

        // Notify the receiver that a settlement was recorded
        if (toMemberId.isNotBlank()) {
            val n = AppNotification(
                id = UUID.randomUUID().toString(),
                type = "settlement_recorded",
                title = "Settlement recorded",
                body = "A settlement of ${(amountMinor / 100)}.${(amountMinor % 100).toString().padStart(2,'0')} $currency was recorded.",
                groupId = groupId,
                actorUid = fromMemberId,
                createdAt = now,
                read = false
            )
            notifications.createNotification(ownerUid = toMemberId, notification = n)
        }

        return id
    }
}
