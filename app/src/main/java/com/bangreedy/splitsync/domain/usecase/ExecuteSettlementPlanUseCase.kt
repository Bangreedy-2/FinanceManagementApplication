package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.domain.model.Payment
import com.bangreedy.splitsync.domain.model.SettlementPlan
import com.bangreedy.splitsync.domain.repository.DirectThreadRepository
import com.bangreedy.splitsync.domain.repository.PaymentRepository
import java.util.UUID

/**
 * Executes a settlement plan by writing payment records into the correct contexts.
 *
 * - GROUP context lines → group payment via PaymentRepository
 * - DIRECT context lines → direct thread payment via DirectThreadRepository
 *
 * All payments share the same settlementId for traceability.
 */
class ExecuteSettlementPlanUseCase(
    private val paymentRepo: PaymentRepository,
    private val directThreadRepo: DirectThreadRepository
) {
    suspend operator fun invoke(plan: SettlementPlan) {
        val now = System.currentTimeMillis()

        for (line in plan.lines) {
            val paymentId = UUID.randomUUID().toString()

            when (line.contextType) {
                "GROUP" -> {
                    val payment = Payment(
                        id = paymentId,
                        groupId = line.contextId,
                        fromMemberId = line.fromUid,
                        toMemberId = line.toUid,
                        amountMinor = line.amountMinor,
                        currency = line.currency,
                        createdAt = now,
                        updatedAt = now,
                        contextType = "GROUP",
                        contextId = line.contextId,
                        mode = "SETTLEMENT_PLAN",
                        settlementId = plan.settlementId
                    )
                    paymentRepo.createPayment(payment)
                }
                "DIRECT" -> {
                    directThreadRepo.createDirectPayment(
                        threadId = line.contextId,
                        fromUid = line.fromUid,
                        toUid = line.toUid,
                        amountMinor = line.amountMinor,
                        currency = line.currency,
                        mode = "SETTLEMENT_PLAN",
                        ratesLastUpdatedAt = plan.fxLocks.maxOfOrNull { it.fetchedAtMillis },
                        asOfDate = plan.fxLocks.firstOrNull()?.asOfDate,
                        settlementId = plan.settlementId
                    )
                }
            }
        }
    }
}


