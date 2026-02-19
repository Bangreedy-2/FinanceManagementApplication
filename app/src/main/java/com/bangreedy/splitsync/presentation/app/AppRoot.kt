package com.bangreedy.splitsync.presentation.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import com.bangreedy.splitsync.domain.repository.AuthState
import com.bangreedy.splitsync.presentation.auth.AuthScreen
import com.bangreedy.splitsync.presentation.navigation.AppNavGraph
import com.bangreedy.splitsync.presentation.profile.CreateProfileScreen
import com.bangreedy.splitsync.presentation.profile.ProfileGateViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppRoot(vm: AppStateViewModel = koinViewModel()) {
    val auth by vm.auth.collectAsState()

    when (val a = auth) {
        AuthState.SignedOut -> AuthScreen()

        is AuthState.SignedIn -> {
            val gateVm: ProfileGateViewModel = koinViewModel()
            val profile by gateVm.profile.collectAsState()

            LaunchedEffect(a.userId) {
                gateVm.start(a.userId)
            }

            val needsProfile = profile == null || profile?.username.isNullOrBlank()

            if (needsProfile) {
                CreateProfileScreen(
                    uid = a.userId,
                    email = a.email,
                    onDone = { /* No-op: gate will switch automatically */ }
                )
            } else {
                AppNavGraph()
            }
        }
    }
}
