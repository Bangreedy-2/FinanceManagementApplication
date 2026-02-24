package com.bangreedy.splitsync.presentation.groupdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangreedy.splitsync.domain.model.Expense
import com.bangreedy.splitsync.domain.model.Member
import com.bangreedy.splitsync.domain.model.Payment
import com.bangreedy.splitsync.domain.model.Settlement
import com.bangreedy.splitsync.domain.usecase.ComputeGroupBalancesUseCase
import com.bangreedy.splitsync.domain.usecase.ObserveExpensesUseCase
import com.bangreedy.splitsync.domain.usecase.ObserveMembersUseCase
import com.bangreedy.splitsync.domain.usecase.ObservePaymentsUseCase
import com.bangreedy.splitsync.domain.usecase.SuggestSettlementsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LedgerUiState(
    val members: List<Member> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val balances: Map<String, Long> = emptyMap(),
    val suggestions: List<Settlement> = emptyList()
)

class LedgerViewModel(
    private val groupId: String,
    private val observeMembers: ObserveMembersUseCase,
    private val observeExpenses: ObserveExpensesUseCase,
    private val observePayments: ObservePaymentsUseCase,
    private val computeBalances: ComputeGroupBalancesUseCase,
    private val suggestSettlements: SuggestSettlementsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LedgerUiState())
    val state: StateFlow<LedgerUiState> = _state.asStateFlow()

    private var lastExpenses: List<Expense> = emptyList()
    private var lastPayments: List<Payment> = emptyList()

    init {
        viewModelScope.launch {
            observeMembers(groupId).collect { members ->
                _state.update { it.copy(members = members) }
            }
        }

        viewModelScope.launch {
            observeExpenses(groupId).collect { expenses ->
                lastExpenses = expenses
                recompute()
            }
        }

        viewModelScope.launch {
            observePayments(groupId).collect { payments ->
                lastPayments = payments
                recompute()
            }
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
