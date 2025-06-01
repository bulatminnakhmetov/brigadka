package com.brigadka.app.domain.verification

import com.brigadka.app.common.Timer
import com.brigadka.app.data.api.BrigadkaApiService
import com.brigadka.app.domain.session.SessionManager
import com.brigadka.app.presentation.common.UIEvent
import com.brigadka.app.presentation.common.UIEventEmitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

const val VERIFICATION_POLLING_INTERVAL_MS = 3000L // 3 seconds
const val RESEND_COOLDOWN_S = 60 // 1 minute

interface VerificationManager {
    val isPolling: StateFlow<Boolean>
    val state: StateFlow<VerificationState>

    val resendCooldown: StateFlow<Int>

    fun start()
    fun stop()
    suspend fun resendVerification()

}

enum class VerificationState {
    RESENDING,
    NOT_VERIFIED,
    RESEND_FAILED
}

class VerificationManagerImpl(
    private val coroutineScope: CoroutineScope,
    private val apiService: BrigadkaApiService,
    private val sessionManager: SessionManager,
    private val uiEventEmitter: UIEventEmitter,
    private val pollingIntervalMs: Long = VERIFICATION_POLLING_INTERVAL_MS
) : VerificationManager {

    private var pollingJob: Job? = null
    private val _isPolling = MutableStateFlow(false)
    override val isPolling: StateFlow<Boolean> = _isPolling.asStateFlow()

//    private var _lastSendAt: MutableStateFlow<Instant> = MutableStateFlow(Clock.System.now())
//    override val lastSendAt: StateFlow<Instant> = _lastSendAt

    private val _state: MutableStateFlow<VerificationState> = MutableStateFlow(VerificationState.NOT_VERIFIED)
    override val state: StateFlow<VerificationState> = _state

    private var timer: Timer? = null
    override var resendCooldown: StateFlow<Int> = MutableStateFlow(RESEND_COOLDOWN_S)
        private set


    fun updateTimer(lastSendAt: Instant) {
        timer?.stop()
        timer = Timer(start = lastSendAt, scope = coroutineScope)
        resendCooldown = timer?.elapsed
            ?.map { it.inWholeSeconds.toInt() }
            ?.map { (RESEND_COOLDOWN_S - it).coerceAtLeast(0) }
            ?.stateIn(coroutineScope, SharingStarted.Eagerly, RESEND_COOLDOWN_S) ?: MutableStateFlow(RESEND_COOLDOWN_S)
    }

    init {
        updateTimer(Clock.System.now())
    }


    override fun start() {
        timer?.start()
        if (pollingJob?.isActive == true) return

        pollingJob = coroutineScope.launch {
            _isPolling.value = true

            try {
                while (isActive) {
                    try {
                        val status = apiService.getVerificationStatus()
                        if (status.verified) {
                            uiEventEmitter.emit(UIEvent.Message("Успех! Теперь вы можете войти!"))
                            stop()
                            sessionManager.logout()
                            break
                        }
                    } catch (e: Exception) {
                        // Just continue polling even if there's an error
                    }

                    delay(pollingIntervalMs)
                }
            } finally {
                _isPolling.value = false
            }
        }
    }

    override fun stop() {
        pollingJob?.cancel()
        pollingJob = null
        _isPolling.value = false
        timer?.stop()
    }

    override suspend fun resendVerification() {
        try {
            _state.value = VerificationState.RESENDING
            apiService.resendVerification()
            updateTimer(Clock.System.now())
            timer?.start()
            _state.value = VerificationState.NOT_VERIFIED
        } catch (e: Exception) {
            _state.value = VerificationState.RESEND_FAILED
        }
    }
}