package org.one.oneappstorebackend.data.repositories

import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.one.oneappstorebackend.data.DatabaseFactory.dbQuery
import org.one.oneappstorebackend.data.dto.*
import org.one.oneappstorebackend.data.models.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.javatime.JavaLocalDateColumnType
import org.jetbrains.exposed.sql.javatime.JavaLocalDateTimeColumnType
import org.jetbrains.exposed.sql.Function
import org.jetbrains.exposed.sql.avg

class AppRepository {
    private val dateFormatter = DateTimeFormatter.ISO_DATE
    private val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME
    
    suspend fun getAllApps(page: Int, pageSize: Int): PaginatedResponse<AppDto> = dbQuery {
        val totalApps = Apps.selectAll().count()
        val totalPages = (totalApps / pageSize) + if (totalApps % pageSize > 0) 1 else 0
        
        val apps = Apps.selectAll()
            .orderBy(Apps.dateAdded, SortOrder.DESC)
            .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
            .map { rowToAppDto(it) }
            
        PaginatedResponse(
            items = apps,
            total = totalApps,
            page = page,
            pageSize = pageSize,
            totalPages = totalPages.toInt()
        )
    }
    
    suspend fun getAllApprovedApps(page: Int, pageSize: Int): PaginatedResponse<AppDto> = dbQuery {
        val totalApps = Apps.select { Apps.approvalStatus eq "APPROVED" }.count()
        val totalPages = (totalApps / pageSize) + if (totalApps % pageSize > 0) 1 else 0
        
        val apps = Apps.select { Apps.approvalStatus eq "APPROVED" }
            .orderBy(Apps.dateAdded, SortOrder.DESC)
            .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
            .map { rowToAppDto(it) }
            
        PaginatedResponse(
            items = apps,
            total = totalApps,
            page = page,
            pageSize = pageSize,
            totalPages = totalPages.toInt()
        )
    }
    
    suspend fun getFeaturedApps(): List<AppDto> = dbQuery {
        Apps.select { (Apps.isFeatured eq true) and (Apps.approvalStatus eq "APPROVED") }
            .orderBy(Apps.dateAdded, SortOrder.DESC)
            .map { rowToAppDto(it) }
    }
    
    suspend fun getAppsByCategory(category: String, page: Int, pageSize: Int): PaginatedResponse<AppDto> = dbQuery {
        val condition = (Apps.category eq category) and (Apps.approvalStatus eq "APPROVED")
        val totalApps = Apps.select { condition }.count()
        val totalPages = (totalApps / pageSize) + if (totalApps % pageSize > 0) 1 else 0
        
        val apps = Apps.select { condition }
            .orderBy(Apps.dateAdded, SortOrder.DESC)
            .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
            .map { rowToAppDto(it) }
            
        PaginatedResponse(
            items = apps,
            total = totalApps,
            page = page,
            pageSize = pageSize,
            totalPages = totalPages.toInt()
        )
    }
    
    suspend fun searchApps(query: String, page: Int, pageSize: Int): PaginatedResponse<AppDto> = dbQuery {
        val searchPattern = "%${query.lowercase()}%"
        
        val condition = ((Apps.name.lowerCase() like searchPattern) or
                       (Apps.developer.lowerCase() like searchPattern) or
                       (Apps.description.lowerCase() like searchPattern)) and
                       (Apps.approvalStatus eq "APPROVED")
        
        val totalApps = Apps.select { condition }.count()
        val totalPages = (totalApps / pageSize) + if (totalApps % pageSize > 0) 1 else 0
        
        val apps = Apps.select { condition }
            .orderBy(Apps.dateAdded, SortOrder.DESC)
            .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
            .map { rowToAppDto(it) }
            
        PaginatedResponse(
            items = apps,
            total = totalApps,
            page = page,
            pageSize = pageSize,
            totalPages = totalPages.toInt()
        )
    }
    
    suspend fun getAppById(appId: String): AppDto? = dbQuery {
        Apps.select { Apps.appId eq appId }
            .map { rowToAppDto(it) }
            .singleOrNull()
    }
    
    suspend fun getAppDetailById(appId: String): AppDetailDto? = dbQuery {
        val appRow = Apps.select { Apps.appId eq appId }
            .singleOrNull() ?: return@dbQuery null
            
        val app = rowToAppDto(appRow)
        
        // Get average rating
        val avgRating = Reviews.select { Reviews.appId eq appId }
            .map { it[Reviews.rating] }
            .average()
            
        // Get platforms
        val platformsJoin = AppPlatformSupport
            .join(Platforms, JoinType.INNER, additionalConstraint = { 
                AppPlatformSupport.platformId eq Platforms.id 
            })
            .select { AppPlatformSupport.appId eq appId }
            .map { it[Platforms.name] }
            .distinct()
            
        // Get versions
        val versions = AppVersions
            .select { (AppVersions.appId eq appId) and (AppVersions.approvalStatus eq "APPROVED") }
            .orderBy(AppVersions.releaseDate, SortOrder.DESC)
            .map { rowToAppVersionDto(it) }
            
        // Get platform support
        val platformSupport = (AppPlatformSupport innerJoin AppVersions innerJoin Platforms)
            .slice(
                AppPlatformSupport.id, AppPlatformSupport.appId,
                AppPlatformSupport.platformId, Platforms.name,
                AppPlatformSupport.versionId, AppVersions.version,
                AppPlatformSupport.downloadUrl, AppPlatformSupport.price
            )
            .select { (AppPlatformSupport.appId eq appId) and (AppVersions.approvalStatus eq "APPROVED") }
            .map {
                AppPlatformSupportDto(
                    id = it[AppPlatformSupport.id].value,
                    appId = it[AppPlatformSupport.appId],
                    platformId = it[AppPlatformSupport.platformId],
                    platformName = it[Platforms.name],
                    versionId = it[AppPlatformSupport.versionId],
                    version = it[AppVersions.version],
                    downloadUrl = it[AppPlatformSupport.downloadUrl],
                    price = it[AppPlatformSupport.price].toDouble()
                )
            }
            
        // Get reviews
        val reviews = (Reviews innerJoin Users)
            .slice(
                Reviews.id, Reviews.appId, Reviews.userId,
                Reviews.userName, Reviews.rating, Reviews.comment,
                Reviews.datePosted
            )
            .select { Reviews.appId eq appId }
            .orderBy(Reviews.datePosted, SortOrder.DESC)
            .map {
                ReviewDto(
                    id = it[Reviews.id].value,
                    appId = it[Reviews.appId],
                    userId = it[Reviews.userId],
                    userName = it[Reviews.userName],
                    rating = it[Reviews.rating],
                    comment = it[Reviews.comment],
                    datePosted = it[Reviews.datePosted].toString()
                )
            }
            
        AppDetailDto(
            id = app.id,
            appId = app.appId,
            name = app.name,
            developer = app.developer,
            description = app.description,
            category = app.category,
            releaseDate = app.releaseDate,
            isFeatured = app.isFeatured,
            dateAdded = app.dateAdded,
            lastUpdated = app.lastUpdated,
            approvalStatus = app.approvalStatus,
            submittedBy = app.submittedBy,
            ratings = avgRating ?: 0.0,
            reviews = reviews,
            versions = versions,
            platformSupport = platformSupport
        )
    }
    
    suspend fun createApp(
        appId: String,
        name: String,
        developer: String,
        description: String,
        category: String,
        releaseDate: LocalDate,
        isFeatured: Boolean = false,
        submittedBy: String? = null
    ): AppDto? = dbQuery {
        // Check if app with this ID already exists
        val existingApp = Apps.select { Apps.appId eq appId }.singleOrNull()
        if (existingApp != null) {
            return@dbQuery null
        }
        
        val id = Apps.insert {
            it[Apps.appId] = appId
            it[Apps.name] = name
            it[Apps.developer] = developer
            it[Apps.description] = description
            it[Apps.category] = category
            it[Apps.releaseDate] = releaseDate
            it[Apps.isFeatured] = isFeatured
            it[Apps.approvalStatus] = "PENDING"
            if (submittedBy != null) {
                it[Apps.submittedBy] = submittedBy
            }
        } get Apps.id
        
        Apps.select { Apps.id eq id }
            .map { rowToAppDto(it) }
            .singleOrNull()
    }
    
    suspend fun createAppVersion(
        appId: String,
        version: String,
        releaseNotes: String? = null,
        releaseDate: LocalDate,
        minOsVersion: String? = null,
        sizeBytes: Long? = null,
        submittedBy: String? = null
    ): Int? = dbQuery {
        // Check if version already exists
        val existingVersion = AppVersions.select { 
            (AppVersions.appId eq appId) and (AppVersions.version eq version) 
        }.singleOrNull()
        
        if (existingVersion != null) {
            return@dbQuery null
        }
        
        val id = AppVersions.insert {
            it[AppVersions.appId] = appId
            it[AppVersions.version] = version
            it[AppVersions.releaseNotes] = releaseNotes
            it[AppVersions.releaseDate] = releaseDate
            it[AppVersions.minOsVersion] = minOsVersion
            it[AppVersions.sizeBytes] = sizeBytes
            it[AppVersions.approvalStatus] = "PENDING"
            if (submittedBy != null) {
                it[AppVersions.submittedBy] = submittedBy
            }
        } get AppVersions.id
        
        id.value
    }
    
    suspend fun createAppPlatformSupport(
        appId: String,
        platformId: Int,
        versionId: Int,
        downloadUrl: String,
        price: Double = 0.0
    ): Boolean = dbQuery {
        try {
            // Check if platform support already exists
            val existing = AppPlatformSupport.select { 
                (AppPlatformSupport.appId eq appId) and 
                (AppPlatformSupport.platformId eq platformId) and 
                (AppPlatformSupport.versionId eq versionId) 
            }.singleOrNull()
            
            if (existing != null) {
                // Update existing record
                AppPlatformSupport.update({
                    (AppPlatformSupport.appId eq appId) and 
                    (AppPlatformSupport.platformId eq platformId) and 
                    (AppPlatformSupport.versionId eq versionId)
                }) {
                    it[AppPlatformSupport.downloadUrl] = downloadUrl
                    it[AppPlatformSupport.price] = price.toBigDecimal()
                }
            } else {
                // Insert new record
                AppPlatformSupport.insert {
                    it[AppPlatformSupport.appId] = appId
                    it[AppPlatformSupport.platformId] = platformId
                    it[AppPlatformSupport.versionId] = versionId
                    it[AppPlatformSupport.downloadUrl] = downloadUrl
                    it[AppPlatformSupport.price] = price.toBigDecimal()
                }
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun getAllPlatforms(): List<PlatformDto> = dbQuery {
        Platforms.selectAll().map {
            PlatformDto(
                id = it[Platforms.id].value,
                name = it[Platforms.name],
                displayName = it[Platforms.displayName]
            )
        }
    }
    
    suspend fun updateApp(
        appId: String,
        name: String? = null,
        developer: String? = null,
        description: String? = null,
        category: String? = null,
        releaseDate: LocalDate? = null,
        isFeatured: Boolean? = null
    ): Boolean = dbQuery {
        val app = Apps.select { Apps.appId eq appId }.singleOrNull() ?: return@dbQuery false
        
        var updates = 0
        
        name?.let { value ->
            updates += Apps.update({ Apps.appId eq appId }) {
                it[Apps.name] = value
            }
        }
        
        developer?.let { value ->
            updates += Apps.update({ Apps.appId eq appId }) {
                it[Apps.developer] = value
            }
        }
        
        description?.let { value ->
            updates += Apps.update({ Apps.appId eq appId }) {
                it[Apps.description] = value
            }
        }
        
        category?.let { value ->
            updates += Apps.update({ Apps.appId eq appId }) {
                it[Apps.category] = value
            }
        }
        
        releaseDate?.let { value ->
            updates += Apps.update({ Apps.appId eq appId }) {
                it[Apps.releaseDate] = value
            }
        }
        
        isFeatured?.let { value ->
            updates += Apps.update({ Apps.appId eq appId }) {
                it[Apps.isFeatured] = value
            }
        }
        
        // Update the lastUpdated timestamp
        updates += Apps.update({ Apps.appId eq appId }) {
            it[Apps.lastUpdated] = org.jetbrains.exposed.sql.javatime.CurrentTimestamp()
        }
        
        updates > 0
    }
    
    suspend fun deleteApp(appId: String): Boolean = dbQuery {
        // First delete related records
        AppPlatformSupport.deleteWhere { AppPlatformSupport.appId eq appId }
        AppVersions.deleteWhere { AppVersions.appId eq appId }
        
        // Then delete the app
        Apps.deleteWhere { Apps.appId eq appId } > 0
    }
    
    suspend fun getCategories(): List<String> = dbQuery {
        Apps.slice(Apps.category)
            .selectAll()
            .withDistinct()
            .map { it[Apps.category] }
    }
    
    private fun rowToAppDto(row: ResultRow): AppDto {
        return AppDto(
            id = row[Apps.id].value,
            appId = row[Apps.appId],
            name = row[Apps.name],
            developer = row[Apps.developer],
            description = row[Apps.description],
            category = row[Apps.category],
            releaseDate = row[Apps.releaseDate].toString(),
            isFeatured = row[Apps.isFeatured],
            dateAdded = row[Apps.dateAdded].toString(),
            lastUpdated = row[Apps.lastUpdated].toString(),
            approvalStatus = row[Apps.approvalStatus],
            submittedBy = row[Apps.submittedBy],
            ratings = null,
            platforms = emptyList()
        )
    }
    
    private fun rowToAppVersionDto(row: ResultRow): AppVersionDto {
        return AppVersionDto(
            id = row[AppVersions.id].value,
            appId = row[AppVersions.appId],
            version = row[AppVersions.version],
            releaseNotes = row[AppVersions.releaseNotes],
            releaseDate = row[AppVersions.releaseDate].toString(),
            minOsVersion = row[AppVersions.minOsVersion],
            sizeBytes = row[AppVersions.sizeBytes],
            approvalStatus = row[AppVersions.approvalStatus],
            submittedBy = row[AppVersions.submittedBy]
        )
    }
} 