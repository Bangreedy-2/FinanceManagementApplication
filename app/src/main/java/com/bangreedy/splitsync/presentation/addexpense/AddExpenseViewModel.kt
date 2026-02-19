package com.bangreedy.splitsync.presentation.addexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangreedy.splitsync.data.sync.SyncCoordinator
import com.bangreedy.splitsync.domain.model.Member
import com.bangreedy.splitsync.domain.usecase.CreateExpenseEqualSplitUseCase
import com.bangreedy.splitsync.domain.usecase.ObserveMembersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddExpenseUiState(
    val members: List<Member> = emptyList(),
    val payerMemberId: String? = null,
    val participantIds: Set<String> = emptySet(),
    val amountText: String = "",
    val note: String = "",
    val currency: String = "EUR",
    val error: String? = null,
    val isSaving: Boolean = false
)

class AddExpenseViewModel(
    private val groupId: String,
    private val observeMembers: ObserveMembersUseCase,
    private val createExpenseEqualSplit: CreateExpenseEqualSplitUseCase,
    private val syncCoordinator: SyncCoordinator
) : ViewModel() {

    private val _state = MutableStateFlow(AddExpenseUiState())
    val state: StateFlow<AddExpenseUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeMembers(groupId).collect { members ->
                _state.update { s ->
                    val payer = s.payerMemberId ?: members.firstOrNull()?.uid
                    val participants = if (s.participantIds.isEmpty()) members.map { it.uid }.toSet() else s.participantIds
                    s.copy(members = members, payerMemberId = payer, participantIds = participants)
                }
            }
        }
    }

    fun onAmountChange(v: String) = _state.update { it.copy(amountText = v, error = null) }
    fun onNoteChange(v: String) = _state.update { it.copy(note = v, error = null) }

    fun onPayerSelected(memberId: String) =
        _state.update { it.copy(payerMemberId = memberId, error = null) }

    fun onToggleParticipant(memberId: String) {
        _state.update { s ->
            val newSet = s.participantIds.toMutableSet()
            if (!newSet.add(memberId)) newSet.remove(memberId)
            s.copy(participantIds = newSet, error = null)
        }
    }

    fun save(onDone: () -> Unit) {
        val s = _state.value
        val payerId = s.payerMemberId

        if (payerId == null) {
            _state.update { it.copy(error = "Select a payer") }
            return
        }

        val amountMinor = parseToMinorUnits(s.amountText)
        if (amountMinor == null || amountMinor <= 0L) {
            _state.update { it.copy(error = "Enter a valid amount") }
            return
        }

        if (s.participantIds.isEmpty()) {
            _state.update { it.copy(error = "Select at least one participant") }
            return
        }

        if (payerId !in s.participantIds) {
            _state.update { it.copy(error = "Payer must be included in participants") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            runCatching {
                createExpenseEqualSplit(
                    groupId = groupId,
                    payerMemberId = payerId,
                    amountMinor = amountMinor,
                    currency = s.currency,
                    note = s.note,
                    participantIds = s.participantIds.toList()
                )
            }.onSuccess {
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) syncCoordinator.pushNow(uid)
                _state.update { it.copy(isSaving = false) }
                onDone()
            }.onFailure { e ->
                _state.update { it.copy(isSaving = false, error = e.message ?: "Failed to save expense") }
            }
        }
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
                    val minorStr = parts[1].padEnd(2, '0').take(2)
                    val minor = minorStr.toLong()
                    major * 100 + minor
                }
                else -> null
            }
        } catch (_: Throwable) {
            null
        }
    }
}
