package com.bangreedy.splitsync.presentation.groupdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangreedy.splitsync.domain.model.Expense
import com.bangreedy.splitsync.domain.model.Member
import com.bangreedy.splitsync.domain.usecase.ComputeGroupBalancesUseCase
import com.bangreedy.splitsync.domain.usecase.ObserveExpensesUseCase
import com.bangreedy.splitsync.domain.usecase.ObserveMembersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LedgerUiState(
    val members: List<Member> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val balances: Map<String, Long> = emptyMap()
)

class LedgerViewModel(
    private val groupId: String,
    private val observeMembers: ObserveMembersUseCase,
    private val observeExpenses: ObserveExpensesUseCase,
    private val computeBalances: ComputeGroupBalancesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LedgerUiState())
    val state: StateFlow<LedgerUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeMembers(groupId).collect { members ->
                _state.update { it.copy(members = members) }
            }
        }
        viewModelScope.launch {
            observeExpenses(groupId).collect { expenses ->
                _state.update { s ->
                    s.copy(
                        expenses = expenses,
                        balances = computeBalances(expenses)
                    )
                }
            }
        }
    }
}
