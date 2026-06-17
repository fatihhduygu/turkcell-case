package com.turkcell.bip.core.webrtc

import android.app.Application
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

class BipRtcClient(
    private val application: Application,
    private val observer: PeerConnection.Observer,
    private val onSendSignalingMessage: (SignalingMessage) -> Unit
) {
    val eglBase: EglBase = EglBase.create()
    val eglBaseContext: EglBase.Context get() = eglBase.eglBaseContext

    private val mediaConstraints = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
    }

    private val peerConnectionFactory: PeerConnectionFactory
    private val localVideoSource: VideoSource
    private val localAudioSource: AudioSource

    // var so we can close and recreate for multiple calls
    private var peerConnection: PeerConnection? = null

    private var videoCapturer: CameraVideoCapturer? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var localStreamInitialized = false

    init {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(application)
                .setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
                .createInitializationOptions()
        )

        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = true
                disableNetworkMonitor = true
            })
            .createPeerConnectionFactory()

        localVideoSource = peerConnectionFactory.createVideoSource(false)
        localAudioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        peerConnection = createNewPeerConnection()
    }

    private fun createNewPeerConnection(): PeerConnection? =
        peerConnectionFactory.createPeerConnection(emptyList(), observer)

    fun initializeLocalStream() {
        if (localStreamInitialized) return
        localStreamInitialized = true

        val surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
        videoCapturer = getFrontCamera()
        videoCapturer?.initialize(surfaceTextureHelper, application, localVideoSource.capturerObserver)
        videoCapturer?.startCapture(1280, 720, 30)

        localVideoTrack = peerConnectionFactory.createVideoTrack("local_video", localVideoSource)
        localAudioTrack = peerConnectionFactory.createAudioTrack("local_audio", localAudioSource)

        val localStream = peerConnectionFactory.createLocalMediaStream("local_stream")
        localStream.addTrack(localVideoTrack)
        localStream.addTrack(localAudioTrack)
        peerConnection?.addStream(localStream)
    }

    fun attachLocalSurface(surface: SurfaceViewRenderer) {
        surface.setEnableHardwareScaler(true)
        surface.setMirror(true)
        surface.init(eglBase.eglBaseContext, null)
        localVideoTrack?.addSink(surface)
    }

    fun initRemoteSurface(surface: SurfaceViewRenderer) {
        surface.setEnableHardwareScaler(true)
        surface.init(eglBase.eglBaseContext, null)
    }

    private fun getFrontCamera(): CameraVideoCapturer {
        return Camera2Enumerator(application).run {
            deviceNames.find { isFrontFacing(it) }?.let { createCapturer(it, null) }
                ?: deviceNames.firstOrNull()?.let { createCapturer(it, null) }
                ?: throw IllegalStateException("No camera available")
        }
    }

    fun call() {
        peerConnection?.createOffer(object : BipSdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(object : BipSdpObserver() {
                    override fun onSetSuccess() {
                        onSendSignalingMessage(
                            SignalingMessage(SignalingMessageType.OFFER, desc?.description)
                        )
                    }
                }, desc)
            }
        }, mediaConstraints)
    }

    fun answer() {
        peerConnection?.createAnswer(object : BipSdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                peerConnection?.setLocalDescription(object : BipSdpObserver() {
                    override fun onSetSuccess() {
                        onSendSignalingMessage(
                            SignalingMessage(SignalingMessageType.ANSWER, desc?.description)
                        )
                    }
                }, desc)
            }
        }, mediaConstraints)
    }

    fun setRemoteDescription(sdp: SessionDescription) {
        peerConnection?.setRemoteDescription(BipSdpObserver(), sdp)
    }

    fun addIceCandidate(candidate: IceCandidate?) {
        peerConnection?.addIceCandidate(candidate)
    }

    fun toggleAudio(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }

    fun toggleVideo(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }

    fun switchCamera() {
        videoCapturer?.switchCamera(null)
    }

    fun endCall() {
        runCatching { peerConnection?.close() }
    }

    /** Prepare for a new call after the previous one ended. */
    fun resetForNewCall() {
        runCatching { peerConnection?.close() }
        runCatching { localVideoTrack?.dispose() }
        runCatching { localAudioTrack?.dispose() }
        runCatching { videoCapturer?.stopCapture() }
        runCatching { videoCapturer?.dispose() }
        videoCapturer = null
        localVideoTrack = null
        localAudioTrack = null
        localStreamInitialized = false
        // Create a fresh peer connection with the same factory
        peerConnection = createNewPeerConnection()
    }

    fun dispose() {
        runCatching {
            localVideoTrack?.dispose()
            localAudioTrack?.dispose()
            videoCapturer?.stopCapture()
            videoCapturer?.dispose()
            peerConnection?.close()
            localVideoSource.dispose()
            localAudioSource.dispose()
            peerConnectionFactory.dispose()
            eglBase.release()
        }
    }
}
