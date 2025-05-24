package com.brigadka.app.presentation.onboarding.basic

import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.brigadka.app.data.api.models.City
import com.brigadka.app.data.api.models.StringItem
import com.brigadka.app.data.repository.ProfileRepository
import com.brigadka.app.presentation.profile.common.ProfileState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

open class BasicInfoEditor(
    private val coroutineScope: CoroutineScope,
    private val _profileState: MutableValue<ProfileState>,
    private val profileRepository: ProfileRepository,
) {

    private val _cities = MutableValue<List<City>>(emptyList())
    val cities: Value<List<City>> = _cities

    private val _genders = MutableValue<List<StringItem>>(emptyList())
    val genders: Value<List<StringItem>> = _genders

    init {
        loadCatalogData()
    }

    private fun loadCatalogData() {
        coroutineScope.launch {
            _cities.value = profileRepository.getCities()
            _genders.value = profileRepository.getGenders()
        }
    }

    fun updateFullName(fullName: String) {
        _profileState.update { it.copy(fullName = fullName) }
    }

    fun updateBirthday(birthday: LocalDate?) {
        _profileState.update { it.copy(birthday = birthday) }
    }

    fun updateGender(gender: String) {
        _profileState.update { it.copy(gender = gender) }
    }

    fun updateCityId(cityId: Int) {
        _profileState.update { it.copy(cityId = cityId) }
    }

    val isCompleted: Boolean
        get() = _profileState.value.fullName.isNotEmpty() &&
                _profileState.value.birthday != null &&
                _profileState.value.cityId != null &&
                _profileState.value.gender != null
}