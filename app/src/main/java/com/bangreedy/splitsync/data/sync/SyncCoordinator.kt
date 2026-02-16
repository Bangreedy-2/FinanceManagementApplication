package com.bangreedy.splitsync.data.sync

class SyncCoordinator(
    private val groupSync: GroupSyncManager,
    // later: memberSync, expenseSync, paymentSync
) {
    fun start(userId: String) {
        groupSync.start(userId)
    }
    suspend fun pushNow(userId: String) {
        groupSync.pushDirtyGroups(userId)
    }
}
