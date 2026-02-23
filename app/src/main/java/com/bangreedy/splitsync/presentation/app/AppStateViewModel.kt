package com.bangreedy.splitsync.presentation.app

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangreedy.splitsync.data.local.dao.ExpenseDao
import com.bangreedy.splitsync.data.sync.SyncCoordinator
import com.bangreedy.splitsync.domain.repository.AuthState
import com.bangreedy.splitsync.domain.usecase.ObserveAuthStateUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class AppStateViewModel(
    private val observeAuthState: ObserveAuthStateUseCase,
    private val syncCoordinator: SyncCoordinator,
    private val expenseDao: ExpenseDao
) : ViewModel() {

    private var startedForUserId: String? = null
    private val _auth = MutableStateFlow<AuthState>(AuthState.SignedOut)
    val auth: StateFlow<AuthState> = _auth.asStateFlow()

    init {
        viewModelScope.launch {
            observeAuthState().collect { state ->
                _auth.value = state

                if (state is AuthState.SignedIn) {
                    if (startedForUserId != state.userId) {
                        startedForUserId = state.userId
                        syncCoordinator.start(state.userId)
                        runIntegrityCheckOnce()
                    }
                } else {
                    startedForUserId = null
                }
            }
        }
    }
    private fun runIntegrityCheckOnce() {
        viewModelScope.launch(Dispatchers.IO) {
            // small delay so initial listeners can populate cache
            delay(1500)

            val missingPayers = expenseDao.countExpensesWithMissingPayerProfiles()
            val missingSplits = expenseDao.countSplitsWithMissingProfiles()

            Log.d("IntegrityCheck", "missingPayers=$missingPayers missingSplits=$missingSplits")
        }
    }
}
