package com.bangreedy.splitsync.data.sync

class SyncCoordinator(
    private val groupSyncManager: GroupSyncManager,
    private val memberSyncManager: MemberSyncManager,
    private val expenseSyncManager: ExpenseSyncManager,
    private val paymentSyncManager: PaymentSyncManager
) {
    fun start(userId: String) {
        groupSyncManager.start(userId) { groupIds ->
            memberSyncManager.startForGroups(groupIds)
        }
        expenseSyncManager.start(userId)
        paymentSyncManager.start(userId)
    }

    suspend fun pushNow(userId: String) {
        groupSyncManager.pushDirtyGroups(userId)
        memberSyncManager.pushDirtyMembers()
        expenseSyncManager.pushDirtyExpenses()
        paymentSyncManager.pushDirtyPayments()
    }

    fun stop() {
        groupSyncManager.stop()
        memberSyncManager.stop()
    }
}
