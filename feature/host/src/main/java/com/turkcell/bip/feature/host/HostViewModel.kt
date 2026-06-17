package com.turkcell.bip.feature.host

import androidx.lifecycle.viewModelScope
import com.turkcell.bip.core.common.BaseViewModel
import com.turkcell.bip.core.common.UiEffect
import com.turkcell.bip.core.common.UiIntent
import com.turkcell.bip.core.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

data object HostState : UiState

sealed interface HostIntent : UiIntent {
    data object OnBackClick : HostIntent
}

sealed interface HostEffect : UiEffect {
    data object NavigateBack : HostEffect
}

@HiltViewModel
class HostViewModel @Inject constructor() : BaseViewModel<HostState, HostIntent, HostEffect>(HostState) {

    override fun onIntent(intent: HostIntent) {
        viewModelScope.launch {
            when (intent) {
                HostIntent.OnBackClick -> sendEffect(HostEffect.NavigateBack)
            }
        }
    }
}
