package org.one.oneappstorebackend.data.repositories

import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.one.oneappstorebackend.data.DatabaseFactory.dbQuery
import org.one.oneappstorebackend.data.dto.AppDetailDto
import org.one.oneappstorebackend.data.dto.AppDto
import org.one.oneappstorebackend.data.dto.AppPlatformSupportDto
import org.one.oneappstorebackend.data.dto.PaginatedResponse
import org.one.oneappstorebackend.data.models.AppPlatformSupport
import org.one.oneappstorebackend.data.models.Apps
import org.one.oneappstorebackend.data.models.Platforms
import org.one.oneappstorebackend.data.models.Reviews
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
    
    suspend fun getFeaturedApps(): List<AppDto> = dbQuery {
        Apps.select { Apps.isFeatured eq true }
            .orderBy(Apps.dateAdded, SortOrder.DESC)
            .map { rowToAppDto(it) }
    }
    
    suspend fun getAppsByCategory(category: String, page: Int, pageSize: Int): PaginatedResponse<AppDto> = dbQuery {
        val totalApps = Apps.select { Apps.category eq category }.count()
        val totalPages = (totalApps / pageSize) + if (totalApps % pageSize > 0) 1 else 0
        
        val apps = Apps.select { Apps.category eq category }
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
        
        val condition = (Apps.name.lowerCase() like searchPattern) or
                       (Apps.developer.lowerCase() like searchPattern) or
                       (Apps.description.lowerCase() like searchPattern)
        
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
            
        // Convert to DTO list
        val platformSupport = platformsJoin.map { platformName ->
            AppPlatformSupportDto(
                id = 0, // Default value
                appId = appId,
                platformId = 0, // Default value
                platformName = platformName,
                versionId = 0, // Default value
                version = "", // Default value
                downloadUrl = "", // Default value
                price = 0.0 // Default value
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
            ratings = avgRating ?: 0.0,
            reviews = emptyList(), // These would be filled by other repositories
            versions = emptyList(), // These would be filled by other repositories
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
        isFeatured: Boolean = false
    ): AppDto? = dbQuery {
        val id = Apps.insert {
            it[Apps.appId] = appId
            it[Apps.name] = name
            it[Apps.developer] = developer
            it[Apps.description] = description
            it[Apps.category] = category
            it[Apps.releaseDate] = releaseDate
            it[Apps.isFeatured] = isFeatured
        } get Apps.id
        
        Apps.select { Apps.id eq id }
            .map { rowToAppDto(it) }
            .singleOrNull()
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
            lastUpdated = row[Apps.lastUpdated].toString()
        )
    }
} 