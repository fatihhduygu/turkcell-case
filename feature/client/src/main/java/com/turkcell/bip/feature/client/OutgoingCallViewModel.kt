package com.turkcell.bip.feature.client

import androidx.lifecycle.viewModelScope
import com.turkcell.bip.core.common.BaseViewModel
import com.turkcell.bip.core.common.UiEffect
import com.turkcell.bip.core.common.UiIntent
import com.turkcell.bip.core.common.UiState
import com.turkcell.bip.core.webrtc.CallManager
import com.turkcell.bip.core.webrtc.CallState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OutgoingCallUiState(val targetAddress: String = "") : UiState

sealed interface OutgoingCallIntent : UiIntent {
    object Cancel : OutgoingCallIntent
}

sealed interface OutgoingCallEffect : UiEffect {
    object NavigateToActiveCall : OutgoingCallEffect
    object NavigateBack : OutgoingCallEffect
}

@HiltViewModel
class OutgoingCallViewModel @Inject constructor(
    private val callManager: CallManager
) : BaseViewModel<OutgoingCallUiState, OutgoingCallIntent, OutgoingCallEffect>(OutgoingCallUiState()) {

    // Guard: only react to terminal states after the outgoing call was actually in progress
    private var callInProgress = false

    init {
        observeCallState()
    }

    private fun observeCallState() {
        viewModelScope.launch {
            callManager.callState.collect { state ->
                when (state) {
                    is CallState.OutgoingCall -> {
                        callInProgress = true
                        setState { copy(targetAddress = state.targetAddress) }
                    }
                    CallState.CallActive -> {
                        if (callInProgress) sendEffect(OutgoingCallEffect.NavigateToActiveCall)
                    }
                    CallState.Idle, CallState.CallEnded -> {
                        if (callInProgress) {
                            callInProgress = false
                            sendEffect(OutgoingCallEffect.NavigateBack)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onIntent(intent: OutgoingCallIntent) {
        viewModelScope.launch {
            when (intent) {
                OutgoingCallIntent.Cancel -> {
                    callInProgress = false
                    callManager.endCall()
                    sendEffect(OutgoingCallEffect.NavigateBack)
                }
            }
        }
    }
}
