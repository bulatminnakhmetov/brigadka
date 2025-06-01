package com.brigadka.app.presentation.auth.register.verification

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.subscribe
import com.brigadka.app.common.coroutineScope
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

    val resendCooldown: StateFlow<Int> = verificationManager.resendCooldown

    init {

        lifecycle.subscribe(
            onStart = {
                // Start any necessary tasks when the component starts
                verificationManager.start()
            },
            onStop = {
                // Handle any cleanup or stopping tasks here
                verificationManager.stop()
            },
        )
    }

    fun onResend() {
        scope.launch {
            verificationManager.resendVerification()
        }
    }

    fun onReset() {
        sessionManager.logout()
    }
}

