package com.turkcell.bip.feature.call

import app.cash.turbine.test
import com.turkcell.bip.core.webrtc.CallManager
import com.turkcell.bip.core.webrtc.CallState
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveCallViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val callStateFlow = MutableStateFlow<CallState>(CallState.CallActive)
    private val callManager = mockk<CallManager>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { callManager.callState } returns callStateFlow
        every { callManager.endCall() } just runs
        every { callManager.toggleMute(any()) } just runs
        every { callManager.toggleVideo(any()) } just runs
        every { callManager.switchCamera() } just runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ActiveCallViewModel(callManager)

    @Test
    fun `toggleMute flips isMuted state and calls callManager`() = runTest(testDispatcher) {
        val vm = viewModel()
        assertEquals(false, vm.state.value.isMuted)

        vm.onIntent(ActiveCallIntent.ToggleMute)
        advanceUntilIdle()

        assertTrue(vm.state.value.isMuted)
        verify { callManager.toggleMute(true) }
    }

    @Test
    fun `toggleMute twice restores isMuted to false`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.onIntent(ActiveCallIntent.ToggleMute)
        vm.onIntent(ActiveCallIntent.ToggleMute)
        advanceUntilIdle()

        assertEquals(false, vm.state.value.isMuted)
        verify { callManager.toggleMute(false) }
    }

    @Test
    fun `toggleVideo flips isVideoOff state and calls callManager`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.onIntent(ActiveCallIntent.ToggleVideo)
        advanceUntilIdle()

        assertTrue(vm.state.value.isVideoOff)
        verify { callManager.toggleVideo(true) }
    }

    @Test
    fun `switchCamera calls callManager`() = runTest(testDispatcher) {
        viewModel().onIntent(ActiveCallIntent.SwitchCamera)
        advanceUntilIdle()
        verify { callManager.switchCamera() }
    }

    @Test
    fun `endCall calls callManager endCall`() = runTest(testDispatcher) {
        viewModel().onIntent(ActiveCallIntent.EndCall)
        advanceUntilIdle()
        verify { callManager.endCall() }
    }

    @Test
    fun `callEnded state emits NavigateBack exactly once`() = runTest(testDispatcher) {
        val vm = viewModel()

        vm.effect.receiveAsFlow().test {
            callStateFlow.value = CallState.CallEnded
            advanceUntilIdle()
            assertEquals(ActiveCallEffect.NavigateBack, awaitItem())

            callStateFlow.value = CallState.Idle
            advanceUntilIdle()
            expectNoEvents() // navigationSent guard prevents second effect

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setupLocalSurface delegates to callManager`() = runTest(testDispatcher) {
        val mockSurface = mockk<org.webrtc.SurfaceViewRenderer>(relaxed = true)
        viewModel().onIntent(ActiveCallIntent.SetupLocalSurface(mockSurface))
        advanceUntilIdle()
        verify { callManager.setupLocalSurface(mockSurface) }
    }

    @Test
    fun `setupRemoteSurface delegates to callManager`() = runTest(testDispatcher) {
        val mockSurface = mockk<org.webrtc.SurfaceViewRenderer>(relaxed = true)
        viewModel().onIntent(ActiveCallIntent.SetupRemoteSurface(mockSurface))
        advanceUntilIdle()
        verify { callManager.setupRemoteSurface(mockSurface) }
    }
}
