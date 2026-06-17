package com.turkcell.bip.core.webrtc

import org.webrtc.PeerConnection

fun interface BipRtcClientFactory {
    fun create(
        observer: PeerConnection.Observer,
        onSendSignalingMessage: (SignalingMessage) -> Unit
    ): BipRtcClient
}
