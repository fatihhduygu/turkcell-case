package com.turkcell.bip.core.webrtc

sealed class CallState {
    object Idle : CallState()
    data class IncomingCall(val callerAddress: String) : CallState()
    data class OutgoingCall(val targetAddress: String) : CallState()
    object CallActive : CallState()
    object CallEnded : CallState()
}
