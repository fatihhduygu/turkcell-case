package com.turkcell.bip.feature.splash

import androidx.lifecycle.viewModelScope
import com.turkcell.bip.core.common.BaseViewModel
import com.turkcell.bip.core.common.UiEffect
import com.turkcell.bip.core.common.UiIntent
import com.turkcell.bip.core.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data object SplashState : UiState

sealed interface SplashIntent : UiIntent

sealed interface SplashEffect : UiEffect {
    data object NavigateToHome : SplashEffect
}

@HiltViewModel
class SplashViewModel @Inject constructor() : BaseViewModel<SplashState, SplashIntent, SplashEffect>(SplashState) {

    init {
        viewModelScope.launch {
            delay(5_000)
            sendEffect(SplashEffect.NavigateToHome)
        }
    }

    override fun onIntent(intent: SplashIntent) = Unit
}
