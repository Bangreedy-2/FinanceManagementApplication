package com.bangreedy.splitsync.data.sync

class SyncCoordinator(
    private val groupSyncManager: GroupSyncManager,
    private val groupMemberSyncManager: GroupMemberSyncManager,
    private val userProfileSyncManager: UserProfileSyncManager,
    private val expenseSyncManager: ExpenseSyncManager,
    private val paymentSyncManager: PaymentSyncManager,
    private val notificationSyncManager: NotificationSyncManager,
    private val friendSyncManager: FriendSyncManager,
    private val directThreadSyncManager: DirectThreadSyncManager
) {
    fun start(userId: String) {
        groupSyncManager.start(userId) { groupIds ->
            groupMemberSyncManager.onGroupsChanged(groupIds)
            expenseSyncManager.onGroupsChanged(groupIds)
            paymentSyncManager.onGroupsChanged(groupIds)
            userProfileSyncManager.onGroupsChanged(groupIds)
        }
        notificationSyncManager.start(userId)

        // Friends sync
        directThreadSyncManager.setMyUid(userId)
        friendSyncManager.onAcceptedFriendsChanged = { acceptedFriendUids ->
            directThreadSyncManager.onAcceptedFriendsChanged(acceptedFriendUids)
        }
        friendSyncManager.start(userId)
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
        notificationSyncManager.stop()
        friendSyncManager.stop()
        directThreadSyncManager.stop()
    }
}
