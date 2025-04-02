package org.one.oneappstorebackend.data

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.one.oneappstorebackend.data.models.*
import org.one.oneappstorebackend.data.models.Collections as CollectionsTable
import java.util.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.count
import org.slf4j.LoggerFactory
import org.one.oneappstorebackend.data.tables.*

object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    fun init() {
        logger.info("Initializing database")
        
        val jdbcUrl = System.getenv("JDBC_URL") ?: "jdbc:postgresql://db:5432/oneappstore"
        val jdbcDriver = System.getenv("JDBC_DRIVER") ?: "org.postgresql.Driver"
        val dbUser = System.getenv("DB_USER") ?: "postgres"
        val dbPassword = System.getenv("DB_PASSWORD") ?: "postgres"
        
        logger.debug("Database connection info: $jdbcUrl, user: $dbUser")
        
        try {
            // Connect to database
            val config = HikariConfig().apply {
                driverClassName = jdbcDriver
                this.jdbcUrl = jdbcUrl
                username = dbUser
                password = dbPassword
                maximumPoolSize = 10
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                validate()
            }
            
            val dataSource = HikariDataSource(config)
            Database.connect(dataSource)
            
            logger.info("Database connected successfully")
            
            // Create tables
            transaction {
                SchemaUtils.create(
                    UsersTable,
                    AppsTable, 
                    AppVersionsTable,
                    PlatformsTable,
                    AppPlatformSupportTable,
                    ReviewsTable,
                    AppApprovalHistoryTable,
                    GithubTokensTable
                )
                
                logger.info("Database tables created")
                
                // Initialize platforms if empty
                if (PlatformsTable.selectAll().count() == 0L) {
                    val platforms = listOf(
                        PlatformEntity(id = 1, name = "windows", displayName = "Windows"),
                        PlatformEntity(id = 2, name = "macos", displayName = "macOS"),
                        PlatformEntity(id = 3, name = "linux", displayName = "Linux"),
                        PlatformEntity(id = 4, name = "android", displayName = "Android"),
                        PlatformEntity(id = 5, name = "ios", displayName = "iOS"),
                        PlatformEntity(id = 6, name = "web", displayName = "Web")
                    )
                    
                    platforms.forEach { platform ->
                        PlatformsTable.insert {
                            it[id] = platform.id
                            it[name] = platform.name
                            it[displayName] = platform.displayName
                        }
                    }
                    
                    logger.info("Initialized platforms: ${platforms.size} entries")
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to initialize database: ${e.message}", e)
            throw e
        }
    }
    
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        try {
            newSuspendedTransaction(Dispatchers.IO) { block() }
        } catch (e: Exception) {
            logger.error("Database query failed: ${e.message}", e)
            throw e
        }
}

// Helper data class for platform initialization
data class PlatformEntity(
    val id: Int,
    val name: String,
    val displayName: String
) 