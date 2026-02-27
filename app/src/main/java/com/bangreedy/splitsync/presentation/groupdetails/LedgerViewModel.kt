package com.bangreedy.splitsync.presentation.groupdetails

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangreedy.splitsync.domain.model.ConversionResult
import com.bangreedy.splitsync.domain.model.DebtBucket
import com.bangreedy.splitsync.domain.model.Expense
import com.bangreedy.splitsync.domain.model.Member
import com.bangreedy.splitsync.domain.model.Payment
import com.bangreedy.splitsync.domain.model.Settlement
import com.bangreedy.splitsync.domain.model.TotalInDefault
import com.bangreedy.splitsync.domain.repository.UserProfileRepository
import com.bangreedy.splitsync.domain.usecase.BuildSettlementPlanUseCase
import com.bangreedy.splitsync.domain.usecase.ComputeGroupBalancesUseCase
import com.bangreedy.splitsync.domain.usecase.ComputeTotalInDefaultCurrencyUseCase
import com.bangreedy.splitsync.domain.usecase.ConvertMoneyUseCase
import com.bangreedy.splitsync.domain.usecase.ExecuteSettlementPlanUseCase
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
    /** memberId -> currency -> signed minor (multi-currency balances) */
    val balancesByCurrency: Map<String, Map<String, Long>> = emptyMap(),
    /** memberId -> approx total in default currency */
    val memberTotals: Map<String, TotalInDefault> = emptyMap(),
    val suggestions: List<Settlement> = emptyList(),
    val userDefaultCurrency: String = "USD",
    val conversions: Map<String, ConversionResult> = emptyMap(),
    /** For backward compat */
    val balances: Map<String, Long> = emptyMap(),
    /** Pairwise debts for settle-up: (memberA, memberB) -> currency -> signed minor */
    val pairwise: Map<Pair<String, String>, Map<String, Long>> = emptyMap(),
    /** Debt buckets for the selected settle-up target */
    val settleTargetUid: String? = null,
    val settleTargetBuckets: List<DebtBucket> = emptyList(),
    val showSettleDialog: Boolean = false,
    val isExecutingPlan: Boolean = false,
    val settleError: String? = null
)

class LedgerViewModel(
    private val groupId: String,
    private val observeMembers: ObserveMembersUseCase,
    private val observeExpenses: ObserveExpensesUseCase,
    private val observePayments: ObservePaymentsUseCase,
    private val computeBalances: ComputeGroupBalancesUseCase,
    private val suggestSettlements: SuggestSettlementsUseCase,
    private val convertMoney: ConvertMoneyUseCase,
    private val computeTotal: ComputeTotalInDefaultCurrencyUseCase,
    private val buildPlan: BuildSettlementPlanUseCase,
    private val executePlan: ExecuteSettlementPlanUseCase,
    private val userProfileRepo: UserProfileRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow(LedgerUiState())
    val state: StateFlow<LedgerUiState> = _state.asStateFlow()

    private var lastExpenses: List<Expense> = emptyList()
    private var lastPayments: List<Payment> = emptyList()
    private val myUid: String? get() = auth.currentUser?.uid

    init {
        val uid = myUid
        if (uid != null) {
            viewModelScope.launch {
                userProfileRepo.observeMyProfile(uid).collect { profile ->
                    val newCurrency = profile?.defaultCurrency ?: "USD"
                    val oldCurrency = _state.value.userDefaultCurrency
                    _state.update { it.copy(userDefaultCurrency = newCurrency) }
                    if (newCurrency != oldCurrency) {
                        fetchConversions()
                        computeMemberTotals()
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
        // Multi-currency balances
        val multiResult = computeBalances.computeMultiCurrency(lastExpenses, lastPayments)

        // Legacy single-currency for old suggestions (backward compat)
        val legacyBalances = computeBalances(lastExpenses, lastPayments)
        val suggestions = suggestSettlements(legacyBalances)

        _state.update { s ->
            s.copy(
                expenses = lastExpenses,
                payments = lastPayments,
                balancesByCurrency = multiResult.byCurrency,
                pairwise = multiResult.pairwise,
                balances = legacyBalances,
                suggestions = suggestions
            )
        }

        computeMemberTotals()
    }

    /**
     * Compute approximate total in default currency for each member.
     */
    private fun computeMemberTotals() {
        viewModelScope.launch(Dispatchers.IO) {
            val defaultCurrency = _state.value.userDefaultCurrency
            val balancesByCurrency = _state.value.balancesByCurrency
            val totals = mutableMapOf<String, TotalInDefault>()

            for ((memberId, currMap) in balancesByCurrency) {
                val total = computeTotal(currMap, defaultCurrency)
                totals[memberId] = total
            }

            _state.update { it.copy(memberTotals = totals) }
        }
    }

    // ---- Settle Up ----

    /**
     * Open settle dialog for a specific member.
     * Computes pairwise debt buckets within this group between me and the target.
     */
    fun openSettleFor(targetUid: String) {
        val uid = myUid ?: return
        val pairwise = _state.value.pairwise

        // Find pairwise entry for (me, target)
        val key = if (uid < targetUid) Pair(uid, targetUid) else Pair(targetUid, uid)
        val currencyMap = pairwise[key] ?: emptyMap()

        // Build debt buckets. Convention: key stores from first's perspective (first < second).
        // If uid == key.first, sign is correct. If uid == key.second, invert.
        val iAmFirst = uid < targetUid

        val buckets = currencyMap
            .filter { it.value != 0L }
            .map { (currency, signedMinor) ->
                val netForMe = if (iAmFirst) signedMinor else -signedMinor
                DebtBucket(
                    contextType = "GROUP",
                    contextId = groupId,
                    currency = currency,
                    netMinor = netForMe, // + = target owes me, - = I owe target
                    label = "Group"
                )
            }

        _state.update {
            it.copy(
                settleTargetUid = targetUid,
                settleTargetBuckets = buckets,
                showSettleDialog = true,
                settleError = null
            )
        }
    }

    fun dismissSettleDialog() {
        _state.update {
            it.copy(showSettleDialog = false, settleTargetUid = null, settleTargetBuckets = emptyList())
        }
    }

    fun executeSettlePlan(selectedBuckets: List<DebtBucket>, payCurrency: String) {
        val uid = myUid ?: return
        val targetUid = _state.value.settleTargetUid ?: return

        viewModelScope.launch {
            _state.update { it.copy(isExecutingPlan = true, settleError = null) }
            try {
                val plan = buildPlan(selectedBuckets, payCurrency, uid, targetUid)
                executePlan(plan)
                _state.update {
                    it.copy(
                        isExecutingPlan = false,
                        showSettleDialog = false,
                        settleTargetUid = null,
                        settleTargetBuckets = emptyList()
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isExecutingPlan = false, settleError = e.message) }
            }
        }
    }
}
