package com.brigadka.app.presentation.auth.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(component: LoginComponent) {
    val state by component.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Login to Brigadka",
            style = MaterialTheme.typography.displayLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = state.email,
            onValueChange = component::onEmailChanged,
            label = { Text("Email") },
            isError = state.emailError != null,
            enabled = !state.isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        state.emailError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style =MaterialTheme.typography.displaySmall,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .fillMaxWidth()
                    .align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = state.password,
            onValueChange = component::onPasswordChanged,
            label = { Text("Password") },
            isError = state.passwordError != null,
            enabled = !state.isLoading,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth()
        )

        state.passwordError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style =MaterialTheme.typography.displaySmall,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .fillMaxWidth()
                    .align(Alignment.Start)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        state.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = component::onLoginClick,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Text("Login")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = component::onRegisterClick,
            enabled = !state.isLoading
        ) {
            Text("Don't have an account? Register")
        }
    }
}