package com.brigadka.app.sync

import com.brigadka.app.data.api.push.PushTokenRegistrator
import com.brigadka.app.data.repository.AuthTokenRepository
import com.brigadka.app.data.repository.PushTokenRepository
import com.brigadka.app.data.repository.UserDataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

interface PushTokenRegistrationManager {
    fun start()
}

class PushTokenRegistrationManagerImpl(
    private val userDataRepository: UserDataRepository,
    private val pushTokenRepository: PushTokenRepository,
    private val pushTokenRegistrator: PushTokenRegistrator,
) : PushTokenRegistrationManager {

    override fun start() {
        CoroutineScope(Dispatchers.Default).launch {
            userDataRepository.isLoggedIn
                .combine(pushTokenRepository.token) { isLoggedIn, token ->
                    isLoggedIn to token
                }
                .collect { (isLoggedIn, token) ->
                    if (token != null) {
                        if (isLoggedIn) {
                            pushTokenRegistrator.registerPushToken(token)
                        } else {
                            pushTokenRegistrator.unregisterPushToken(token)
                        }
                    }
                }
        }
    }
}