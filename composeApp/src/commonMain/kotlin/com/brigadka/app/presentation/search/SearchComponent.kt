package com.brigadka.app.presentation.search

import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.pushNew
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.brigadka.app.common.coroutineScope
import com.brigadka.app.data.api.models.City
import com.brigadka.app.data.api.models.StringItem
import com.brigadka.app.data.repository.ProfileRepository
import com.brigadka.app.data.repository.SearchResult
import com.brigadka.app.di.ProfileViewComponentFactory
import com.brigadka.app.presentation.common.TopBarState
import com.brigadka.app.presentation.common.UIEvent
import com.brigadka.app.presentation.common.UIEventEmitter
import com.brigadka.app.presentation.profile.view.ProfileViewComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

private val logger = Logger.withTag("SearchComponent")

data class SearchTopBarState(
    val query: String,
    val onQueryChange: (String) -> Unit,
    val onSearch: () -> Unit,
    val onToggleFilters: () -> Unit
): TopBarState

class SearchComponent(
    componentContext: ComponentContext,
    private val uiEventEmitter: UIEventEmitter,
    private val profileRepository: ProfileRepository,
    private val profileViewComponentFactory: ProfileViewComponentFactory,
) : ComponentContext by componentContext {

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> get() = _state

    // Navigation
    private val navigation = StackNavigation<SearchConfig>()
    private val _childStack = childStack(
        source = navigation,
        initialConfiguration = SearchConfig.SearchList,
        handleBackButton = true,
        serializer = SearchConfig.serializer(),
        childFactory = ::createChild
    )

    val childStack: Value<ChildStack<SearchConfig, SearchChild>> = _childStack

    private fun createChild(
        config: SearchConfig,
        componentContext: ComponentContext
    ): SearchChild = when (config) {
        is SearchConfig.SearchList -> SearchChild.SearchPage
        is SearchConfig.Profile -> SearchChild.Profile(
            profileViewComponentFactory.create(
                componentContext,
                config.userID,
                { navigation.pop() }
            )
        )
    }

    private val coroutineScope = coroutineScope()
    private var searchJob: Job? = null

    fun toggleFilters() {
        _state.update { it.copy(showFilters = !it.showFilters) }
    }

    init {
        loadReferenceData()
        performSearch()
    }

    suspend fun showTopBar() {
        state.collect {
            val topBarState = SearchTopBarState(
                query = _state.value.nameFilter ?: "",
                onQueryChange = ::updateNameFilter,
                onSearch = ::performSearch,
                onToggleFilters = ::toggleFilters
            )
            uiEventEmitter.emit(UIEvent.TopBarUpdate(topBarState))
        }
    }

    private fun loadReferenceData() {
        coroutineScope.launch {
            try {
                val cities = profileRepository.getCities()
                val genders = profileRepository.getGenders()
                val goals = profileRepository.getImprovGoals()
                val styles = profileRepository.getImprovStyles()

                _state.update { it.copy(
                    cities = cities,
                    genderFilter = genders.toOptions(),
                    goalFilter = goals.toOptions(),
                    improvStyleFilter = styles.toOptions(),
                    isLoading = false
                ) }

            } catch (e: Exception) {
                _state.update { it.copy(
                    error = "Failed to load reference data",
                    isLoading = false
                ) }
            }
        }
    }

    fun performSearch() {
        // Cancel previous search if still running
        searchJob?.cancel()

        // Reset search state before performing new search
        _state.update { it.copy(
            searchResult = null,
            currentPage = 0,  // Set to 0 so nextPage() will load page 1
            isLoading = false,
            error = null
        )}

        // Use nextPage to perform the initial search
        nextPage()
    }

    fun nextPage() {
        val currentState = _state.value

        // If we're loading or there are no more pages, don't proceed
        if (currentState.isLoading) return

        // Calculate the next page to load
        val nextPage = if (currentState.searchResult == null) 1 else currentState.searchResult.page + 1

        // Check if we've reached the end of results
        if (currentState.searchResult != null &&
            nextPage > (currentState.searchResult.totalCount / currentState.searchResult.pageSize) + 1) {
            return
        }

        _state.update { it.copy(isLoading = true, error = null) }

        searchJob?.cancel()
        searchJob = coroutineScope.launch {
            try {
                val newResults = withContext(Dispatchers.IO) {
                    profileRepository.searchProfiles(
                        fullName = currentState.nameFilter,
                        ageMin = currentState.minAgeFilter,
                        ageMax = currentState.maxAgeFilter,
                        cityId = currentState.selectedCityID,
                        genders = currentState.genderFilter.mapNotNull { if (it.isSelected) it.id else null },
                        goals = currentState.goalFilter.mapNotNull { if (it.isSelected) it.id else null },
                        improvStyles = currentState.improvStyleFilter.mapNotNull { if (it.isSelected) it.id else null },
                        lookingForTeam = if (currentState.lookingForTeamFilter) true else null,
                        hasAvatar = if (currentState.hasAvatarFilter) true else null,
                        hasVideo = if (currentState.hasVideoFilter) true else null,
                        page = nextPage,
                        pageSize = currentState.pageSize
                    )
                }

                // Combine with existing profiles or use new profiles if this is first page
                val combinedProfiles = if (currentState.searchResult != null) {
                    currentState.searchResult.profiles + newResults.profiles
                } else {
                    newResults.profiles
                }

                _state.update { it.copy(
                    searchResult = SearchResult(
                        profiles = combinedProfiles,
                        page = newResults.page,
                        pageSize = newResults.pageSize,
                        totalCount = newResults.totalCount
                    ),
                    currentPage = nextPage,
                    isLoading = false
                )}

            } catch (e: Exception) {
                _state.update { it.copy(
                    error = "Search failed: ${e.message}",
                    isLoading = false
                )}
            }
        }
    }

    fun updateNameFilter(name: String) {
        _state.update { it.copy(nameFilter = name) }
    }

    fun updateAgeRange(min: Int?, max: Int?) {
        _state.update { it.copy(
            minAgeFilter = min,
            maxAgeFilter = max
        ) }
    }

    fun updateCityFilter(cityId: Int?) {
        _state.update { it.copy(selectedCityID = cityId) }
    }

    fun toggleGender(gender: String) {
        _state.update { it.copy(genderFilter = it.genderFilter.toggle(gender)) }
    }

    fun toggleGoal(goal: String) {
        _state.update { it.copy(goalFilter = it.goalFilter.toggle(goal)) }
    }

    fun toggleImprovStyle(style: String) {
        _state.update {it.copy(improvStyleFilter = it.improvStyleFilter.toggle(style))}
    }

    fun toggleLookingForTeam(value: Boolean) {
        _state.update { it.copy(lookingForTeamFilter = value) }
    }

    fun toggleHasAvatar(value: Boolean) {
        _state.update { it.copy(hasAvatarFilter = value) }
    }

    fun toggleHasVideo(value: Boolean) {
        _state.update { it.copy(hasVideoFilter = value) }
    }

    fun onProfileClick(userId: Int) {
        navigation.pushNew(SearchConfig.Profile(userId))
    }

    fun resetFilters() {
        _state.update {
            SearchState(
                cities = it.cities,
                genderFilter = it.genderFilter.reset(),
                goalFilter = it.goalFilter.reset(),
                improvStyleFilter = it.improvStyleFilter.reset(),
            )
        }
        performSearch()
    }
}

@Serializable
sealed class SearchConfig {
    @Serializable
    object SearchList : SearchConfig()

    @Serializable
    data class Profile(val userID: Int) : SearchConfig()
}

sealed class SearchChild {
    object SearchPage : SearchChild()
    data class Profile(val component: ProfileViewComponent) : SearchChild()
}

fun List<Option>.toggle(
    optionId: String
): List<Option> {
    return map { option ->
        if (option.id == optionId) {
            option.copy(isSelected = !option.isSelected)
        } else {
            option
        }
    }
}

fun List<Option>.reset(): List<Option> {
    return map { option -> option.copy(isSelected = false) }
}

fun List<StringItem>.toOptions(): List<Option> {
    return map { item -> Option(id = item.code, label = item.label, isSelected = false) }
}

data class Option (
    val id: String,
    val label: String,
    val isSelected: Boolean,
)

data class SearchState(
    // Reference data
    val cities: List<City> = emptyList(),

    val showFilters: Boolean = false,

    // Filter values
    val nameFilter: String? = null,

    val minAgeFilter: Int? = null,
    val maxAgeFilter: Int? = null,

    val genderFilter: List<Option> = emptyList(),
    val goalFilter: List<Option> = emptyList(),
    val improvStyleFilter: List<Option> = emptyList(),

    val selectedCityID: Int? = null,

    val lookingForTeamFilter: Boolean = false,
    val hasAvatarFilter: Boolean = false,
    val hasVideoFilter: Boolean = false,

    // Pagination
    val currentPage: Int = 1,
    val pageSize: Int = 20,

    // Results
    val searchResult: SearchResult? = null,

    // UI state
    val isLoading: Boolean = true,
    val error: String? = null
)