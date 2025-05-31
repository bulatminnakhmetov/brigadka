package com.brigadka.app.presentation.auth.register.verification

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnStop
import com.arkivanov.essenty.lifecycle.subscribe
import com.brigadka.app.common.coroutineScope
import com.brigadka.app.data.repository.UserRepository
import com.brigadka.app.di.CreateVerificationManager
import com.brigadka.app.di.VerificationManagerFactory
import com.brigadka.app.domain.session.SessionManager
import com.brigadka.app.domain.verification.VerificationManager
import com.brigadka.app.domain.verification.VerificationState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VerificationComponent(
    componentContext: ComponentContext,
    private val sessionManager: SessionManager,
    verificationManagerFactory: VerificationManagerFactory,
): ComponentContext by componentContext {

    private val scope = coroutineScope()
    private val verificationManager: VerificationManager = verificationManagerFactory.create(scope)

    val state: StateFlow<VerificationState> = verificationManager.state

    init {
        lifecycle.subscribe(
            onStop = {
                // Handle any cleanup or stopping tasks here
                verificationManager.stopPolling()
            },
            onStart = {
                // Start any necessary tasks when the component starts
                verificationManager.startPolling()
            }
        )
    }

    fun onResend() {
        scope.launch {
            verificationManager.resendVerification()
        }
    }

    fun onReset() {
        verificationManager.stopPolling()
        sessionManager.logout()
    }
}

