package com.turkcell.bip.feature.home

import androidx.lifecycle.viewModelScope
import com.turkcell.bip.core.common.BaseViewModel
import com.turkcell.bip.core.common.UiEffect
import com.turkcell.bip.core.common.UiIntent
import com.turkcell.bip.core.common.UiState
import com.turkcell.bip.core.webrtc.CallManager
import com.turkcell.bip.core.webrtc.CallState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

data class HomeUiState(
    val localAddress: String = "Sunucu başlatılıyor...",
    val targetAddress: String = "",
    val connectionError: Boolean = false
) : UiState

sealed interface HomeIntent : UiIntent {
    data class OnTargetAddressChanged(val address: String) : HomeIntent
    object OnCallClick : HomeIntent
    object DismissError : HomeIntent
}

sealed interface HomeEffect : UiEffect {
    object NavigateToOutgoingCall : HomeEffect
    object NavigateToIncomingCall : HomeEffect
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val callManager: CallManager
) : BaseViewModel<HomeUiState, HomeIntent, HomeEffect>(HomeUiState()) {

    // Prevents double-navigation when IncomingCall state persists
    private var incomingCallNavigated = false

    init {
        callManager.startServer()
        observeServerAddress()
        observeCallState()
    }

    private fun observeServerAddress() {
        viewModelScope.launch {
            callManager.serverAddress.collect { address ->
                if (address.isNotEmpty()) {
                    setState { copy(localAddress = address) }
                }
            }
        }
    }

    private fun observeCallState() {
        viewModelScope.launch {
            callManager.callState.collect { callState ->
                when (callState) {
                    is CallState.IncomingCall -> {
                        if (!incomingCallNavigated) {
                            incomingCallNavigated = true
                            sendEffect(HomeEffect.NavigateToIncomingCall)
                        }
                    }
                    is CallState.Idle, is CallState.CallEnded -> {
                        incomingCallNavigated = false
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onIntent(intent: HomeIntent) {
        viewModelScope.launch {
            when (intent) {
                is HomeIntent.OnTargetAddressChanged ->
                    setState { copy(targetAddress = intent.address, connectionError = false) }

                HomeIntent.OnCallClick -> {
                    val address = state.value.targetAddress.trim()
                    if (address.isNotBlank()) {
                        callManager.makeCall(address)
                        sendEffect(HomeEffect.NavigateToOutgoingCall)
                    }
                }

                HomeIntent.DismissError ->
                    setState { copy(connectionError = false) }
            }
        }
    }
}
