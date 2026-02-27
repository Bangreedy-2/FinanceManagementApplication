package com.bangreedy.splitsync.presentation.settleup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangreedy.splitsync.data.sync.SyncCoordinator
import com.bangreedy.splitsync.domain.model.Member
import com.bangreedy.splitsync.domain.model.Settlement
import com.bangreedy.splitsync.domain.usecase.CreatePaymentUseCase
import com.bangreedy.splitsync.domain.usecase.ObserveMembersUseCase
import com.bangreedy.splitsync.domain.usecase.ObserveExpensesUseCase
import com.bangreedy.splitsync.domain.usecase.ComputeGroupBalancesUseCase
import com.bangreedy.splitsync.domain.usecase.SuggestSettlementsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.bangreedy.splitsync.domain.model.Payment
import com.bangreedy.splitsync.domain.usecase.ObservePaymentsUseCase
import com.bangreedy.splitsync.presentation.common.isValidCurrency


data class SettleUpUiState(
    val members: List<Member> = emptyList(),
    val fromId: String = "",
    val toId: String = "",
    val currency: String = "EUR",
    val suggestedAmountMinor: Long = 0L,
    val amountText: String = "0",
    val error: String? = null,
    val isSaving: Boolean = false
)

class SettleUpViewModel(
    private val groupId: String,
    private val initialFromId: String,
    private val initialToId: String,
    private val initialAmountMinor: Long,
    private val observeMembers: ObserveMembersUseCase,
    private val observeExpenses: ObserveExpensesUseCase,
    private val computeBalances: ComputeGroupBalancesUseCase,
    private val suggestSettlements: SuggestSettlementsUseCase,
    private val createPayment: CreatePaymentUseCase,
    private val observePayments: ObservePaymentsUseCase,
    private val syncCoordinator: SyncCoordinator
    ) : ViewModel() {

    private val _state = MutableStateFlow(SettleUpUiState())
    val state: StateFlow<SettleUpUiState> = _state.asStateFlow()

    private var lastSuggestions: List<Settlement> = emptyList()
    private var lastExpenses = emptyList<com.bangreedy.splitsync.domain.model.Expense>()
    private var lastPayments = emptyList<Payment>()

    init {
        viewModelScope.launch {
            observeMembers(groupId).collect { members ->
                _state.update { s ->
                    val fallbackFrom = members.firstOrNull()?.uid.orEmpty()
                    val fallbackTo = members.getOrNull(1)?.uid.orEmpty()

                    val from = initialFromId.ifBlank { s.fromId.ifBlank { fallbackFrom } }
                    val to = initialToId.ifBlank { s.toId.ifBlank { fallbackTo } }

                    s.copy(members = members, fromId = from, toId = to)
                }
                refreshSuggestedAmount()
            }
        }
        viewModelScope.launch {
            observeExpenses(groupId).collect { expenses ->
                lastExpenses = expenses
                recalcSuggestions()
            }
        }

        viewModelScope.launch {
            observePayments(groupId).collect { payments ->
                lastPayments = payments
                recalcSuggestions()
            }
        }


        // If we opened from a suggestion tap, prefill amount
        if (initialAmountMinor > 0L) {
            _state.update { it.copy(amountText = minorToText(initialAmountMinor)) }
        }
    }
    private fun recalcSuggestions() {
        val balances = computeBalances(lastExpenses, lastPayments)
        lastSuggestions = suggestSettlements(balances)
        _state.update { it.copy(currency = lastExpenses.firstOrNull()?.currency ?: it.currency) }
        refreshSuggestedAmount()
    }


    fun onFromSelected(id: String) {
        _state.update { it.copy(fromId = id, error = null) }
        refreshSuggestedAmount()
    }

    fun onToSelected(id: String) {
        _state.update { it.copy(toId = id, error = null) }
        refreshSuggestedAmount()
    }

    fun onAmountChange(v: String) {
        _state.update { it.copy(amountText = v, error = null) }
    }

    fun onCurrencyChange(v: String) {
        _state.update { it.copy(currency = v, error = null) }
    }

    private fun refreshSuggestedAmount() {
        val s = _state.value
        val from = s.fromId
        val to = s.toId

        val suggested = lastSuggestions
            .firstOrNull { it.fromMemberId == from && it.toMemberId == to }
            ?.amountMinor ?: 0L

        _state.update { st ->
            // If user opened blank (no prefill), keep amountText synced to suggestion.
            // If you want "user edits override", we can add a flag later.
            val shouldOverwrite = initialAmountMinor == 0L
            st.copy(
                suggestedAmountMinor = suggested,
                amountText = if (shouldOverwrite) minorToText(suggested) else st.amountText
            )
        }
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        if (s.fromId.isBlank() || s.toId.isBlank()) {
            _state.update { it.copy(error = "Select both people") }
            return
        }
        if (s.fromId == s.toId) {
            _state.update { it.copy(error = "Cannot settle with the same person") }
            return
        }

        val amountMinor = parseToMinorUnits(s.amountText)
        if (amountMinor == null || amountMinor <= 0L) {
            _state.update { it.copy(error = "Enter a valid amount") }
            return
        }

        if (!isValidCurrency(s.currency)) {
            _state.update { it.copy(error = "Select a valid currency") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            runCatching {
                createPayment(
                    groupId = groupId,
                    fromMemberId = s.fromId,
                    toMemberId = s.toId,
                    amountMinor = amountMinor,
                    currency = s.currency
                )
            }.onSuccess {
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) syncCoordinator.pushNow(uid)
                _state.update { it.copy(isSaving = false) }
                onDone()
            }.onFailure { e ->
                _state.update { it.copy(isSaving = false, error = e.message ?: "Failed to save payment") }
            }
        }
    }

    private fun minorToText(minor: Long): String {
        val major = minor / 100
        val cents = (minor % 100).toString().padStart(2, '0')
        return "$major.$cents"
    }

    private fun parseToMinorUnits(text: String): Long? {
        val t = text.trim().replace(',', '.')
        if (t.isBlank()) return null
        val parts = t.split('.')
        return try {
            when (parts.size) {
                1 -> parts[0].toLong() * 100
                2 -> {
                    val major = parts[0].toLong()
                    val minor = parts[1].padEnd(2, '0').take(2).toLong()
                    major * 100 + minor
                }
                else -> null
            }
        } catch (_: Throwable) { null }
    }
}
