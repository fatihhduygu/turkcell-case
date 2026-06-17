package com.turkcell.bip.core.webrtc

data class SignalingMessage(
    val type: SignalingMessageType,
    val data: Any? = null
)
