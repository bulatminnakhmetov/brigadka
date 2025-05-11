package com.brigadka.app.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brigadka.app.data.repository.ProfileView
import com.brigadka.app.presentation.profile.common.Avatar
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.brigadka.app.data.repository.SearchResult


@Composable
fun SearchScreen(component: SearchComponent) {
    val state by component.state.subscribeAsState()
    var showFilters by remember { mutableStateOf(false) }

    SearchScreen(
        state = state,
        showFilters = showFilters,
        onToggleFilters = { showFilters = !showFilters },
        onUpdateAgeRange = component::updateAgeRange,
        onUpdateCityFilter = component::updateCityFilter,
        onToggleGender = component::toggleGender,
        onToggleGoal = component::toggleGoal,
        onToggleImprovStyle = component::toggleImprovStyle,
        onToggleLookingForTeam = component::toggleLookingForTeam,
        onToggleHasAvatar = component::toggleHasAvatar,
        onToggleHasVideo = component::toggleHasVideo,
        onResetFilters = component::resetFilters,
        onUpdateNameFilter = component::updateNameFilter,
        onSearch = component::performSearch,
        onPreviousPage = component::previousPage,
        onNextPage = component::nextPage,
        onProfileClick = component::onProfileClick
    )
}

@Composable
fun SearchScreenPreview() {
    val profiles = listOf(
        ProfileView(
            userID = 1,
            fullName = "John Doe",
            age = 28,
            genderLabel = "Male",
            cityLabel = "New York",
            bio = "Improv enthusiast.",
            goalLabel = "Have fun",
            improvStylesLabels = listOf("Short Form", "Long Form"),
            lookingForTeam = true,
            avatar = null,
            videos = emptyList()
        ),
        ProfileView(
            userID = 1,
            fullName = "Jane Smith",
            age = 32,
            genderLabel = "Female",
            cityLabel = "Los Angeles",
            bio = "Comedy lover and performer.",
            goalLabel = "Meet new people",
            improvStylesLabels = listOf("Musical", "Short Form"),
            lookingForTeam = false,
            avatar = null,
            videos = emptyList()
        ),
        ProfileView(
            userID = 1,
            fullName = "Alex Johnson",
            age = 25,
            genderLabel = "Non-binary",
            cityLabel = "Chicago",
            bio = "Always up for a laugh.",
            goalLabel = "Improve skills",
            improvStylesLabels = listOf("Long Form"),
            lookingForTeam = true,
            avatar = null,
            videos = emptyList()
        )
    )

    val searchResult = SearchResult(
        profiles = profiles,
        page = 1,
        pageSize = 20,
        totalCount = profiles.size
    )

    val improvStyles = listOf(
        Option("short_form", "Короткая форма", false),
        Option("long_form", "Длинная форма", false),
        Option("musical", "Мюзикл", false),
        Option("musical", "Реп", false),
        Option("musical", "Баттлы", false),
        Option("musical", "Плейбек", false),
    )

    val goals = listOf(
        Option("have_fun", "Have Fun", false),
        Option("meet_people", "Meet People", false),
        Option("improve_skills", "Improve Skills", false)
    )

    val genders = listOf(
        Option("male", "Male", false),
        Option("female", "Female", false),
    )

    val state = SearchState(
        searchResult = searchResult,
        isLoading = false,
        improvStyleFilter = improvStyles,
        goalFilter = goals,
        genderFilter = genders,
    )

    SearchScreen(
        state = state,
        showFilters = true,
        onToggleFilters = { },
        onUpdateAgeRange = { _, _ -> },
        onUpdateCityFilter = { },
        onToggleGender = { },
        onToggleGoal = { },
        onToggleImprovStyle = { },
        onToggleLookingForTeam = { },
        onToggleHasAvatar = { },
        onToggleHasVideo = { },
        onResetFilters = { },
        onUpdateNameFilter = { },
        onSearch = { },
        onPreviousPage = { },
        onNextPage = { },
        onProfileClick = { }
    )
}

@Composable
fun SearchScreen(
    state: SearchState,
    showFilters: Boolean,
    onToggleFilters: () -> Unit,
    onUpdateAgeRange: (min: Int?, max: Int?) -> Unit,
    onUpdateCityFilter: (Int?) -> Unit,
    onToggleGender: (String) -> Unit,
    onToggleGoal: (String) -> Unit,
    onToggleImprovStyle: (String) -> Unit,
    onToggleLookingForTeam: () -> Unit,
    onToggleHasAvatar: () -> Unit,
    onToggleHasVideo: () -> Unit,
    onResetFilters: () -> Unit,
    onUpdateNameFilter: (String) -> Unit,
    onSearch: () -> Unit,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onProfileClick: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            SearchTopBar(
                query = state.nameFilter ?: "",
                onQueryChange = onUpdateNameFilter,
                onSearch = onSearch,
                onToggleFilters = onToggleFilters
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showFilters) {
                SearchFilters(
                    state = state,
                    onAgeRangeChange = onUpdateAgeRange,
                    onCityChange = onUpdateCityFilter,
                    onToggleGender = onToggleGender,
                    onToggleGoal = onToggleGoal,
                    onToggleImprovStyle = onToggleImprovStyle,
                    onLookingForTeamToggle = onToggleLookingForTeam,
                    onHasAvatarToggle = onToggleHasAvatar,
                    onHasVideoToggle = onToggleHasVideo,
                    onReset = onResetFilters,
                )
            }

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                val searchResults = state.searchResult
                if (searchResults != null) {
                    if (searchResults.profiles.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No profiles found matching your criteria",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    } else {
                        Column {
                            Text(
                                text = "Found ${searchResults.totalCount} profiles",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            LazyColumn(
                                modifier = Modifier.weight(1f)
                            ) {
                                items(searchResults.profiles) { profile ->
                                    ProfileCard(profile, onClick = { onProfileClick(profile.userID) })
                                    HorizontalDivider()
                                }
                            }

                            // Pagination controls
                            SearchPagination(
                                currentPage = searchResults.page,
                                totalPages = (searchResults.totalCount / searchResults.pageSize) + 1,
                                onPreviousPage = onPreviousPage,
                                onNextPage = onNextPage
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onToggleFilters: () -> Unit
) {
    TopAppBar(
        title = {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                placeholder = { Text("Search profiles...") },
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            modifier = Modifier.clickable { onQueryChange("") }
                        )
                    }
                }
            )
        },
        actions = {
            IconButton(onClick = onSearch) {
                Icon(Icons.Default.Search, contentDescription = "Search")
            }
            IconButton(onClick = onToggleFilters) {
                Icon(Icons.Default.Menu, contentDescription = "Filters") // TODO: replace with a better icon
            }
        }
    )
}

@Composable
fun FilterLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun SearchFilters(
    state: SearchState,
    onAgeRangeChange: (Int?, Int?) -> Unit,
    onCityChange: (Int?) -> Unit,
    onToggleGender: (String) -> Unit,
    onToggleGoal: (String) -> Unit,
    onToggleImprovStyle: (String) -> Unit,
    onLookingForTeamToggle: () -> Unit,
    onHasAvatarToggle: () -> Unit,
    onHasVideoToggle: () -> Unit,
    onReset: () -> Unit
) {
    var minAgeText by remember { mutableStateOf(state.minAgeFilter?.toString() ?: "") }
    var maxAgeText by remember { mutableStateOf(state.maxAgeFilter?.toString() ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Filters",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Цель",
            style = MaterialTheme.typography.titleSmall
        )

        ChipsFilter(state.goalFilter, onToggleGoal)

        Spacer(modifier = Modifier.height(16.dp))

        FilterLabel("Пол")

        ChipsFilter(state.genderFilter, onToggleGender)

        Spacer(modifier = Modifier.height(16.dp))

        FilterLabel("Импровизация")

        ChipsFilter(state.improvStyleFilter, onToggleImprovStyle)

        Spacer(modifier = Modifier.height(16.dp))

        FilterLabel("Возраст")

        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = minAgeText,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() }) {
                        minAgeText = newValue
                        onAgeRangeChange(newValue.toIntOrNull(), state.maxAgeFilter)
                    }
                },
                modifier = Modifier.weight(1f),
                label = { Text("От") }
            )

            OutlinedTextField(
                value = maxAgeText,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() }) {
                        maxAgeText = newValue
                        onAgeRangeChange(state.minAgeFilter, newValue.toIntOrNull())
                    }
                },
                modifier = Modifier.weight(1f),
                label = { Text("До") }
            )
        }

        // City filter
        Text(
            text = "Город",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 16.dp)
        )

        DropdownMenu(
            expanded = false,
            onDismissRequest = { },
            modifier = Modifier.fillMaxWidth()
        ) {
            state.cities.forEach { city ->
                DropdownMenuItem(
                    text = { Text(city.name) },
                    onClick = { onCityChange(city.id) }
                )
            }
        }

        Text(
            text = state.cities.find { it.id == state.selectedCityID }?.name ?: "Select city",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable { /* Show dropdown */ }
        )

        // Boolean filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = state.lookingForTeamFilter,
                onCheckedChange = { onLookingForTeamToggle() }
            )

            Text(
                text = "Looking for team",
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = state.hasAvatarFilter,
                onCheckedChange = { onHasAvatarToggle() }
            )

            Text(
                text = "Has avatar",
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = state.hasVideoFilter,
                onCheckedChange = { onHasVideoToggle() }
            )

            Text(
                text = "Has video",
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Reset button
        Button(
            onClick = onReset,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 16.dp)
        ) {
            Text("Reset Filters")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipsFilter(options: List<Option>, onClick: (String) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option.isSelected,
                onClick = { onClick(option.id) },
                label = { Text(option.label) }
            )
        }
    }
}

@Composable
fun ProfileCard(profile: ProfileView, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Profile avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .padding(end = 16.dp)
            ) {
                Avatar(
                    mediaItem = profile.avatar,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Profile details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = profile.fullName,
                    style = MaterialTheme.typography.headlineSmall
                )

                Row(
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    if (profile.age != null) {
                        Text(
                            text = "${profile.age} years",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    if (profile.genderLabel != null) {
                        Text(
                            text = profile.genderLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                    if (profile.cityLabel != null) {
                        Text(
                            text = profile.cityLabel,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (profile.goalLabel != null) {
                    Text(
                        text = "Goal: ${profile.goalLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Text(
                    text = profile.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                if (profile.improvStylesLabels.isNotEmpty()) {
                    Text(
                        text = "Styles: ${profile.improvStylesLabels.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (profile.lookingForTeam) {
                    Text(
                        text = "Looking for team",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SearchPagination(
    currentPage: Int,
    totalPages: Int,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onPreviousPage,
            enabled = currentPage > 1
        ) {
            Text("Previous")
        }

        Text("${currentPage} of $totalPages")

        Button(
            onClick = onNextPage,
            enabled = currentPage < totalPages
        ) {
            Text("Next")
        }
    }
}