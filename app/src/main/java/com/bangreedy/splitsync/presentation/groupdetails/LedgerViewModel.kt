package com.bangreedy.splitsync.presentation.groupdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangreedy.splitsync.domain.model.ConversionResult
import com.bangreedy.splitsync.domain.model.Expense
import com.bangreedy.splitsync.domain.model.Member
import com.bangreedy.splitsync.domain.model.Payment
import com.bangreedy.splitsync.domain.model.Settlement
import com.bangreedy.splitsync.domain.repository.UserProfileRepository
import com.bangreedy.splitsync.domain.usecase.ComputeGroupBalancesUseCase
import com.bangreedy.splitsync.domain.usecase.ConvertMoneyUseCase
import com.bangreedy.splitsync.domain.usecase.ObserveExpensesUseCase
import com.bangreedy.splitsync.domain.usecase.ObserveMembersUseCase
import com.bangreedy.splitsync.domain.usecase.ObservePaymentsUseCase
import com.bangreedy.splitsync.domain.usecase.SuggestSettlementsUseCase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

data class LedgerUiState(
    val members: List<Member> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val balances: Map<String, Long> = emptyMap(),
    val suggestions: List<Settlement> = emptyList(),
    val userDefaultCurrency: String = "USD",
    val conversions: Map<String, ConversionResult> = emptyMap()
)

class LedgerViewModel(
    private val groupId: String,
    private val observeMembers: ObserveMembersUseCase,
    private val observeExpenses: ObserveExpensesUseCase,
    private val observePayments: ObservePaymentsUseCase,
    private val computeBalances: ComputeGroupBalancesUseCase,
    private val suggestSettlements: SuggestSettlementsUseCase,
    private val convertMoney: ConvertMoneyUseCase,
    private val userProfileRepo: UserProfileRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow(LedgerUiState())
    val state: StateFlow<LedgerUiState> = _state.asStateFlow()

    private var lastExpenses: List<Expense> = emptyList()
    private var lastPayments: List<Payment> = emptyList()

    init {
        // Observe the current user's profile to get their default currency
        val uid = auth.currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                userProfileRepo.observeMyProfile(uid).collect { profile ->
                    val newCurrency = profile?.defaultCurrency ?: "USD"
                    val oldCurrency = _state.value.userDefaultCurrency
                    _state.update { it.copy(userDefaultCurrency = newCurrency) }
                    if (newCurrency != oldCurrency) {
                        fetchConversions()
                    }
                }
            }
        }

        viewModelScope.launch {
            observeMembers(groupId).collect { members ->
                _state.update { it.copy(members = members) }
            }
        }

        viewModelScope.launch {
            observeExpenses(groupId).collect { expenses ->
                lastExpenses = expenses
                recompute()
                fetchConversions()
            }
        }

        viewModelScope.launch {
            observePayments(groupId).collect { payments ->
                lastPayments = payments
                recompute()
                fetchConversions()
            }
        }
    }

    private fun fetchConversions() {
        viewModelScope.launch(Dispatchers.IO) {
            val conversions = mutableMapOf<String, ConversionResult>()
            val userDefaultCurrency = _state.value.userDefaultCurrency

            for (expense in lastExpenses) {
                if (!expense.currency.equals(userDefaultCurrency, ignoreCase = true)) {
                    val expenseDate = Instant.ofEpochMilli(expense.createdAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()

                    val result = convertMoney(
                        amountMinor = expense.amountMinor,
                        fromCurrency = expense.currency,
                        toCurrency = userDefaultCurrency,
                        asOfDate = expenseDate
                    ).getOrNull()

                    if (result != null) {
                        conversions[expense.id] = result
                    }
                }
            }

            for (payment in lastPayments) {
                if (!payment.currency.equals(userDefaultCurrency, ignoreCase = true)) {
                    val paymentDate = Instant.ofEpochMilli(payment.createdAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()

                    val result = convertMoney(
                        amountMinor = payment.amountMinor,
                        fromCurrency = payment.currency,
                        toCurrency = userDefaultCurrency,
                        asOfDate = paymentDate
                    ).getOrNull()

                    if (result != null) {
                        conversions[payment.id] = result
                    }
                }
            }

            _state.update { it.copy(conversions = conversions) }
        }
    }

    private fun recompute() {
        val balances = computeBalances(lastExpenses, lastPayments)
        val suggestions = suggestSettlements(balances)

        _state.update { s ->
            s.copy(
                expenses = lastExpenses,
                payments = lastPayments,
                balances = balances,
                suggestions = suggestions
            )
        }
    }
}
