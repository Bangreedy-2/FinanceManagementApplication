package com.bangreedy.splitsync.data.sync

class SyncCoordinator(
    private val groupSyncManager: GroupSyncManager,
    private val memberSyncManager: MemberSyncManager
) {
    fun start(userId: String) {
        groupSyncManager.start(userId) { groupIds ->
            memberSyncManager.startForGroups(groupIds)
        }
    }

    suspend fun pushNow(userId: String) {
        groupSyncManager.pushDirtyGroups(userId)
        memberSyncManager.pushDirtyMembers()
    }

    fun stop() {
        groupSyncManager.stop()
        memberSyncManager.stop()
    }
}
