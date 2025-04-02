package org.one.oneappstorebackend.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterDto(
    val username: String,
    val email: String,
    val password: String,
    val isDeveloper: Boolean = false
)

@Serializable
data class LoginDto(
    val email: String,
    val password: String
)

@Serializable
data class GithubCallbackDto(
    val code: String
)

@Serializable
data class UpdateUserDto(
    val username: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class CreateAppDto(
    val appId: String,
    val name: String,
    val developer: String,
    val description: String,
    val category: String
)

@Serializable
data class CreateAppVersionDto(
    val version: String,
    val releaseNotes: String? = null,
    val minOsVersion: String? = null,
    val sizeBytes: Long? = null
)

@Serializable
data class CreatePlatformSupportDto(
    val platformId: Int,
    val downloadUrl: String,
    val price: Double = 0.0
)

@Serializable
data class UpdateAppDto(
    val name: String? = null,
    val developer: String? = null,
    val description: String? = null,
    val category: String? = null,
    val releaseDate: String? = null,
    val isFeatured: Boolean? = null
)

@Serializable
data class ApprovalDto(
    val notes: String? = null
)

@Serializable
data class ImportGithubReleaseDto(
    val repoOwner: String,
    val repoName: String,
    val releaseTag: String,
    val appName: String,
    val appDescription: String,
    val appCategory: String
)

@Serializable
data class UpdateFromGithubReleaseDto(
    val repoOwner: String,
    val repoName: String,
    val releaseTag: String
)

@Serializable
data class GithubRepoDto(
    val id: Int,
    val name: String,
    val fullName: String,
    val description: String?,
    val defaultBranch: String,
    val htmlUrl: String,
    val updatedAt: String,
    val hasReleases: Boolean
)

@Serializable
data class GithubReleaseDto(
    val id: Int,
    val tagName: String,
    val name: String?,
    val body: String?,
    val draft: Boolean,
    val prerelease: Boolean,
    val createdAt: String,
    val publishedAt: String?,
    val htmlUrl: String,
    val assets: List<GithubReleaseAssetDto>
)

@Serializable
data class GithubReleaseAssetDto(
    val id: Int,
    val name: String,
    val label: String?,
    val state: String,
    val contentType: String,
    val size: Long,
    val downloadCount: Int,
    val browserDownloadUrl: String
)

@Serializable
data class ReleaseInfoDto(
    val tagName: String,
    val name: String?,
    val description: String?,
    val publishedAt: String?,
    val platforms: Map<String, ReleaseAssetInfoDto>
)

@Serializable
data class ReleaseAssetInfoDto(
    val fileName: String,
    val downloadUrl: String,
    val size: Long,
    val downloadCount: Int
) 