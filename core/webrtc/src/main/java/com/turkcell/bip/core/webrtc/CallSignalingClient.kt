package com.turkcell.bip.core.webrtc

import com.google.gson.Gson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallSignalingClient @Inject constructor(private val gson: Gson) {

    private var webSocket: WebSocketClient? = null

    fun connect(address: String, listener: ClientListener) {
        webSocket = object : WebSocketClient(URI("ws://$address")) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                listener.onConnected()
            }

            override fun onMessage(message: String?) {
                runCatching {
                    gson.fromJson(message, SignalingMessage::class.java)
                }.onSuccess { listener.onMessage(it) }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                listener.onDisconnected()
            }

            override fun onError(ex: Exception?) {
                listener.onError()
            }
        }
        webSocket?.connect()
    }

    fun send(message: SignalingMessage) {
        runCatching { webSocket?.send(gson.toJson(message)) }
    }

    fun disconnect() {
        runCatching {
            webSocket?.close()
            webSocket = null
        }
    }

    interface ClientListener {
        fun onConnected()
        fun onDisconnected()
        fun onMessage(message: SignalingMessage)
        fun onError()
    }
}
