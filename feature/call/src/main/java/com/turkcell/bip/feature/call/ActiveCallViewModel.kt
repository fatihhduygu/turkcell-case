package com.turkcell.bip.feature.call

import androidx.lifecycle.viewModelScope
import com.turkcell.bip.core.common.BaseViewModel
import com.turkcell.bip.core.common.UiEffect
import com.turkcell.bip.core.common.UiIntent
import com.turkcell.bip.core.common.UiState
import com.turkcell.bip.core.webrtc.CallManager
import com.turkcell.bip.core.webrtc.CallState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

data class ActiveCallUiState(
    val isMuted: Boolean = false,
    val isVideoOff: Boolean = false
) : UiState

sealed interface ActiveCallIntent : UiIntent {
    object ToggleMute : ActiveCallIntent
    object ToggleVideo : ActiveCallIntent
    object SwitchCamera : ActiveCallIntent
    object EndCall : ActiveCallIntent
    data class SetupLocalSurface(val surface: SurfaceViewRenderer) : ActiveCallIntent
    data class SetupRemoteSurface(val surface: SurfaceViewRenderer) : ActiveCallIntent
}

sealed interface ActiveCallEffect : UiEffect {
    object NavigateBack : ActiveCallEffect
}

@HiltViewModel
class ActiveCallViewModel @Inject constructor(
    private val callManager: CallManager
) : BaseViewModel<ActiveCallUiState, ActiveCallIntent, ActiveCallEffect>(ActiveCallUiState()) {

    // Ensures NavigateBack fires exactly once regardless of who triggers it
    private var navigationSent = false

    init {
        observeCallState()
    }

    private fun observeCallState() {
        viewModelScope.launch {
            callManager.callState.collect { state ->
                if (state is CallState.CallEnded || state is CallState.Idle) {
                    navigateBack()
                }
            }
        }
    }

    private suspend fun navigateBack() {
        if (!navigationSent) {
            navigationSent = true
            sendEffect(ActiveCallEffect.NavigateBack)
        }
    }

    override fun onIntent(intent: ActiveCallIntent) {
        when (intent) {
            is ActiveCallIntent.SetupRemoteSurface -> callManager.setupRemoteSurface(intent.surface)
            is ActiveCallIntent.SetupLocalSurface -> callManager.setupLocalSurface(intent.surface)

            ActiveCallIntent.ToggleMute -> {
                val muted = !state.value.isMuted
                setState { copy(isMuted = muted) }
                callManager.toggleMute(muted)
            }
            ActiveCallIntent.ToggleVideo -> {
                val videoOff = !state.value.isVideoOff
                setState { copy(isVideoOff = videoOff) }
                callManager.toggleVideo(videoOff)
            }
            ActiveCallIntent.SwitchCamera -> callManager.switchCamera()

            ActiveCallIntent.EndCall -> {
                callManager.endCall()
                // navigateBack() fires when state becomes CallEnded via observeCallState
            }
        }
    }
}
