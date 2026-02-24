package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.model.Payment
import com.bangreedy.splitsync.domain.repository.PaymentRepository
import java.util.UUID

class CreatePaymentUseCase(
    private val repo: PaymentRepository
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

        return repo.createPayment(payment)
    }
}
