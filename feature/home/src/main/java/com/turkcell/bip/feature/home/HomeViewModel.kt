package com.turkcell.bip.feature.home

import androidx.lifecycle.viewModelScope
import com.turkcell.bip.core.common.BaseViewModel
import com.turkcell.bip.core.common.UiEffect
import com.turkcell.bip.core.common.UiIntent
import com.turkcell.bip.core.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

data object HomeState : UiState

sealed interface HomeIntent : UiIntent {
    data object OnHostClick : HomeIntent
    data object OnClientClick : HomeIntent
}

sealed interface HomeEffect : UiEffect {
    data object NavigateToHost : HomeEffect
    data object NavigateToClient : HomeEffect
}

@HiltViewModel
class HomeViewModel @Inject constructor() : BaseViewModel<HomeState, HomeIntent, HomeEffect>(HomeState) {

    override fun onIntent(intent: HomeIntent) {
        viewModelScope.launch {
            when (intent) {
                HomeIntent.OnHostClick -> sendEffect(HomeEffect.NavigateToHost)
                HomeIntent.OnClientClick -> sendEffect(HomeEffect.NavigateToClient)
            }
        }
    }
}
