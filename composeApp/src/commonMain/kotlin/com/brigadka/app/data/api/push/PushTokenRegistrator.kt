// In composeApp/src/commonMain/kotlin/com/brigadka/app/data/repository/PushNotificationRepository.kt
package com.brigadka.app.data.api.push

import com.brigadka.app.data.api.BrigadkaApiServiceAuthorized
import com.brigadka.app.data.api.models.RegisterPushTokenRequest
import com.brigadka.app.data.api.models.UnregisterPushTokenRequest
import com.brigadka.app.getPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface PushTokenRegistrator {
    fun registerPushToken(token: String)
    fun unregisterPushToken(token: String)
}

class PushTokenRegistratorImpl(
    private val apiService: BrigadkaApiServiceAuthorized,
    private val deviceIdProvider: DeviceIdProvider,
) : PushTokenRegistrator {

    override fun registerPushToken(token: String) {
        val deviceId = deviceIdProvider.getDeviceId()
        val platform = getPlatform().name

        val request = RegisterPushTokenRequest(
            device_id = deviceId,
            platform = platform,
            token = token
        )

        CoroutineScope(Dispatchers.Default).launch {
            try{
                apiService.registerPushToken(request)
            } catch (e: Exception) {
                // TODO: handler error
            }
        }
    }

    override fun unregisterPushToken(token: String) {
        val request = UnregisterPushTokenRequest(
            token = token,
        )

        CoroutineScope(Dispatchers.Default).launch {
            try {
                apiService.unregisterPushToken(request)
            } catch (e: Exception) {
                // TODO: Handle error if needed
            }
        }
    }
}

// Device ID provider interface and implementation
interface DeviceIdProvider {
    fun getDeviceId(): String
}