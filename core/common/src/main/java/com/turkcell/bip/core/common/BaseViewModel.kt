package com.turkcell.bip.core.common

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class BaseViewModel<S : UiState, I : UiIntent, E : UiEffect>(
    initialState: S
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    val effect: Channel<E> = Channel(Channel.BUFFERED)

    protected fun setState(reducer: S.() -> S) {
        _state.value = _state.value.reducer()
    }

    protected suspend fun sendEffect(e: E) {
        effect.send(e)
    }

    abstract fun onIntent(intent: I)
}
