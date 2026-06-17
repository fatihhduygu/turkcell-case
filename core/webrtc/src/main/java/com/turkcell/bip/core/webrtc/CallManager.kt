package com.turkcell.bip.core.webrtc

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CallManager"

@Singleton
class CallManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val signalingServer: CallSignalingServer,
    private val signalingClient: CallSignalingClient,
    private val gson: Gson
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val application = context as Application

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _serverAddress = MutableStateFlow("")
    val serverAddress: StateFlow<String> = _serverAddress.asStateFlow()

    private var isHost = false

    // Remote video state — both fields managed on Main thread
    private var remoteSurface: SurfaceViewRenderer? = null
    private var remoteVideoTrack: VideoTrack? = null

    // ICE candidates queued before remote SDP is set
    private val pendingIceCandidates = mutableListOf<IceCandidate>()
    private var remoteDescriptionSet = false

    val localIpAddress: String get() = getLocalIpAddress(context) ?: "0.0.0.0"

    private val rtcClient: BipRtcClient by lazy {
        BipRtcClient(
            application = application,
            observer = object : BipPeerConnectionObserver() {
                override fun onIceCandidate(p0: IceCandidate?) {
                    super.onIceCandidate(p0)
                    p0 ?: return
                    rtcClient.addIceCandidate(p0)
                    val msg = SignalingMessage(SignalingMessageType.ICE, gson.toJson(p0))
                    sendSignalingMessage(msg)
                }

                override fun onAddStream(p0: MediaStream?) {
                    super.onAddStream(p0)
                    Log.d(TAG, "onAddStream: ${p0?.videoTracks?.size} video tracks, ${p0?.audioTracks?.size} audio tracks")
                    scope.launch {
                        remoteVideoTrack = p0?.videoTracks?.getOrNull(0)
                        remoteSurface?.let { surface ->
                            remoteVideoTrack?.addSink(surface)
                        }
                    }
                }

                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                    Log.d(TAG, "PeerConnection state: $newState")
                    if (newState == PeerConnection.PeerConnectionState.DISCONNECTED ||
                        newState == PeerConnection.PeerConnectionState.CLOSED
                    ) {
                        scope.launch { _callState.emit(CallState.CallEnded) }
                    }
                }
            },
            onSendSignalingMessage = { sendSignalingMessage(it) }
        )
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    fun startServer() {
        signalingServer.start(object : CallSignalingServer.ServerListener {
            override fun onStarted(port: Int) {
                Log.d(TAG, "Server started — $localIpAddress:$port")
                scope.launch { _serverAddress.emit("$localIpAddress:$port") }
            }

            override fun onClientConnected() {
                Log.d(TAG, "Client connected to server")
            }

            override fun onClientDisconnected() {
                Log.d(TAG, "Client disconnected from server")
                scope.launch {
                    val current = _callState.value
                    if (current is CallState.CallActive || current is CallState.IncomingCall) {
                        _callState.emit(CallState.CallEnded)
                    }
                }
            }

            override fun onMessage(message: SignalingMessage) {
                scope.launch { handleServerMessage(message) }
            }
        })
    }

    fun makeCall(serverAddress: String) {
        isHost = false
        scope.launch { _callState.emit(CallState.OutgoingCall(serverAddress)) }
        signalingClient.connect(serverAddress, object : CallSignalingClient.ClientListener {
            override fun onConnected() {
                Log.d(TAG, "Connected to $serverAddress — sending CALL_REQUEST")
                signalingClient.send(SignalingMessage(SignalingMessageType.CALL_REQUEST))
            }

            override fun onDisconnected() {
                scope.launch {
                    val current = _callState.value
                    if (current !is CallState.Idle && current !is CallState.CallEnded) {
                        _callState.emit(CallState.CallEnded)
                    }
                }
            }

            override fun onMessage(message: SignalingMessage) {
                scope.launch { handleClientMessage(message) }
            }

            override fun onError() {
                Log.e(TAG, "Socket client error")
                scope.launch { _callState.emit(CallState.Idle) }
            }
        })
    }

    fun acceptCall() {
        isHost = true
        signalingServer.send(SignalingMessage(SignalingMessageType.CALL_ACCEPT))
        scope.launch {
            rtcClient.initializeLocalStream()
            setAudioMode(true)
        }
    }

    fun rejectCall() {
        signalingServer.send(SignalingMessage(SignalingMessageType.CALL_REJECT))
        signalingServer.disconnectClient()
        scope.launch { _callState.emit(CallState.Idle) }
    }

    fun endCall() {
        sendSignalingMessage(SignalingMessage(SignalingMessageType.END_CALL))
        scope.launch { cleanupCall() }
    }

    fun setupLocalSurface(surface: SurfaceViewRenderer) {
        rtcClient.attachLocalSurface(surface)
    }

    fun setupRemoteSurface(surface: SurfaceViewRenderer) {
        remoteSurface = surface
        rtcClient.initRemoteSurface(surface)
        // If remote track arrived before surface was ready, attach now
        remoteVideoTrack?.addSink(surface)
    }

    fun toggleMute(muted: Boolean) = rtcClient.toggleAudio(!muted)
    fun toggleVideo(paused: Boolean) = rtcClient.toggleVideo(!paused)
    fun switchCamera() = rtcClient.switchCamera()

    // ─── Signal handlers (always called on Main via scope.launch) ────────────

    private suspend fun handleServerMessage(message: SignalingMessage) {
        Log.d(TAG, "SERVER ← ${message.type}")
        when (message.type) {
            SignalingMessageType.CALL_REQUEST -> {
                _callState.emit(CallState.IncomingCall(""))
            }
            SignalingMessageType.OFFER -> {
                // acceptCall() already started local stream; now process offer and answer
                val sdp = SessionDescription(SessionDescription.Type.OFFER, message.data.toString())
                rtcClient.setRemoteDescription(sdp)
                remoteDescriptionSet = true
                drainPendingIceCandidates()
                rtcClient.answer()
                _callState.emit(CallState.CallActive)
            }
            SignalingMessageType.ICE -> {
                val candidate = parseIceCandidate(message) ?: return
                if (remoteDescriptionSet) {
                    rtcClient.addIceCandidate(candidate)
                } else {
                    Log.d(TAG, "Queuing ICE candidate (remote SDP not yet set)")
                    pendingIceCandidates.add(candidate)
                }
            }
            SignalingMessageType.END_CALL -> cleanupCall()
            else -> {}
        }
    }

    private suspend fun handleClientMessage(message: SignalingMessage) {
        Log.d(TAG, "CLIENT ← ${message.type}")
        when (message.type) {
            SignalingMessageType.CALL_ACCEPT -> {
                rtcClient.initializeLocalStream()
                setAudioMode(true)
                rtcClient.call()
                _callState.emit(CallState.CallActive)
            }
            SignalingMessageType.CALL_REJECT -> {
                signalingClient.disconnect()
                _callState.emit(CallState.Idle)
            }
            SignalingMessageType.ANSWER -> {
                val sdp = SessionDescription(SessionDescription.Type.ANSWER, message.data.toString())
                rtcClient.setRemoteDescription(sdp)
                remoteDescriptionSet = true
                drainPendingIceCandidates()
            }
            SignalingMessageType.ICE -> {
                val candidate = parseIceCandidate(message) ?: return
                if (remoteDescriptionSet) {
                    rtcClient.addIceCandidate(candidate)
                } else {
                    Log.d(TAG, "Queuing ICE candidate (remote SDP not yet set)")
                    pendingIceCandidates.add(candidate)
                }
            }
            SignalingMessageType.END_CALL -> cleanupCall()
            else -> {}
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun drainPendingIceCandidates() {
        if (pendingIceCandidates.isEmpty()) return
        Log.d(TAG, "Draining ${pendingIceCandidates.size} pending ICE candidates")
        pendingIceCandidates.forEach { rtcClient.addIceCandidate(it) }
        pendingIceCandidates.clear()
    }

    private fun parseIceCandidate(message: SignalingMessage): IceCandidate? =
        runCatching { gson.fromJson(message.data.toString(), IceCandidate::class.java) }
            .getOrNull()

    private fun cleanupCall() {
        Log.d(TAG, "Cleaning up call")
        remoteVideoTrack?.let { track ->
            remoteSurface?.let { track.removeSink(it) }
        }
        remoteVideoTrack = null
        remoteSurface = null
        pendingIceCandidates.clear()
        remoteDescriptionSet = false
        isHost = false
        runCatching { rtcClient.endCall() }
        runCatching { rtcClient.resetForNewCall() }
        signalingClient.disconnect()
        setAudioMode(false)
        scope.launch { _callState.emit(CallState.CallEnded) }
    }

    private fun sendSignalingMessage(message: SignalingMessage) {
        if (isHost) signalingServer.send(message) else signalingClient.send(message)
    }

    private fun setAudioMode(inCall: Boolean) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        @Suppress("DEPRECATION")
        audioManager.mode = if (inCall) AudioManager.MODE_IN_COMMUNICATION else AudioManager.MODE_NORMAL
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = inCall
    }
}
