package com.brigadka.app.presentation.onboarding.basic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.brigadka.app.presentation.common.DatePickerField

@Composable
fun BasicInfoScreen(component: BasicInfoComponent) {
    val state by component.profileData.subscribeAsState()
    val cities by component.cities.subscribeAsState()
    val genders by component.genders.subscribeAsState()
    val scrollState = rememberScrollState()

    // For city selection
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var citySearchQuery by remember { mutableStateOf("") }
    val currentCityName = remember(state.cityId, cities) {
        cities.find { it.id == state.cityId }?.name ?: ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Tell us about yourself",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = state.fullName,
            onValueChange = { component.updateFullName(it) },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth()
        )

        // Birthday selection
        Text("Birthday")
        DatePickerField(
            label = "Birthday",
            onDateSelected = {
                component.updateBirthday(it)
            },
        )

        // Gender selection from API
        Text("Gender")
        if (genders.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                genders.forEach { gender ->
                    FilterChip(
                        selected = state.gender == gender.code,
                        onClick = { component.updateGender(gender.code) },
                        label = { Text(gender.label) }
                    )
                }
            }
        } else {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }

        // City selection with dropdown
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = currentCityName.ifEmpty { citySearchQuery },
                onValueChange = {
                    citySearchQuery = it
                    isDropdownExpanded = true
                },
                label = { Text("City") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { isDropdownExpanded = !isDropdownExpanded }) {
                        Icon(
                            imageVector = if (isDropdownExpanded)
                                androidx.compose.material.icons.Icons.Filled.KeyboardArrowUp
                            else
                                androidx.compose.material.icons.Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Toggle dropdown"
                        )
                    }
                }
            )

            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                val filteredCities = cities.filter {
                    it.name.contains(citySearchQuery, ignoreCase = true)
                }

                filteredCities.forEach { city ->
                    DropdownMenuItem(
                        onClick = {
                            component.updateCityId(city.id)
                            isDropdownExpanded = false
                        },
                        text = { Text(city.name) }
                    )
                }

                if (filteredCities.isEmpty() && cities.isNotEmpty()) {
                    DropdownMenuItem(
                        onClick = { },
                        text = { Text("No matching cities") },
                        enabled = false
                    )
                }

                if (cities.isEmpty()) {
                    DropdownMenuItem(
                        onClick = { },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Loading cities...")
                            }
                        },
                        enabled = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { component.next() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            enabled = component.isCompleted
        ) {
            Text("Continue")
        }
    }
}