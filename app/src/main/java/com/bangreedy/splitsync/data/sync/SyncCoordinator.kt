package com.bangreedy.splitsync.data.sync

class SyncCoordinator(
    private val groupSyncManager: GroupSyncManager,
    private val groupMemberSyncManager: GroupMemberSyncManager,
    private val userProfileSyncManager: UserProfileSyncManager,
    private val expenseSyncManager: ExpenseSyncManager,
    private val paymentSyncManager: PaymentSyncManager
) {
    fun start(userId: String) {
        groupSyncManager.start(userId) { groupIds ->
            groupMemberSyncManager.onGroupsChanged(groupIds)
            expenseSyncManager.onGroupsChanged(groupIds)
            paymentSyncManager.onGroupsChanged(groupIds)

            // profiles depend on group_members (uids) so we trigger refresh after groups updated.
            userProfileSyncManager.onGroupsChanged(groupIds)
        }
    }

    suspend fun pushNow(userId: String) {
        groupSyncManager.pushDirtyGroups(userId)
        expenseSyncManager.pushDirtyExpenses()
        paymentSyncManager.pushDirtyPayments()
    }

    fun stop() {
        groupSyncManager.stop()
        groupMemberSyncManager.stop()
        expenseSyncManager.stop()
        paymentSyncManager.stop()
        // userProfileSyncManager doesn't hold listeners in this design
    }
}
