package com.brigadka.app.presentation.auth.register.verification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brigadka.app.domain.verification.VerificationState

@Composable
fun VerificationScreen(component: VerificationComponent) {
    val state by component.state.collectAsState()
    val cooldown by component.resendCooldown.collectAsState()

    VerificationContent(
        state = state,
        cooldown = cooldown,
        onResendClick = component::onResend,
        onResetClick = component::onReset
    )
}

@Composable
private fun VerificationContent(
    state: VerificationState,
    cooldown: Int,
    onResendClick: () -> Unit,
    onResetClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Подтвердите почту",
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text =  "Мы отправили вам ссылку на почту. Как только вы подтвердите, этот экран закроется сам.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onResendClick,
            modifier = Modifier.fillMaxWidth(0.8f),
            enabled = state != VerificationState.RESENDING && cooldown <= 0
        ) {
            if (state == VerificationState.RESENDING) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                if (cooldown > 0) {
                    Text("Отправить повторно ($cooldown сек)")
                } else {
                    Text("Отправить повторно")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onResetClick,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Вернуться к регистрации")
        }
    }
}

@Composable
fun VerificationScreenPreview() {
    VerificationContent(
        state = VerificationState.NOT_VERIFIED,
        cooldown = 50,
        onResendClick = {},
        onResetClick = {},
    )
}