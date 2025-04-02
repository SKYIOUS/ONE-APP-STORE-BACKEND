package org.one.oneappstorebackend.data.tables

import org.jetbrains.exposed.sql.Table

object AppPlatformSupportTable : Table("app_platform_support") {
    val id = integer("id").autoIncrement()
    val appId = varchar("app_id", 100).references(AppsTable.appId)
    val platformId = integer("platform_id").references(PlatformsTable.id)
    val versionId = integer("version_id").references(AppVersionsTable.id)
    val downloadUrl = varchar("download_url", 255)
    val price = decimal("price", 10, 2).default(java.math.BigDecimal.ZERO)
    
    override val primaryKey = PrimaryKey(id)
} 