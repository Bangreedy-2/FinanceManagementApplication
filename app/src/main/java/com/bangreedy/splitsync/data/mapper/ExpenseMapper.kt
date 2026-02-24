package com.bangreedy.splitsync.data.mapper

import com.bangreedy.splitsync.data.local.dao.ExpenseWithSplits
import com.bangreedy.splitsync.data.local.entity.ExpenseEntity
import com.bangreedy.splitsync.data.local.entity.ExpenseSplitEntity
import com.bangreedy.splitsync.data.local.entity.SyncState
import com.bangreedy.splitsync.domain.model.Expense
import com.bangreedy.splitsync.domain.model.SplitShare

fun ExpenseWithSplits.toDomain(): Expense =
    Expense(
        id = expense.id,
        groupId = expense.groupId,
        payerMemberId = expense.payerMemberId,
        amountMinor = expense.amountMinor,
        currency = expense.currency,
        note = expense.note,
        createdAt = expense.createdAt,
        updatedAt = expense.updatedAt,
        deleted = expense.deleted,
        splits = splits.map { SplitShare(memberId = it.memberId, owedMinor = it.owedMinor) }
    )

fun Expense.toEntity(syncState: Int = SyncState.DIRTY): ExpenseEntity =
    ExpenseEntity(
        id = id,
        groupId = groupId,
        payerMemberId = payerMemberId,
        amountMinor = amountMinor,
        currency = currency,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deleted = deleted,
        syncState = syncState
    )

fun Expense.toSplitEntities(): List<ExpenseSplitEntity> =
    splits.map { s ->
        ExpenseSplitEntity(
            expenseId = id,
            memberId = s.memberId,
            owedMinor = s.owedMinor
        )
    }
