package com.bangreedy.splitsync.presentation.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangreedy.splitsync.domain.model.DebtBucket
import com.bangreedy.splitsync.domain.model.FriendActivity
import com.bangreedy.splitsync.domain.model.TotalInDefault
import com.bangreedy.splitsync.domain.repository.UserProfileRepository
import com.bangreedy.splitsync.domain.usecase.BuildSettlementPlanUseCase
import com.bangreedy.splitsync.domain.usecase.ComputeTotalInDefaultCurrencyUseCase
import com.bangreedy.splitsync.domain.usecase.CreateDirectExpenseUseCase
import com.bangreedy.splitsync.domain.usecase.EnsureDirectThreadUseCase
import com.bangreedy.splitsync.domain.usecase.ExecuteSettlementPlanUseCase
import com.bangreedy.splitsync.domain.usecase.ObserveFriendActivityUseCase
import com.bangreedy.splitsync.domain.usecase.ObservePairwiseDebtBucketsUseCase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FriendDetailsUiState(
    val friendUid: String = "",
    val friendDisplayName: String = "",
    val friendUsername: String = "",
    val friendPhotoUrl: String? = null,
    val friendEmail: String? = null,
    val activity: FriendActivity = FriendActivity(emptyList(), emptyMap()),
    val totalInDefault: TotalInDefault? = null,
    val defaultCurrency: String = "USD",
    val threadId: String? = null,
    val error: String? = null,
    val showAddExpense: Boolean = false,
    val showSettleUp: Boolean = false,
    val debtBuckets: List<DebtBucket> = emptyList(),
    val isFullySettled: Boolean = true,
    val isExecutingPlan: Boolean = false
)

class FriendDetailsViewModel(
    private val friendUid: String,
    private val observeActivity: ObserveFriendActivityUseCase,
    private val createDirectExpense: CreateDirectExpenseUseCase,
    private val ensureThread: EnsureDirectThreadUseCase,
    private val computeTotal: ComputeTotalInDefaultCurrencyUseCase,
    private val observeDebtBuckets: ObservePairwiseDebtBucketsUseCase,
    private val buildPlan: BuildSettlementPlanUseCase,
    private val executePlan: ExecuteSettlementPlanUseCase,
    private val userProfileRepo: UserProfileRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow(FriendDetailsUiState(friendUid = friendUid))
    val state: StateFlow<FriendDetailsUiState> = _state.asStateFlow()

    private val myUid: String? get() = auth.currentUser?.uid

    init {
        val uid = myUid
        if (uid != null) {
            // Load friend's profile
            viewModelScope.launch {
                userProfileRepo.observeMyProfile(friendUid).collect { profile ->
                    if (profile != null) {
                        _state.update {
                            it.copy(
                                friendDisplayName = profile.displayName,
                                friendUsername = profile.username,
                                friendPhotoUrl = profile.photoUrl,
                                friendEmail = profile.email
                            )
                        }
                    }
                }
            }

            // Ensure thread exists
            viewModelScope.launch {
                try {
                    val tid = ensureThread(uid, friendUid)
                    _state.update { it.copy(threadId = tid) }
                } catch (e: Exception) {
                    _state.update { it.copy(error = e.message) }
                }
            }

            val defaultCurrencyFlow = userProfileRepo.observeMyProfile(uid)
                .filterNotNull()
                .map { it.defaultCurrency }

            val activityFlow = observeActivity(uid, friendUid)
            val bucketsFlow = observeDebtBuckets(uid, friendUid)

            // Combine activity + default currency + buckets
            viewModelScope.launch {
                combine(activityFlow, defaultCurrencyFlow, bucketsFlow) { activity, defaultCurrency, buckets ->
                    Triple(activity, defaultCurrency, buckets)
                }.collect { (activity, defaultCurrency, buckets) ->
                    _state.update {
                        it.copy(
                            activity = activity,
                            defaultCurrency = defaultCurrency,
                            debtBuckets = buckets,
                            isFullySettled = buckets.isEmpty()
                        )
                    }
                    recomputeTotal(activity.netByCurrency, defaultCurrency)
                }
            }
        }
    }

    private fun recomputeTotal(netByCurrency: Map<String, Long>, defaultCurrency: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val total = computeTotal(netByCurrency, defaultCurrency)
            _state.update { it.copy(totalInDefault = total) }
        }
    }

    fun addDirectExpense(amountMinor: Long, currency: String, note: String?, iPayForFriend: Boolean) {
        val uid = myUid ?: return
        val tid = _state.value.threadId ?: return
        viewModelScope.launch {
            try {
                val payerUid = if (iPayForFriend) uid else friendUid
                createDirectExpense(
                    threadId = tid,
                    payerUid = payerUid,
                    amountMinor = amountMinor,
                    currency = currency,
                    note = note,
                    splitUids = listOf(uid, friendUid)
                )
                _state.update { it.copy(showAddExpense = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    /**
     * Build and execute a settlement plan for selected debt buckets.
     */
    fun settleWithPlan(selectedBuckets: List<DebtBucket>, payCurrency: String) {
        val uid = myUid ?: return
        viewModelScope.launch {
            _state.update { it.copy(isExecutingPlan = true, error = null) }
            try {
                val plan = buildPlan(selectedBuckets, payCurrency, uid, friendUid)
                executePlan(plan)
                _state.update { it.copy(showSettleUp = false, isExecutingPlan = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isExecutingPlan = false, error = e.message) }
            }
        }
    }

    fun toggleAddExpense() = _state.update { it.copy(showAddExpense = !it.showAddExpense) }
    fun toggleSettleUp() = _state.update { it.copy(showSettleUp = !it.showSettleUp) }
    fun clearError() = _state.update { it.copy(error = null) }
}
