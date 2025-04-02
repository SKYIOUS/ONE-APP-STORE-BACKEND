package org.one.oneappstorebackend.services

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.one.oneappstorebackend.data.dto.GithubUserDto
import org.one.oneappstorebackend.data.dto.UserDto
import org.one.oneappstorebackend.data.repositories.UserRepository
import java.time.LocalDateTime

class GithubAuthService(private val userRepository: UserRepository) {
    private val clientId = System.getenv("GITHUB_CLIENT_ID") ?: "your-github-client-id"
    private val clientSecret = System.getenv("GITHUB_CLIENT_SECRET") ?: "your-github-client-secret"
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }
    
    @Serializable
    private data class GithubTokenResponse(
        val access_token: String,
        val token_type: String,
        val scope: String,
        val refresh_token: String? = null,
        val expires_in: Long? = null
    )
    
    @Serializable
    private data class GithubUserResponse(
        val id: Int,
        val login: String,
        val name: String? = null,
        val email: String? = null,
        val avatar_url: String? = null
    )
    
    suspend fun exchangeCodeForToken(code: String, redirectUri: String): GithubTokenResponse {
        return httpClient.post("https://github.com/login/oauth/access_token") {
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "client_id" to clientId,
                    "client_secret" to clientSecret,
                    "code" to code,
                    "redirect_uri" to redirectUri
                )
            )
            header("Accept", "application/json")
        }.body()
    }
    
    suspend fun getUserInfo(accessToken: String): GithubUserResponse {
        return httpClient.get("https://api.github.com/user") {
            header("Authorization", "Bearer $accessToken")
            header("Accept", "application/json")
        }.body()
    }
    
    suspend fun getUserEmail(accessToken: String): String? {
        val emails = httpClient.get("https://api.github.com/user/emails") {
            header("Authorization", "Bearer $accessToken")
            header("Accept", "application/json")
        }.body<List<Map<String, Any>>>()
        
        return emails.firstOrNull { it["primary"] == true }?.get("email") as? String
    }
    
    suspend fun authenticateWithGithub(code: String, redirectUri: String): UserDto {
        val tokenResponse = exchangeCodeForToken(code, redirectUri)
        val userResponse = getUserInfo(tokenResponse.access_token)
        var email = userResponse.email
        
        if (email == null) {
            email = getUserEmail(tokenResponse.access_token)
        }
        
        val tokenExpiry = tokenResponse.expires_in?.let {
            LocalDateTime.now().plusSeconds(it)
        }
        
        return userRepository.findOrCreateGithubUser(
            githubId = userResponse.id.toString(),
            githubUsername = userResponse.login,
            email = email,
            accessToken = tokenResponse.access_token,
            refreshToken = tokenResponse.refresh_token,
            tokenExpiry = tokenExpiry
        )
    }
    
    suspend fun refreshAccessToken(userId: String): String? {
        val refreshToken = userRepository.getGithubRefreshToken(userId) ?: return null
        
        val tokenResponse = httpClient.post("https://github.com/login/oauth/access_token") {
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "client_id" to clientId,
                    "client_secret" to clientSecret,
                    "grant_type" to "refresh_token",
                    "refresh_token" to refreshToken
                )
            )
            header("Accept", "application/json")
        }.body<GithubTokenResponse>()
        
        val tokenExpiry = tokenResponse.expires_in?.let {
            LocalDateTime.now().plusSeconds(it)
        }
        
        userRepository.updateGithubTokens(
            userId = userId,
            accessToken = tokenResponse.access_token,
            refreshToken = tokenResponse.refresh_token,
            tokenExpiry = tokenExpiry
        )
        
        return tokenResponse.access_token
    }
    
    suspend fun listUserRepositories(accessToken: String): List<GithubRepoDto> {
        return httpClient.get("https://api.github.com/user/repos") {
            header("Authorization", "Bearer $accessToken")
            parameter("sort", "updated")
            parameter("per_page", "100")
        }.body()
    }
    
    suspend fun getRepositoryReleases(owner: String, repo: String, accessToken: String): List<GithubReleaseDto> {
        return httpClient.get("https://api.github.com/repos/$owner/$repo/releases") {
            header("Authorization", "Bearer $accessToken")
        }.body()
    }
    
    @Serializable
    data class GithubRepoDto(
        val id: Int,
        val name: String,
        val full_name: String,
        val private: Boolean,
        val html_url: String,
        val description: String?,
        val fork: Boolean,
        val created_at: String,
        val updated_at: String
    )
    
    @Serializable
    data class GithubReleaseDto(
        val id: Int,
        val tag_name: String,
        val name: String?,
        val body: String?,
        val draft: Boolean,
        val prerelease: Boolean,
        val created_at: String,
        val published_at: String,
        val assets: List<GithubReleaseAssetDto>
    )
    
    @Serializable
    data class GithubReleaseAssetDto(
        val id: Int,
        val name: String,
        val content_type: String,
        val size: Int,
        val browser_download_url: String,
        val created_at: String,
        val updated_at: String
    )
} 