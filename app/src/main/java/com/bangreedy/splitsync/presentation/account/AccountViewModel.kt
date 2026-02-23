package com.bangreedy.splitsync.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangreedy.splitsync.domain.model.NotificationPrefs
import com.bangreedy.splitsync.domain.model.UserProfile
import com.bangreedy.splitsync.domain.repository.UserProfileRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AccountViewModel(
    private val repo: UserProfileRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    init {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                repo.observeMyProfile(uid).collectLatest { profile ->
                    _userProfile.value = profile
                }
            }
        }
    }

    fun updateDisplayName(newName: String) {
        val uid = auth.currentUser?.uid ?: return
        if (newName.isBlank()) return
        viewModelScope.launch {
            repo.updateProfile(uid, displayName = newName, photoUrl = null)
        }
    }

    fun updateDefaultCurrency(currencyCode: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repo.updateDefaultCurrency(uid, currencyCode)
        }
    }

    fun updateNotificationPrefs(prefs: NotificationPrefs) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            repo.updateNotificationPrefs(uid, prefs)
        }
    }

    suspend fun checkUsernameAvailability(username: String): Boolean {
        // Simple client-side validation first
        if (username.length !in 3..20 || !username.all { it.isLetterOrDigit() || it == '_' }) {
            return false
        }
        // Don't check against own username (would return 'not available' if implemented naively,
        // but repo check usually looks up owner. If owner == self, it's available.
        // But here we want to know if *another* user has it.)
        // Ideally repo.isUsernameAvailable handles this, or we check if username == current.
        val current = _userProfile.value?.username
        if (username.equals(current, ignoreCase = true)) return true

        return repo.isUsernameAvailable(username)
    }

    fun updateUsername(newUsername: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val currentProfile = _userProfile.value ?: return

        viewModelScope.launch {
            try {
                if (checkUsernameAvailability(newUsername)) {
                    repo.claimUsername(uid, newUsername, currentProfile.displayName, currentProfile.email)
                    onSuccess()
                } else {
                    onError("Username unavailable")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to update username")
            }
        }
    }

    fun signOut() {
        auth.signOut()
        // Navigation to Auth screen should happen automatically via AppRoot observer
    }
}
