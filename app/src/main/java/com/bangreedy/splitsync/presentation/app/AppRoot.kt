package com.bangreedy.splitsync.presentation.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.bangreedy.splitsync.domain.repository.AuthState
import com.bangreedy.splitsync.presentation.auth.AuthScreen
import com.bangreedy.splitsync.presentation.navigation.AppNavGraph
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppRoot(vm: AppStateViewModel = koinViewModel()) {
    val auth by vm.auth.collectAsState()

    when (auth) {
        is AuthState.SignedIn -> AppNavGraph()
        AuthState.SignedOut -> AuthScreen()
    }
}
