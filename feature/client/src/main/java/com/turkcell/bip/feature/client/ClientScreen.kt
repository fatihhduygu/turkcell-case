package com.turkcell.bip.feature.client

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
fun ClientScreen(
    onBack: () -> Unit,
    viewModel: ClientViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        for (effect in viewModel.effect) {
            when (effect) {
                ClientEffect.NavigateBack -> onBack()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Client Screen")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.onIntent(ClientIntent.OnBackClick) }) {
                Text("Back")
            }
        }
    }
}
