package com.brigadka.app.data.api

import com.brigadka.app.data.api.models.AuthResponse
import com.brigadka.app.data.api.models.CreateProfileRequest
import com.brigadka.app.data.api.models.LoginRequest
import com.brigadka.app.data.api.models.MediaListResponse
import com.brigadka.app.data.api.models.MediaResponse
import com.brigadka.app.data.api.models.Profile
import com.brigadka.app.data.api.models.ProfileResponse
import com.brigadka.app.data.api.models.ProfileSearchRequest
import com.brigadka.app.data.api.models.ProfileSearchResponse
import com.brigadka.app.data.api.models.RefreshRequest
import com.brigadka.app.data.api.models.RegisterRequest
import com.brigadka.app.data.api.models.TranslatedItem
import com.brigadka.app.data.storage.Token
import com.brigadka.app.data.storage.TokenStorage
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

interface BrigadkaApiService {
    val unauthorizedClient: HttpClient
    val authorizedClient: HttpClient
    val baseUrl: String


    suspend fun login(request: LoginRequest): AuthResponse

    suspend fun register(request: RegisterRequest): AuthResponse

    suspend fun refreshToken(refreshToken: String): AuthResponse

    suspend fun verifyToken(token: String): String

    suspend fun createProfile(request: CreateProfileRequest): Profile

    suspend fun getProfile(profileId: Int): ProfileResponse

    suspend fun getProfileMedia(profileId: Int, role: String? = null): MediaListResponse

    suspend fun getMedia(mediaId: Int): MediaResponse

    suspend fun deleteMedia(mediaId: Int)

    suspend fun uploadMedia(profileId: Int, role: String, file: ByteArray, fileName: String): MediaResponse

    suspend fun getActivityTypes(lang: String? = null): List<TranslatedItem>

    suspend fun getImprovGoals(lang: String? = null): List<TranslatedItem>

    suspend fun getImprovStyles(lang: String? = null): List<TranslatedItem>

    suspend fun getMusicGenres(lang: String? = null): List<TranslatedItem>

    suspend fun getMusicInstruments(lang: String? = null): List<TranslatedItem>

    suspend fun searchProfiles(request: ProfileSearchRequest): ProfileSearchResponse

    suspend fun searchProfilesGet(
        fullName: String? = null,
        cityId: Int? = null,
        activityType: String? = null,
        improvLookingForTeam: Boolean? = null,
        improvGoal: String? = null,
        improvStyle: String? = null,
        musicGenre: String? = null,
        musicInstrument: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): ProfileSearchResponse
}

class BrigadkaApiServiceImpl(
    override val unauthorizedClient: HttpClient,
    override val authorizedClient: HttpClient,
    override val baseUrl: String
) : BrigadkaApiService {
    override suspend fun login(request: LoginRequest): AuthResponse {
        return unauthorizedClient.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun register(request: RegisterRequest): AuthResponse {
        return unauthorizedClient.post("$baseUrl/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun refreshToken(refreshToken: String): AuthResponse {
        return unauthorizedClient.post("$baseUrl/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(refresh_token = refreshToken))
        }.body()
    }

    override suspend fun verifyToken(token: String): String {
        return authorizedClient.get("$baseUrl/api/auth/verify") {
            header("Authorization", "Bearer $token")
        }.body()
    }

    override suspend fun createProfile(request: CreateProfileRequest): Profile {
        return authorizedClient.post("$baseUrl/api/profiles") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun getProfile(profileId: Int): ProfileResponse {
        return authorizedClient.get("$baseUrl/api/profiles/$profileId").body()
    }

    override suspend fun getProfileMedia(profileId: Int, role: String?): MediaListResponse {
        return authorizedClient.get("$baseUrl/api/profiles/$profileId/media") {
            if (role != null) {
                parameter("role", role)
            }
        }.body()
    }

    override suspend fun getMedia(mediaId: Int): MediaResponse {
        return authorizedClient.get("$baseUrl/api/media/$mediaId").body()
    }

    override suspend fun deleteMedia(mediaId: Int) {
        authorizedClient.delete("$baseUrl/api/media/$mediaId")
    }

    override suspend fun uploadMedia(
        profileId: Int,
        role: String,
        file: ByteArray,
        fileName: String
    ): MediaResponse {
        return authorizedClient.submitFormWithBinaryData(
            url = "$baseUrl/api/media/upload",
            formData = formData {
                append("profile_id", profileId)
                append("role", role)
                append("file", file, Headers.build {
                    append(io.ktor.http.HttpHeaders.ContentType, "image/jpeg")
                    append(io.ktor.http.HttpHeaders.ContentDisposition, "filename=$fileName")
                })
            }
        ) {
        }.body()
    }

    override suspend fun getActivityTypes(lang: String?): List<TranslatedItem> {
        return authorizedClient.get("$baseUrl/api/profiles/catalog/activity-types") {
            if (lang != null) {
                parameter("lang", lang)
            }
        }.body()
    }

    override suspend fun getImprovGoals(lang: String?): List<TranslatedItem> {
        return authorizedClient.get("$baseUrl/api/profiles/catalog/improv-goals") {
            if (lang != null) {
                parameter("lang", lang)
            }
        }.body()
    }

    override suspend fun getImprovStyles(lang: String?): List<TranslatedItem> {
        return authorizedClient.get("$baseUrl/api/profiles/catalog/improv-styles") {
            if (lang != null) {
                parameter("lang", lang)
            }
        }.body()
    }

    override suspend fun getMusicGenres(lang: String?): List<TranslatedItem> {
        return authorizedClient.get("$baseUrl/api/profiles/catalog/music-genres") {
            if (lang != null) {
                parameter("lang", lang)
            }
        }.body()
    }

    override suspend fun getMusicInstruments(lang: String?): List<TranslatedItem> {
        return authorizedClient.get("$baseUrl/api/profiles/catalog/music-instruments") {
            if (lang != null) {
                parameter("lang", lang)
            }
        }.body()
    }

    override suspend fun searchProfiles(request: ProfileSearchRequest): ProfileSearchResponse {
        return authorizedClient.post("$baseUrl/api/search/profiles") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    override suspend fun searchProfilesGet(
        fullName: String?,
        cityId: Int?,
        activityType: String?,
        improvLookingForTeam: Boolean?,
        improvGoal: String?,
        improvStyle: String?,
        musicGenre: String?,
        musicInstrument: String?,
        limit: Int?,
        offset: Int?
    ): ProfileSearchResponse {
        return authorizedClient.get("$baseUrl/api/search/profiles") {
            if (fullName != null) {
                parameter("full_name", fullName)
            }
            if (cityId != null) {
                parameter("city_id", cityId)
            }
            if (activityType != null) {
                parameter("activity_type", activityType)
            }
            if (improvLookingForTeam != null) {
                parameter("improv_looking_for_team", improvLookingForTeam)
            }
            if (improvGoal != null) {
                parameter("improv_goal", improvGoal)
            }
            if (improvStyle != null) {
                parameter("improv_style", improvStyle)
            }
            if (musicGenre != null) {
                parameter("music_genre", musicGenre) // Corrected: musicGenre is now used!
            }
            if (musicInstrument != null) {
                parameter("music_instrument", musicInstrument)
            }
            if (limit != null) {
                parameter("limit", limit)
            }
            if (offset != null) {
                parameter("offset", offset)
            }
        }.body()
    }
}

fun createUnauthorizedKtorClient() = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    // Логирование
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.ALL
    }
}

fun createAuthorizedKtorClient(tokenStorage: TokenStorage, refreshAccessToken: suspend (String?) -> Token?) = HttpClient {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    // Логирование
    install(Logging) {
        logger = Logger.DEFAULT
        level = LogLevel.ALL
    }
    install(Auth) {
        bearer {
            loadTokens {
                tokenStorage.token.first().let { token ->
                    if (token.accessToken != null && token.refreshToken != null) {
                        BearerTokens(token.accessToken, token.refreshToken)
                    } else {
                        null
                    }
                }
            }
            refreshTokens {
                try {
                    val oldRefreshToken = this.oldTokens?.refreshToken
                    if(oldRefreshToken == null){
                        tokenStorage.clearToken()
                        return@refreshTokens null
                    }
                    val newToken = refreshAccessToken(oldRefreshToken)
                    if (newToken != null) {
                        tokenStorage.saveToken(newToken)
                        BearerTokens(newToken.accessToken!!, newToken.refreshToken!!)
                    } else {
                        tokenStorage.clearToken()
                        null
                    }
                } catch (e: ClientRequestException) {
                    if (e.response.status == HttpStatusCode.Unauthorized) {
                        tokenStorage.clearToken()
                        null
                    } else {
                        throw e
                    }
                }
            }
        }
    }
}