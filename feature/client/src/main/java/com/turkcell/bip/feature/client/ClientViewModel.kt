package com.turkcell.bip.feature.client

import androidx.lifecycle.viewModelScope
import com.turkcell.bip.core.common.UiState
import com.turkcell.bip.core.common.BaseViewModel
import com.turkcell.bip.core.common.UiEffect
import com.turkcell.bip.core.common.UiIntent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

data object ClientState : UiState

sealed interface ClientIntent : UiIntent {
    data object OnBackClick : ClientIntent
}

sealed interface ClientEffect : UiEffect {
    data object NavigateBack : ClientEffect
}

@HiltViewModel
class ClientViewModel @Inject constructor() : BaseViewModel<ClientState, ClientIntent, ClientEffect>(ClientState) {

    override fun onIntent(intent: ClientIntent) {
        viewModelScope.launch {
            when (intent) {
                ClientIntent.OnBackClick -> sendEffect(ClientEffect.NavigateBack)
            }
        }
    }
}
