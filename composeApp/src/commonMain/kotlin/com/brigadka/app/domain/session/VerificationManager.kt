package com.brigadka.app.domain.verification

import com.brigadka.app.data.api.BrigadkaApiService
import com.brigadka.app.data.api.models.ResendVerificationRequest
import com.brigadka.app.data.repository.UserRepository
import com.brigadka.app.domain.session.SessionManager
import com.brigadka.app.presentation.common.UIEvent
import com.brigadka.app.presentation.common.UIEventEmitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

const val VERIFICATION_POLLING_INTERVAL_MS = 3000L // 3 seconds

interface VerificationManager {
    val isPolling: StateFlow<Boolean>
    val state: StateFlow<VerificationState>

    fun startPolling()
    fun stopPolling()
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
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val uiEventEmitter: UIEventEmitter,
    private val pollingIntervalMs: Long = VERIFICATION_POLLING_INTERVAL_MS
) : VerificationManager {

    private var pollingJob: Job? = null
    private val _isPolling = MutableStateFlow(false)
    override val isPolling: StateFlow<Boolean> = _isPolling.asStateFlow()

    private val _state: MutableStateFlow<VerificationState> = MutableStateFlow(VerificationState.NOT_VERIFIED)
    override val state: StateFlow<VerificationState> = _state


    override fun startPolling() {
        if (pollingJob?.isActive == true) return

        pollingJob = coroutineScope.launch {
            _isPolling.value = true

            try {
                while (isActive) {
                    try {
                        val status = apiService.getVerificationStatus()
                        if (status.verified) {
                            sessionManager.logout()
                            uiEventEmitter.emit(UIEvent.Message("Подтверждение успешно выполнено. Теперь вы можете войти!"))
                            stopPolling()
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

    override fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _isPolling.value = false
    }

    override suspend fun resendVerification() {
        try {
            _state.value = VerificationState.RESENDING
            apiService.resendVerification()
            _state.value = VerificationState.NOT_VERIFIED
        } catch (e: Exception) {
            _state.value = VerificationState.RESEND_FAILED
        }
    }
}