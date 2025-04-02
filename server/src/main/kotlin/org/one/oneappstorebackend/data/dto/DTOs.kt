package org.one.oneappstorebackend.data.dto

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable
data class AppDto(
    val id: Int,
    val appId: String,
    val name: String,
    val developer: String,
    val description: String,
    val category: String,
    val releaseDate: String,
    val isFeatured: Boolean,
    val dateAdded: String,
    val lastUpdated: String,
    val approvalStatus: String,
    val submittedBy: String? = null,
    val ratings: Double? = null,
    val platforms: List<String> = emptyList()
)

@Serializable
data class AppDetailDto(
    val id: Int,
    val appId: String,
    val name: String,
    val developer: String,
    val description: String,
    val category: String,
    val releaseDate: String,
    val isFeatured: Boolean,
    val dateAdded: String,
    val lastUpdated: String,
    val approvalStatus: String,
    val submittedBy: String? = null,
    val ratings: Double? = null,
    val reviews: List<ReviewDto> = emptyList(),
    val versions: List<AppVersionDto> = emptyList(),
    val platformSupport: List<AppPlatformSupportDto> = emptyList()
)

@Serializable
data class AppVersionDto(
    val id: Int,
    val appId: String,
    val version: String,
    val releaseNotes: String?,
    val releaseDate: String,
    val minOsVersion: String?,
    val sizeBytes: Long?,
    val approvalStatus: String,
    val submittedBy: String? = null
)

@Serializable
data class PlatformDto(
    val id: Int,
    val name: String,
    val displayName: String
)

@Serializable
data class AppPlatformSupportDto(
    val id: Int,
    val appId: String,
    val platformId: Int,
    val platformName: String,
    val versionId: Int,
    val version: String,
    val downloadUrl: String,
    val price: Double
)

@Serializable
data class ReviewDto(
    val id: Int,
    val appId: String,
    val userId: String,
    val userName: String,
    val rating: Int,
    val comment: String?,
    val datePosted: String
)

@Serializable
data class UserDto(
    val id: Int,
    val userId: String,
    val username: String,
    val email: String,
    val dateRegistered: String,
    val isAdmin: Boolean,
    val isDeveloper: Boolean,
    val githubUsername: String? = null
)

@Serializable
data class UserRegistrationDto(
    val username: String,
    val email: String,
    val password: String
)

@Serializable
data class UserLoginDto(
    val email: String,
    val password: String
)

@Serializable
data class UserAuthResponseDto(
    val token: String,
    val user: UserDto
)

@Serializable
data class GithubAuthRequestDto(
    val code: String,
    val redirectUri: String
)

@Serializable
data class GithubUserDto(
    val id: String,
    val login: String,
    val name: String?,
    val email: String?,
    val avatarUrl: String?
)

@Serializable
data class TokenRefreshRequestDto(
    val refreshToken: String
)

@Serializable
data class TokenRefreshResponseDto(
    val accessToken: String,
    val expiresIn: Long
)

@Serializable
data class AppApprovalDto(
    val appId: String,
    val versionId: Int? = null,
    val status: String,
    val reviewNotes: String? = null
)

@Serializable
data class AppApprovalHistoryDto(
    val id: Int,
    val appId: String,
    val versionId: Int?,
    val status: String,
    val reviewedBy: String?,
    val reviewerName: String?,
    val reviewNotes: String?,
    val timestamp: String
)

@Serializable
data class AppSubmissionDto(
    val appId: String,
    val name: String,
    val description: String,
    val category: String,
    val version: String,
    val releaseNotes: String? = null,
    val releaseDate: String,
    val minOsVersion: String? = null,
    val platforms: List<AppPlatformSubmissionDto>
)

@Serializable
data class AppPlatformSubmissionDto(
    val platformId: Int,
    val downloadUrl: String,
    val price: Double = 0.0
)

@Serializable
data class AppVersionSubmissionDto(
    val appId: String,
    val version: String,
    val releaseNotes: String? = null,
    val releaseDate: String,
    val minOsVersion: String? = null,
    val platforms: List<AppPlatformSubmissionDto>
)

@Serializable
data class UserInstallationDto(
    val id: Int,
    val userId: String,
    val appId: String,
    val appName: String,
    val versionId: Int,
    val version: String,
    val platformId: Int,
    val platformName: String,
    val installDate: String,
    val isActive: Boolean
)

@Serializable
data class UserWishlistDto(
    val id: Int,
    val userId: String,
    val appId: String,
    val appName: String,
    val developer: String,
    val dateAdded: String
)

@Serializable
data class CollectionDto(
    val id: Int,
    val collectionId: String,
    val name: String,
    val description: String?,
    val isSystem: Boolean,
    val createdBy: String,
    val creatorName: String,
    val dateCreated: String,
    val appCount: Int
)

@Serializable
data class CollectionDetailDto(
    val id: Int,
    val collectionId: String,
    val name: String,
    val description: String?,
    val isSystem: Boolean,
    val createdBy: String,
    val creatorName: String,
    val dateCreated: String,
    val apps: List<AppDto>
)

@Serializable
data class NotificationDto(
    val id: Int,
    val userId: String,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val dateCreated: String
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null
)

@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Long,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int
) 