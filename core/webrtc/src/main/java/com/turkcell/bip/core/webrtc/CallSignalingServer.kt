package com.turkcell.bip.core.webrtc

import com.google.gson.Gson
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallSignalingServer @Inject constructor(private val gson: Gson) {

    private var server: WebSocketServer? = null
    private var connectedClient: WebSocket? = null
    private var _port: Int = 3015
    val port: Int get() = _port

    fun start(listener: ServerListener) {
        if (server != null) return
        createAndStartServer(listener)
    }

    private fun createAndStartServer(listener: ServerListener) {
        server = object : WebSocketServer(InetSocketAddress(port)) {
            override fun onOpen(conn: WebSocket?, handshake: ClientHandshake?) {
                connectedClient = conn
                listener.onClientConnected()
            }

            override fun onClose(conn: WebSocket?, code: Int, reason: String?, remote: Boolean) {
                if (conn == connectedClient) {
                    connectedClient = null
                    listener.onClientDisconnected()
                }
            }

            override fun onMessage(conn: WebSocket?, message: String?) {
                runCatching {
                    gson.fromJson(message, SignalingMessage::class.java)
                }.onSuccess { listener.onMessage(it) }
            }

            override fun onError(conn: WebSocket?, ex: Exception?) {
                if (ex?.message?.contains("Address already in use") == true) {
                    this@CallSignalingServer._port++
                    stop()
                    createAndStartServer(listener)
                }
            }

            override fun onStart() {
                listener.onStarted(port)
            }
        }.apply { start() }
    }

    fun send(message: SignalingMessage) {
        runCatching { connectedClient?.send(gson.toJson(message)) }
    }

    fun disconnectClient() {
        runCatching { connectedClient?.close() }
        connectedClient = null
    }

    fun stop() {
        runCatching {
            server?.stop()
            server = null
        }
    }

    interface ServerListener {
        fun onStarted(port: Int)
        fun onClientConnected()
        fun onClientDisconnected()
        fun onMessage(message: SignalingMessage)
    }
}
