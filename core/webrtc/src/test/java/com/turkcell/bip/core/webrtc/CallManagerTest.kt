package com.turkcell.bip.core.webrtc

import android.content.Context
import android.media.AudioManager
import android.net.wifi.WifiManager
import com.google.gson.Gson
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CallManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val gson = Gson()

    private val context = mockk<Context>(relaxed = true)
    private val signalingServer = mockk<CallSignalingServer>(relaxed = true)
    private val signalingClient = mockk<CallSignalingClient>(relaxed = true)
    private val rtcClient = mockk<BipRtcClient>(relaxed = true)
    private val rtcClientFactory = BipRtcClientFactory { _, _ -> rtcClient }

    private fun callManager() = CallManager(context, signalingServer, signalingClient, gson, rtcClientFactory)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { context.getSystemService(Context.WIFI_SERVICE) } returns mockk<WifiManager>(relaxed = true)
        every { context.getSystemService(Context.AUDIO_SERVICE) } returns mockk<AudioManager>(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startServer calls signalingServer start`() = runTest(testDispatcher) {
        callManager().startServer()
        verify { signalingServer.start(any()) }
    }

    @Test
    fun `makeCall emits OutgoingCall state`() = runTest(testDispatcher) {
        val cm = callManager()
        cm.makeCall("192.168.1.10:3015")
        assertEquals(CallState.OutgoingCall("192.168.1.10:3015"), cm.callState.value)
    }

    @Test
    fun `makeCall connects signalingClient`() = runTest(testDispatcher) {
        callManager().makeCall("192.168.1.10:3015")
        verify { signalingClient.connect("192.168.1.10:3015", any()) }
    }

    @Test
    fun `acceptCall sends CALL_ACCEPT via server`() = runTest(testDispatcher) {
        callManager().acceptCall()
        verify { signalingServer.send(SignalingMessage(SignalingMessageType.CALL_ACCEPT)) }
    }

    @Test
    fun `rejectCall sends CALL_REJECT and emits Idle`() = runTest(testDispatcher) {
        val cm = callManager()
        cm.rejectCall()
        verify { signalingServer.send(SignalingMessage(SignalingMessageType.CALL_REJECT)) }
        assertEquals(CallState.Idle, cm.callState.value)
    }

    @Test
    fun `endCall sends END_CALL message`() = runTest(testDispatcher) {
        val cm = callManager()
        cm.makeCall("192.168.1.10:3015")
        cm.endCall()
        verify { signalingClient.send(SignalingMessage(SignalingMessageType.END_CALL)) }
    }

    @Test
    fun `onServerMessage CALL_REQUEST emits IncomingCall`() = runTest(testDispatcher) {
        val listenerSlot = slot<CallSignalingServer.ServerListener>()
        every { signalingServer.start(capture(listenerSlot)) } just runs

        val cm = callManager()
        cm.startServer()

        listenerSlot.captured.onMessage(SignalingMessage(SignalingMessageType.CALL_REQUEST))
        advanceUntilIdle()

        assertEquals(CallState.IncomingCall(""), cm.callState.value)
    }

    @Test
    fun `onClientMessage CALL_ACCEPT emits CallActive`() = runTest(testDispatcher) {
        val listenerSlot = slot<CallSignalingClient.ClientListener>()
        every { signalingClient.connect(any(), capture(listenerSlot)) } just runs

        val cm = callManager()
        cm.makeCall("192.168.1.10:3015")

        listenerSlot.captured.onMessage(SignalingMessage(SignalingMessageType.CALL_ACCEPT))
        advanceUntilIdle()

        assertEquals(CallState.CallActive, cm.callState.value)
    }

    @Test
    fun `onClientMessage CALL_REJECT emits Idle`() = runTest(testDispatcher) {
        val listenerSlot = slot<CallSignalingClient.ClientListener>()
        every { signalingClient.connect(any(), capture(listenerSlot)) } just runs

        val cm = callManager()
        cm.makeCall("192.168.1.10:3015")

        listenerSlot.captured.onMessage(SignalingMessage(SignalingMessageType.CALL_REJECT))
        advanceUntilIdle()

        assertEquals(CallState.Idle, cm.callState.value)
    }
}
