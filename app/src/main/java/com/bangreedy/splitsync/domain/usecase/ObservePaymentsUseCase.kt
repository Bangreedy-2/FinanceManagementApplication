package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.model.Payment
import com.bangreedy.splitsync.domain.repository.PaymentRepository
import kotlinx.coroutines.flow.Flow

class ObservePaymentsUseCase(
    private val repo: PaymentRepository
) {
    operator fun invoke(groupId: String): Flow<List<Payment>> = repo.observePayments(groupId)
}
