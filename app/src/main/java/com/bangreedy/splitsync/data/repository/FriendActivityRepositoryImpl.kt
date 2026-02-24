package com.bangreedy.splitsync.data.repository

import com.bangreedy.splitsync.data.local.dao.ExpenseDao
import com.bangreedy.splitsync.data.local.dao.GroupDao
import com.bangreedy.splitsync.data.local.dao.PaymentDao
import com.bangreedy.splitsync.data.local.dao.UserProfileDao
import com.bangreedy.splitsync.data.local.dao.ExpenseWithSplits
import com.bangreedy.splitsync.data.local.entity.PaymentEntity
import com.bangreedy.splitsync.domain.model.ActivitySource
import com.bangreedy.splitsync.domain.model.FriendActivity
import com.bangreedy.splitsync.domain.model.FriendActivityItem
import com.bangreedy.splitsync.domain.repository.FriendActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class FriendActivityRepositoryImpl(
    private val expenseDao: ExpenseDao,
    private val paymentDao: PaymentDao,
    private val groupDao: GroupDao,
    private val userProfileDao: UserProfileDao
) : FriendActivityRepository {

    override fun observeFriendActivity(myUid: String, friendUid: String): Flow<FriendActivity> {
        val expensesFlow = expenseDao.observeAllExpensesWithSplits()
        val paymentsFlow = paymentDao.observeAllPayments()

        return combine(expensesFlow, paymentsFlow) { allExpenses, allPayments ->
            buildFriendActivity(myUid, friendUid, allExpenses, allPayments)
        }
    }

    /**
     * Option B: pairwise extraction from multi-person expenses.
     *
     * For each expense, compute pairwise delta (A's perspective):
     *   - Payer is A: delta = +owedB (B owes A)
     *   - Payer is B: delta = -owedA (A owes B)
     *   - Payer is neither: delta = 0 (exclude from timeline)
     *
     * For payments between A and B:
     *   - from A to B: delta = +amountMinor (A reducing debt to B)
     *   - from B to A: delta = -amountMinor (B reducing debt to A)
     */
    private suspend fun buildFriendActivity(
        myUid: String,
        friendUid: String,
        allExpenses: List<ExpenseWithSplits>,
        allPayments: List<PaymentEntity>
    ): FriendActivity {
        val items = mutableListOf<FriendActivityItem>()
        val netByCurrency = mutableMapOf<String, Long>()

        // Cache for group names and profile names
        val groupNameCache = mutableMapOf<String, String>()
        val nameCache = mutableMapOf<String, String>()

        suspend fun resolveName(uid: String): String {
            return nameCache.getOrPut(uid) {
                userProfileDao.getByUid(uid)?.displayName ?: uid.take(8)
            }
        }

        suspend fun resolveSource(contextType: String, contextId: String): ActivitySource {
            return if (contextType == "DIRECT") {
                ActivitySource.Direct
            } else {
                val name = groupNameCache.getOrPut(contextId) {
                    groupDao.getGroupName(contextId) ?: "Group"
                }
                ActivitySource.Group(groupId = contextId, groupName = name)
            }
        }

        // Process expenses
        for (ews in allExpenses) {
            val e = ews.expense
            val splits = ews.splits
            val splitMap = splits.associate { it.memberId to it.owedMinor }

            val payerIsMe = e.payerMemberId == myUid
            val payerIsFriend = e.payerMemberId == friendUid

            if (!payerIsMe && !payerIsFriend) continue // delta = 0, skip

            val delta: Long = when {
                payerIsMe -> splitMap[friendUid] ?: 0L      // B owes A
                payerIsFriend -> -(splitMap[myUid] ?: 0L)    // A owes B
                else -> 0L
            }

            if (delta == 0L) continue // No pairwise effect

            val source = resolveSource(e.contextType, e.contextId)

            items.add(
                FriendActivityItem.ExpenseItem(
                    id = e.id,
                    createdAt = e.createdAt,
                    currency = e.currency,
                    amountMinor = e.amountMinor,
                    pairwiseDeltaMinor = delta,
                    source = source,
                    description = e.note,
                    payerUid = e.payerMemberId,
                    payerName = resolveName(e.payerMemberId)
                )
            )

            netByCurrency[e.currency] = (netByCurrency[e.currency] ?: 0L) + delta
        }

        // Process payments: only those between me and friend
        for (p in allPayments) {
            val isMyPayToFriend = p.fromMemberId == myUid && p.toMemberId == friendUid
            val isFriendPayToMe = p.fromMemberId == friendUid && p.toMemberId == myUid

            if (!isMyPayToFriend && !isFriendPayToMe) continue

            val delta: Long = if (isMyPayToFriend) {
                +p.amountMinor   // A paid B → A's net goes up
            } else {
                -p.amountMinor   // B paid A → A's net goes down
            }

            val source = resolveSource(p.contextType, p.contextId)

            items.add(
                FriendActivityItem.PaymentItem(
                    id = p.id,
                    createdAt = p.createdAt,
                    currency = p.currency,
                    amountMinor = p.amountMinor,
                    pairwiseDeltaMinor = delta,
                    source = source,
                    fromUid = p.fromMemberId,
                    toUid = p.toMemberId,
                    fromName = resolveName(p.fromMemberId),
                    toName = resolveName(p.toMemberId)
                )
            )

            netByCurrency[p.currency] = (netByCurrency[p.currency] ?: 0L) + delta
        }

        // Sort by createdAt descending
        items.sortByDescending { it.createdAt }

        return FriendActivity(items = items, netByCurrency = netByCurrency)
    }
}

