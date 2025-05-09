package com.brigadka.app.presentation.onboarding.improv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImprovInfoScreen(component: ImprovInfoComponent) {
    val state by component.profileData.subscribeAsState()
    val improvGoals by component.improvGoals.collectAsState()
    val improvStyles by component.improvStyles.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Tell us more about yourself",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "This information helps other improvisers know you better",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // TODO: установить ограничения на количество символов
        OutlinedTextField(
            value = state.bio,
            onValueChange = { component.updateBio(it) },
            label = { Text("Bio") },
            placeholder = { Text("Tell us about your background...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your improv goal",
            style = MaterialTheme.typography.titleMedium
        )

        if (improvGoals.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                improvGoals.forEach { goal ->
                    FilterChip(
                        selected = state.goal == goal.code,
                        onClick = { component.updateGoal(goal.code) },
                        label = { Text(goal.label) }
                    )
                }
            }
        } else {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Improv styles you enjoy",
            style = MaterialTheme.typography.titleMedium
        )

        if (improvStyles.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                improvStyles.forEach { style ->
                    FilterChip(
                        selected = style.code in state.improvStyles,
                        onClick = { component.toggleStyle(style.code) },
                        label = { Text(style.label) }
                    )
                }
            }
        } else {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Looking for a team",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Let others know you're available to join a team",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = state.lookingForTeam,
                onCheckedChange = { component.updateLookingForTeam(it) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilledTonalButton(
                onClick = { component.back() },
                modifier = Modifier.weight(1f)
            ) {
                Text("Back")
            }

            Button(
                onClick = { component.next() },
                modifier = Modifier.weight(1f),
                enabled = component.isCompleted
            ) {
                Text("Continue")
            }
        }
    }
}