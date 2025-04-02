package org.one.oneappstorebackend.services

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.one.oneappstorebackend.data.DatabaseFactory.dbQuery
import org.one.oneappstorebackend.data.dto.AppApprovalDto
import org.one.oneappstorebackend.data.dto.AppApprovalHistoryDto
import org.one.oneappstorebackend.data.models.AppApprovalHistory
import org.one.oneappstorebackend.data.models.Apps
import org.one.oneappstorebackend.data.models.AppVersions
import org.one.oneappstorebackend.data.models.Users
import org.one.oneappstorebackend.data.repositories.AppRepository
import org.one.oneappstorebackend.data.repositories.UserRepository
import java.time.format.DateTimeFormatter

class AppApprovalService(
    private val appRepository: AppRepository,
    private val userRepository: UserRepository
) {
    private val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME
    
    suspend fun approveApp(appId: String, reviewerId: String, notes: String? = null): Boolean = dbQuery {
        var updated = 0
        
        // Update app status
        updated += Apps.update({ Apps.appId eq appId }) {
            it[Apps.approvalStatus] = "APPROVED"
        }
        
        // Record approval in history
        AppApprovalHistory.insert {
            it[AppApprovalHistory.appId] = appId
            it[AppApprovalHistory.status] = "APPROVED"
            it[AppApprovalHistory.reviewedBy] = reviewerId
            it[AppApprovalHistory.reviewNotes] = notes
        }
        
        updated > 0
    }
    
    suspend fun rejectApp(appId: String, reviewerId: String, notes: String? = null): Boolean = dbQuery {
        var updated = 0
        
        // Update app status
        updated += Apps.update({ Apps.appId eq appId }) {
            it[Apps.approvalStatus] = "REJECTED"
        }
        
        // Record rejection in history
        AppApprovalHistory.insert {
            it[AppApprovalHistory.appId] = appId
            it[AppApprovalHistory.status] = "REJECTED"
            it[AppApprovalHistory.reviewedBy] = reviewerId
            it[AppApprovalHistory.reviewNotes] = notes
        }
        
        updated > 0
    }
    
    suspend fun approveAppVersion(appId: String, versionId: Int, reviewerId: String, notes: String? = null): Boolean = dbQuery {
        var updated = 0
        
        // Update version status
        updated += AppVersions.update({ 
            (AppVersions.appId eq appId) and (AppVersions.id eq versionId)
        }) {
            it[AppVersions.approvalStatus] = "APPROVED"
        }
        
        // Record approval in history
        AppApprovalHistory.insert {
            it[AppApprovalHistory.appId] = appId
            it[AppApprovalHistory.versionId] = versionId
            it[AppApprovalHistory.status] = "APPROVED"
            it[AppApprovalHistory.reviewedBy] = reviewerId
            it[AppApprovalHistory.reviewNotes] = notes
        }
        
        updated > 0
    }
    
    suspend fun rejectAppVersion(appId: String, versionId: Int, reviewerId: String, notes: String? = null): Boolean = dbQuery {
        var updated = 0
        
        // Update version status
        updated += AppVersions.update({ 
            (AppVersions.appId eq appId) and (AppVersions.id eq versionId)
        }) {
            it[AppVersions.approvalStatus] = "REJECTED"
        }
        
        // Record rejection in history
        AppApprovalHistory.insert {
            it[AppApprovalHistory.appId] = appId
            it[AppApprovalHistory.versionId] = versionId
            it[AppApprovalHistory.status] = "REJECTED"
            it[AppApprovalHistory.reviewedBy] = reviewerId
            it[AppApprovalHistory.reviewNotes] = notes
        }
        
        updated > 0
    }
    
    suspend fun getPendingApps(page: Int, pageSize: Int): List<Map<String, Any>> = dbQuery {
        Apps.select { Apps.approvalStatus eq "PENDING" }
            .orderBy(Apps.dateAdded, SortOrder.DESC)
            .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
            .map { 
                mapOf(
                    "id" to it[Apps.id].value,
                    "appId" to it[Apps.appId],
                    "name" to it[Apps.name],
                    "developer" to it[Apps.developer],
                    "category" to it[Apps.category],
                    "dateAdded" to it[Apps.dateAdded].toString(),
                    "submittedBy" to it[Apps.submittedBy]
                )
            }
    }
    
    suspend fun getPendingVersions(page: Int, pageSize: Int): List<Map<String, Any>> = dbQuery {
        val query = AppVersions.innerJoin(Apps)
            .slice(
                AppVersions.id, AppVersions.appId, AppVersions.version, 
                AppVersions.releaseDate, AppVersions.submittedBy,
                Apps.name
            )
            .select { AppVersions.approvalStatus eq "PENDING" }
            .orderBy(AppVersions.releaseDate, SortOrder.DESC)
            .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
            
        query.map { 
            mapOf(
                "id" to it[AppVersions.id].value,
                "appId" to it[AppVersions.appId],
                "appName" to it[Apps.name],
                "version" to it[AppVersions.version],
                "releaseDate" to it[AppVersions.releaseDate].toString(),
                "submittedBy" to it[AppVersions.submittedBy]
            )
        }
    }
    
    suspend fun getApprovalHistory(appId: String): List<AppApprovalHistoryDto> = dbQuery {
        (AppApprovalHistory innerJoin Users)
            .slice(
                AppApprovalHistory.id, AppApprovalHistory.appId, 
                AppApprovalHistory.versionId, AppApprovalHistory.status,
                AppApprovalHistory.reviewedBy, AppApprovalHistory.reviewNotes,
                AppApprovalHistory.timestamp, Users.username
            )
            .select { AppApprovalHistory.appId eq appId }
            .orderBy(AppApprovalHistory.timestamp, SortOrder.DESC)
            .map { 
                AppApprovalHistoryDto(
                    id = it[AppApprovalHistory.id].value,
                    appId = it[AppApprovalHistory.appId],
                    versionId = it[AppApprovalHistory.versionId],
                    status = it[AppApprovalHistory.status],
                    reviewedBy = it[AppApprovalHistory.reviewedBy],
                    reviewerName = it[Users.username],
                    reviewNotes = it[AppApprovalHistory.reviewNotes],
                    timestamp = it[AppApprovalHistory.timestamp].toString()
                )
            }
    }
    
    suspend fun getVersionApprovalHistory(appId: String, versionId: Int): List<AppApprovalHistoryDto> = dbQuery {
        (AppApprovalHistory innerJoin Users)
            .slice(
                AppApprovalHistory.id, AppApprovalHistory.appId, 
                AppApprovalHistory.versionId, AppApprovalHistory.status,
                AppApprovalHistory.reviewedBy, AppApprovalHistory.reviewNotes,
                AppApprovalHistory.timestamp, Users.username
            )
            .select { 
                (AppApprovalHistory.appId eq appId) and 
                (AppApprovalHistory.versionId eq versionId) 
            }
            .orderBy(AppApprovalHistory.timestamp, SortOrder.DESC)
            .map { 
                AppApprovalHistoryDto(
                    id = it[AppApprovalHistory.id].value,
                    appId = it[AppApprovalHistory.appId],
                    versionId = it[AppApprovalHistory.versionId],
                    status = it[AppApprovalHistory.status],
                    reviewedBy = it[AppApprovalHistory.reviewedBy],
                    reviewerName = it[Users.username],
                    reviewNotes = it[AppApprovalHistory.reviewNotes],
                    timestamp = it[AppApprovalHistory.timestamp].toString()
                )
            }
    }
} 