package com.brigadka.app.data.notification

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface PushNotificationService {
    suspend fun requestNotificationPermission()
}