package com.bangreedy.splitsync.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bangreedy.splitsync.domain.model.UserProfile
import com.bangreedy.splitsync.domain.usecase.ObserveMyProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileGateViewModel(
    private val observeMyProfile: ObserveMyProfileUseCase
) : ViewModel() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private var startedForUid: String? = null

    fun start(uid: String) {
        if (startedForUid == uid) return
        startedForUid = uid

        viewModelScope.launch {
            observeMyProfile(uid).collect { p ->
                _profile.value = p
            }
        }
    }
}
