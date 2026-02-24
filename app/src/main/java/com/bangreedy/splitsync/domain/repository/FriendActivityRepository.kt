package com.bangreedy.splitsync.domain.repository

import com.bangreedy.splitsync.domain.model.FriendActivity
import kotlinx.coroutines.flow.Flow

interface FriendActivityRepository {
    fun observeFriendActivity(myUid: String, friendUid: String): Flow<FriendActivity>
}

