package com.bangreedy.splitsync.presentation.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangreedy.splitsync.domain.model.NfcInvitePayload
import com.bangreedy.splitsync.domain.repository.NfcRedeemResult
import com.bangreedy.splitsync.domain.usecase.AcceptNfcFriendInviteUseCase
import com.bangreedy.splitsync.domain.usecase.CreateNfcFriendTokenUseCase
import com.bangreedy.splitsync.presentation.common.NfcCoordinator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class ScanResult {
    data object Idle : ScanResult()
    data object Processing : ScanResult()
    data class Success(val message: String) : ScanResult()
    data class Error(val message: String) : ScanResult()
}

data class NfcFriendUiState(
    val sharePayload: NfcInvitePayload? = null,
    val shareLoading: Boolean = false,
    val shareError: String? = null,
    val scanResult: ScanResult = ScanResult.Idle,
    val nfcSupported: Boolean = true
)

class NfcFriendViewModel(
    private val createToken: CreateNfcFriendTokenUseCase,
    private val acceptInvite: AcceptNfcFriendInviteUseCase,
    private val nfcCoordinator: NfcCoordinator,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow(NfcFriendUiState())
    val state: StateFlow<NfcFriendUiState> = _state.asStateFlow()

    private val myUid: String? get() = auth.currentUser?.uid

    init {
        // Collect incoming NFC scans
        viewModelScope.launch {
            nfcCoordinator.pendingInvites.collect { payload ->
                handleScannedPayload(payload)
            }
        }
    }

    /**
     * Generate a new NFC share token for the current user.
     */
    fun generateShareToken() {
        val uid = myUid ?: return
        viewModelScope.launch {
            _state.update { it.copy(shareLoading = true, shareError = null) }
            try {
                val payload = createToken(uid)
                _state.update { it.copy(sharePayload = payload, shareLoading = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        shareLoading = false,
                        shareError = e.message ?: "Failed to generate token"
                    )
                }
            }
        }
    }

    /**
     * Handle a manually entered or QR-scanned URI.
     */
    fun onManualUri(uriString: String) {
        val payload = NfcInvitePayload.fromUri(uriString)
        if (payload == null) {
            _state.update { it.copy(scanResult = ScanResult.Error("Invalid link")) }
            return
        }
        handleScannedPayload(payload)
    }

    fun resetScanResult() {
        _state.update { it.copy(scanResult = ScanResult.Idle) }
    }

    fun setNfcSupported(supported: Boolean) {
        _state.update { it.copy(nfcSupported = supported) }
    }

    private fun handleScannedPayload(payload: NfcInvitePayload) {
        val uid = myUid ?: return
        viewModelScope.launch {
            _state.update { it.copy(scanResult = ScanResult.Processing) }
            try {
                when (val result = acceptInvite(uid, payload)) {
                    is NfcRedeemResult.FriendRequestSent ->
                        _state.update {
                            it.copy(scanResult = ScanResult.Success("Friend added: ${result.friendDisplayName}"))
                        }
                    is NfcRedeemResult.AlreadyFriends ->
                        _state.update {
                            it.copy(scanResult = ScanResult.Success("Already friends with ${result.friendDisplayName}"))
                        }
                    is NfcRedeemResult.Error ->
                        _state.update {
                            it.copy(scanResult = ScanResult.Error(result.message))
                        }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(scanResult = ScanResult.Error(e.message ?: "Failed to process invite"))
                }
            }
        }
    }
}

