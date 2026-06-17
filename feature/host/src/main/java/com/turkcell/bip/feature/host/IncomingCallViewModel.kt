package com.turkcell.bip.feature.host

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

data object IncomingCallUiState : UiState

sealed interface IncomingCallIntent : UiIntent {
    object Accept : IncomingCallIntent
    object Reject : IncomingCallIntent
}

sealed interface IncomingCallEffect : UiEffect {
    object NavigateToActiveCall : IncomingCallEffect
    object NavigateBack : IncomingCallEffect
}

@HiltViewModel
class IncomingCallViewModel @Inject constructor(
    private val callManager: CallManager
) : BaseViewModel<IncomingCallUiState, IncomingCallIntent, IncomingCallEffect>(IncomingCallUiState) {

    // Guard: only react after user has accepted the call
    private var callAccepted = false

    init {
        observeCallState()
    }

    private fun observeCallState() {
        viewModelScope.launch {
            callManager.callState.collect { state ->
                when (state) {
                    CallState.CallActive -> {
                        if (callAccepted) sendEffect(IncomingCallEffect.NavigateToActiveCall)
                    }
                    CallState.Idle, CallState.CallEnded -> {
                        // Remote hung up before we answered
                        if (!callAccepted) sendEffect(IncomingCallEffect.NavigateBack)
                    }
                    else -> {}
                }
            }
        }
    }

    override fun onIntent(intent: IncomingCallIntent) {
        viewModelScope.launch {
            when (intent) {
                IncomingCallIntent.Accept -> {
                    callAccepted = true
                    callManager.acceptCall()
                    // We'll navigate when we receive CallActive via state observer
                }
                IncomingCallIntent.Reject -> {
                    callManager.rejectCall()
                    sendEffect(IncomingCallEffect.NavigateBack)
                }
            }
        }
    }
}
