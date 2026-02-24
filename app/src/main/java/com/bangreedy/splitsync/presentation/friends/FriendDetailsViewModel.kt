package com.bangreedy.splitsync.presentation.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangreedy.splitsync.domain.model.FriendActivity
import com.bangreedy.splitsync.domain.model.TotalInDefault
import com.bangreedy.splitsync.domain.repository.UserProfileRepository
import com.bangreedy.splitsync.domain.usecase.ComputeTotalInDefaultCurrencyUseCase
import com.bangreedy.splitsync.domain.usecase.CreateDirectExpenseUseCase
import com.bangreedy.splitsync.domain.usecase.CreateDirectPaymentUseCase
import com.bangreedy.splitsync.domain.usecase.EnsureDirectThreadUseCase
import com.bangreedy.splitsync.domain.usecase.ObserveFriendActivityUseCase
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
    val showSettleUp: Boolean = false
)

class FriendDetailsViewModel(
    private val friendUid: String,
    private val observeActivity: ObserveFriendActivityUseCase,
    private val createDirectExpense: CreateDirectExpenseUseCase,
    private val createDirectPayment: CreateDirectPaymentUseCase,
    private val ensureThread: EnsureDirectThreadUseCase,
    private val computeTotal: ComputeTotalInDefaultCurrencyUseCase,
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

            // Ensure thread exists and get threadId
            viewModelScope.launch {
                try {
                    val tid = ensureThread(uid, friendUid)
                    _state.update { it.copy(threadId = tid) }
                } catch (e: Exception) {
                    _state.update { it.copy(error = e.message) }
                }
            }

            // Observe my default currency
            val defaultCurrencyFlow = userProfileRepo.observeMyProfile(uid)
                .filterNotNull()
                .map { it.defaultCurrency }

            // Observe activity
            val activityFlow = observeActivity(uid, friendUid)

            // Combine activity + default currency → recompute total
            viewModelScope.launch {
                combine(activityFlow, defaultCurrencyFlow) { activity, defaultCurrency ->
                    Pair(activity, defaultCurrency)
                }.collect { (activity, defaultCurrency) ->
                    _state.update {
                        it.copy(
                            activity = activity,
                            defaultCurrency = defaultCurrency
                        )
                    }
                    // Recompute total in background
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

    fun settleUp(amountMinor: Long, currency: String, iPayFriend: Boolean) {
        val uid = myUid ?: return
        val tid = _state.value.threadId ?: return
        viewModelScope.launch {
            try {
                val from = if (iPayFriend) uid else friendUid
                val to = if (iPayFriend) friendUid else uid
                createDirectPayment(tid, from, to, amountMinor, currency)
                _state.update { it.copy(showSettleUp = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            }
        }
    }

    fun toggleAddExpense() = _state.update { it.copy(showAddExpense = !it.showAddExpense) }
    fun toggleSettleUp() = _state.update { it.copy(showSettleUp = !it.showSettleUp) }
    fun clearError() = _state.update { it.copy(error = null) }
}




