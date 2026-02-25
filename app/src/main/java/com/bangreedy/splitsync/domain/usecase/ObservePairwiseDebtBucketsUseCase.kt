package com.bangreedy.splitsync.domain.usecase

import com.bangreedy.splitsync.data.local.dao.ExpenseDao
import com.bangreedy.splitsync.data.local.dao.GroupDao
import com.bangreedy.splitsync.data.local.dao.PaymentDao
import com.bangreedy.splitsync.domain.model.DebtBucket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Computes pairwise debt buckets between two users across all contexts
 * (groups + direct threads), broken down by (contextType, contextId, currency).
 */
class ObservePairwiseDebtBucketsUseCase(
    private val expenseDao: ExpenseDao,
    private val paymentDao: PaymentDao,
    private val groupDao: GroupDao
) {
    operator fun invoke(myUid: String, friendUid: String): Flow<List<DebtBucket>> {
        val expensesFlow = expenseDao.observeAllExpensesWithSplits()
        val paymentsFlow = paymentDao.observeAllPayments()

        return combine(expensesFlow, paymentsFlow) { allExpenses, allPayments ->
            // Key = (contextType, contextId, currency)
            val nets = mutableMapOf<Triple<String, String, String>, Long>()

            // Process expenses - Option B pairwise rules
            for (ews in allExpenses) {
                val e = ews.expense
                val splitMap = ews.splits.associate { it.memberId to it.owedMinor }

                val payerIsMe = e.payerMemberId == myUid
                val payerIsFriend = e.payerMemberId == friendUid

                if (!payerIsMe && !payerIsFriend) continue

                val delta: Long = when {
                    payerIsMe -> splitMap[friendUid] ?: 0L       // friend owes me
                    payerIsFriend -> -(splitMap[myUid] ?: 0L)    // I owe friend
                    else -> 0L
                }

                if (delta == 0L) continue

                val key = Triple(e.contextType, e.contextId, e.currency)
                nets[key] = (nets[key] ?: 0L) + delta
            }

            // Process payments between me and friend
            for (p in allPayments) {
                val isMyPayToFriend = p.fromMemberId == myUid && p.toMemberId == friendUid
                val isFriendPayToMe = p.fromMemberId == friendUid && p.toMemberId == myUid

                if (!isMyPayToFriend && !isFriendPayToMe) continue

                val sign: Int = if (isMyPayToFriend) +1 else -1

                // Apply payment to its native currency in its own context
                val key = Triple(p.contextType, p.contextId, p.currency)
                nets[key] = (nets[key] ?: 0L) + sign * p.amountMinor
            }

            // Build debt buckets, filter out zeroed ones
            val buckets = mutableListOf<DebtBucket>()
            for ((key, net) in nets) {
                if (net == 0L) continue
                val (contextType, contextId, currency) = key
                val label = if (contextType == "DIRECT") {
                    "Direct"
                } else {
                    val groupName = groupDao.getGroupName(contextId) ?: "Group"
                    "Group: $groupName"
                }
                buckets.add(
                    DebtBucket(
                        contextType = contextType,
                        contextId = contextId,
                        currency = currency,
                        netMinor = net,
                        label = label
                    )
                )
            }

            // Sort: by currency, then by label
            buckets.sortWith(compareBy({ it.currency }, { it.label }))
            buckets
        }
    }
}

