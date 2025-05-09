package com.brigadka.app.data.repository

import com.brigadka.app.data.api.BrigadkaApiServiceAuthorized
import com.brigadka.app.data.api.models.City
import com.brigadka.app.data.api.models.MediaItem
import com.brigadka.app.data.api.models.Profile
import com.brigadka.app.data.api.models.ProfileCreateRequest
import com.brigadka.app.data.api.models.ProfileUpdateRequest
import com.brigadka.app.data.api.models.SearchRequest
import com.brigadka.app.data.api.models.StringItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface ProfileRepository {
    // Current user's profile as a flow
    val currentUserProfile: StateFlow<Profile?>
    val currentUserProfileView: StateFlow<ProfileView?>

    // Get any profile by ID without changing currentUserProfile
    suspend fun getProfileView(userId: Int?): ProfileView

    // Load/refresh the current user's profile and update currentUserProfile flow
    suspend fun refreshCurrentUserProfile()

    // Update the current user's profile
    suspend fun updateProfile(request: ProfileUpdateRequest): Profile

    // Create a new profile for the current user
    suspend fun createProfile(request: ProfileCreateRequest): Profile

    // Clear the current user's profile
    suspend fun clearProfile()

    // Additional methods for reference data
    suspend fun getCities(): List<City>
    suspend fun getGenders(): List<StringItem>
    suspend fun getImprovGoals(): List<StringItem>
    suspend fun getImprovStyles(): List<StringItem>

    // Search profiles
    suspend fun searchProfiles(request: SearchRequest): SearchResult
    suspend fun searchProfiles(
        fullName: String? = null,
        ageMin: Int? = null,
        ageMax: Int? = null,
        cityId: Int? = null,
        genders: List<String>? = null,
        goals: List<String>? = null,
        improvStyles: List<String>? = null,
        lookingForTeam: Boolean? = null,
        hasAvatar: Boolean? = null,
        hasVideo: Boolean? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): SearchResult
}

class ProfileRepositoryImpl(
    private val apiService: BrigadkaApiServiceAuthorized,
    private val userDataRepository: UserDataRepository
) : ProfileRepository {

    private val _currentUserProfile = MutableStateFlow<Profile?>(null)
    override val currentUserProfile = _currentUserProfile.asStateFlow()

    // Cache for translated items
    private val _cities = MutableStateFlow<List<City>?>(null)
    private val _genders = MutableStateFlow<List<StringItem>?>(null)
    private val _improvGoals = MutableStateFlow<List<StringItem>?>(null)
    private val _improvStyles = MutableStateFlow<List<StringItem>?>(null)

    override val currentUserProfileView: StateFlow<ProfileView?> = combine(_cities, _genders, _improvGoals, _improvStyles, currentUserProfile) { cities, genders, improvGoals, improvStyles, profile ->
        if (cities != null && genders != null && improvGoals != null && improvStyles != null && profile != null) {
            convertToProfileView(profile, cities, improvGoals, improvStyles, genders)
        }
        null
    }.stateIn(
        scope = CoroutineScope(Dispatchers.Default),
        started = SharingStarted.Eagerly,
        initialValue = null
    )


    init {
        CoroutineScope(Dispatchers.Default).launch {
            userDataRepository.currentUserId.collect { userId ->
                if (userId != null) {
                    refreshCurrentUserProfile()
                    loadFieldValues()
                } else {
                    clearProfile()
                }
            }
        }
    }

    private suspend fun loadFieldValues() {
        _cities.value = getCities()
        _genders.value = getGenders()
        _improvGoals.value = getImprovGoals()
        _improvStyles.value = getImprovStyles()
    }

    override suspend fun refreshCurrentUserProfile() {
        try {
            val userId = userDataRepository.currentUserId.value
            if (userId != null) {
                val profile = apiService.getProfile(userId)
                _currentUserProfile.update { profile }
            }
        } catch (e: Exception) {
            // TODO: Handle error (e.g., log it, show a message, etc.)
            _currentUserProfile.update { null }
        }
    }

    override suspend fun getProfileView(userId: Int?): ProfileView {
        // TODO: getImprovGoals() should internally use the cached values
        val profile = apiService.getProfile(userId ?: userDataRepository.requireUserId())
        val cities = _cities.value ?: getCities()
        val improvGoals = _improvGoals.value ?: getImprovGoals()
        val improvStyles = _improvStyles.value ?: getImprovStyles()
        val genders = _genders.value ?: getGenders()

        return convertToProfileView(profile, cities, improvGoals, improvStyles, genders)
    }

    override suspend fun createProfile(request: ProfileCreateRequest): Profile {
        val profile = apiService.createProfile(request)
        _currentUserProfile.update { profile }
        return profile
    }

    override suspend fun updateProfile(request: ProfileUpdateRequest): Profile {
        val profile = apiService.updateProfile(request)
        _currentUserProfile.update { profile }
        return profile
    }

    override suspend fun clearProfile() {
        _currentUserProfile.update { null }
    }

    override suspend fun getCities(): List<City> {
        return apiService.getCities()
    }

    override suspend fun getGenders(): List<StringItem> {
        return apiService.getGenders()
    }

    override suspend fun getImprovGoals(): List<StringItem> {
        return apiService.getImprovGoals()
    }

    override suspend fun getImprovStyles(): List<StringItem> {
        return apiService.getImprovStyles()
    }

    override suspend fun searchProfiles(
        fullName: String?,
        ageMin: Int?,
        ageMax: Int?,
        cityId: Int?,
        genders: List<String>?,
        goals: List<String>?,
        improvStyles: List<String>?,
        lookingForTeam: Boolean?,
        hasAvatar: Boolean?,
        hasVideo: Boolean?,
        page: Int,
        pageSize: Int
    ): SearchResult {
        val request = SearchRequest(
            full_name = fullName,
            age_min = ageMin,
            age_max = ageMax,
            city_id = cityId,
            genders = genders,
            goals = goals,
            improv_styles = improvStyles,
            looking_for_team = lookingForTeam,
            has_avatar = hasAvatar,
            has_video = hasVideo,
            page = page,
            page_size = pageSize
        )

        return searchProfiles(request)
    }

    override suspend fun searchProfiles(request: SearchRequest): SearchResult {
        val response = apiService.searchProfiles(request)

        val cities = _cities.value ?: getCities()
        val genders = _genders.value ?: getGenders()
        val improvGoals = _improvGoals.value ?: getImprovGoals()
        val improvStyles = _improvStyles.value ?: getImprovStyles()

        val profileViews = response.profiles.map { profile ->
            convertToProfileView(profile, cities, improvGoals, improvStyles, genders)
        }

        return SearchResult(
            profiles = profileViews,
            page = response.page,
            pageSize = response.page_size,
            totalCount = response.total_count
        )
    }
}

private fun convertToProfileView(
    profile: Profile,
    cities: List<City>,
    improvGoals: List<StringItem>,
    improvStyles: List<StringItem>,
    genders: List<StringItem>,
): ProfileView {
    return ProfileView(
        fullName = profile.full_name,
        age = 10,
        genderLabel = genders.find { it.code == profile.gender }?.label,
        cityLabel = cities.find { it.id == profile.city_id }?.name,
        bio = profile.bio,
        goalLabel = improvGoals.find { it.code == profile.goal }?.label,
        improvStylesLabels = profile.improv_styles.mapNotNull { styleCode ->
            improvStyles.find { it.code == styleCode }?.label
        },
        lookingForTeam = profile.looking_for_team,
        avatar = profile.avatar,
        videos = profile.videos
    )
}

data class ProfileView(
    val fullName: String,
    val age: Int?,
    val genderLabel: String?,
    val cityLabel: String?,
    val bio: String,
    val goalLabel: String?,
    val improvStylesLabels: List<String> = emptyList(),
    val lookingForTeam: Boolean = false,
    val avatar: MediaItem?,
    val videos: List<MediaItem> = emptyList()
)

data class SearchResult(
    val profiles: List<ProfileView>,
    val page: Int,
    val pageSize: Int,
    val totalCount: Int
)