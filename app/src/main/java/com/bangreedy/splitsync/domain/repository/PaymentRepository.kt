package com.bangreedy.splitsync.domain.repository

import com.bangreedy.splitsync.domain.model.Payment
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {
    fun observePayments(groupId: String): Flow<List<Payment>>
    suspend fun createPayment(payment: Payment): String
}
