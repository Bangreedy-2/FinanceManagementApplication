package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.repository.DirectThreadRepository

class CreateDirectExpenseUseCase(private val repo: DirectThreadRepository) {
    suspend operator fun invoke(
        threadId: String,
        payerUid: String,
        amountMinor: Long,
        currency: String,
        note: String?,
        splitUids: List<String>
    ): String = repo.createDirectExpense(threadId, payerUid, amountMinor, currency, note, splitUids)
}

class CreateDirectPaymentUseCase(private val repo: DirectThreadRepository) {
    suspend operator fun invoke(
        threadId: String,
        fromUid: String,
        toUid: String,
        amountMinor: Long,
        currency: String
    ): String = repo.createDirectPayment(threadId, fromUid, toUid, amountMinor, currency)
}

class EnsureDirectThreadUseCase(private val repo: DirectThreadRepository) {
    suspend operator fun invoke(uidA: String, uidB: String): String =
        repo.ensureThread(uidA, uidB)
}

