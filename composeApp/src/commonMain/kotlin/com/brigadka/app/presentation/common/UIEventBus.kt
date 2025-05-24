package com.brigadka.app.presentation.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface UIEventEmitter {
    /**
     * Emits a UI event to the bus
     */
    suspend fun emit(event: UIEvent)
}

interface UIEventFlowProvider {
    /**
     * Provides a flow of UI events
     */
    val events: SharedFlow<UIEvent>
}

/**
 * Singleton that serves as an event bus for UI events across the application
 */
class UIEventBus: UIEventEmitter, UIEventFlowProvider {
    // Use SharedFlow as a publish-subscribe channel for UI events
    private val _events = MutableSharedFlow<UIEvent>(extraBufferCapacity = 10)
    override val events: SharedFlow<UIEvent> = _events.asSharedFlow()

    // Method to emit events to the bus
    override suspend fun emit(event: UIEvent) {
        _events.emit(event)
    }
}

/**
 * Base interface for UI events
 */
sealed interface UIEvent {
    /**
     * Event for when top bar state needs to be updated
     */
    data class TopBarUpdate(val topBarState: TopBarState) : UIEvent
}

/**
 * Common interface for top bar states from different screens
 */
interface TopBarState