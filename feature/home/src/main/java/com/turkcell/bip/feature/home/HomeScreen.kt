package com.turkcell.bip.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun HomeScreen(
    onNavigateToHost: () -> Unit,
    onNavigateToClient: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        for (effect in viewModel.effect) {
            when (effect) {
                HomeEffect.NavigateToHost -> onNavigateToHost()
                HomeEffect.NavigateToClient -> onNavigateToClient()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Home")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.onIntent(HomeIntent.OnHostClick) }) {
                Text("Host")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.onIntent(HomeIntent.OnClientClick) }) {
                Text("Client")
            }
        }
    }
}
