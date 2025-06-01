package com.brigadka.app.domain.push

import co.touchlab.kermit.Logger
import com.brigadka.app.data.api.BrigadkaApiService
import com.brigadka.app.data.api.models.RegisterPushTokenRequest
import com.brigadka.app.data.api.models.UnregisterPushTokenRequest
import com.brigadka.app.data.repository.PushTokenRepository
import com.brigadka.app.data.repository.UserRepository
import com.brigadka.app.domain.session.SessionManager
import com.brigadka.app.getPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private val logger = Logger.withTag("PushTokenRegistrationManager")

interface PushTokenRegistrationManager

class PushTokenRegistrationManagerImpl(
    private val coroutineScope: CoroutineScope,
    private val userRepository: UserRepository,
    sessionManager: SessionManager,
    private val apiService: BrigadkaApiService,
    private val pushTokenRepository: PushTokenRepository,
    private val deviceIdProvider: DeviceIdProvider,
) : PushTokenRegistrationManager {

    init {
        coroutineScope.launch {
            userRepository.isVerified.combine(pushTokenRepository.token) { loggingState, token -> loggingState to token }
                .collect { (isVerified, token) ->
                    logger.d("Received token: $token, loggingState: $isVerified")

                    if (token != null && isVerified) {
                        logger.d("Registering push token")
                        registerPushToken(token)
                    }
                }
        }
        sessionManager.registerLogoutObserver {
            val token = pushTokenRepository.token.value
            if (token != null && userRepository.isVerified.value) {
                logger.d("Unregistering push token")
                unregisterPushToken(token)
            }
        }
    }

    fun registerPushToken(token: String) {
        val deviceId = deviceIdProvider.getDeviceId()
        val platform = getPlatform().name

        val request = RegisterPushTokenRequest(
            device_id = deviceId,
            platform = platform,
            token = token
        )

        coroutineScope.launch {
            try{
                apiService.registerPushToken(request)
                logger.d("Push token registered successfully: $token")
            } catch (e: Exception) {
                logger.e("Failed to register push token: $token", e)
                // TODO: handler error
            }
        }
    }

    fun unregisterPushToken(token: String) {
        val request = UnregisterPushTokenRequest(
            token = token,
        )

        coroutineScope.launch {
            try {
                apiService.unregisterPushToken(request)
                logger.d("Push token unregistered successfully: $token")
            } catch (e: Exception) {
                logger.e("Failed to unregister push token: $token", e)
                // TODO: Handle error if needed
            }
        }
    }
}

// Device ID provider interface and implementation
interface DeviceIdProvider {
    fun getDeviceId(): String
}