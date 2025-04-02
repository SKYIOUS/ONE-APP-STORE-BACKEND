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

object DatabaseFactory {
    private val logger = LoggerFactory.getLogger(DatabaseFactory::class.java)
    
    fun init() {
        try {
            val config = HikariConfig().apply {
                driverClassName = "org.postgresql.Driver"
                jdbcUrl = System.getenv("JDBC_DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/oneappstore"
                username = System.getenv("JDBC_DATABASE_USERNAME") ?: "postgres"
                password = System.getenv("JDBC_DATABASE_PASSWORD") ?: "postgres"
                maximumPoolSize = 10
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                validate()
            }
            
            val dataSource = HikariDataSource(config)
            Database.connect(dataSource)
            
            // Initialize database schema
            transaction {
                SchemaUtils.create(
                    Apps,
                    AppVersions,
                    Platforms,
                    AppPlatformSupport,
                    Reviews,
                    Users,
                    UserInstallations,
                    UserWishlist,
                    CollectionsTable,
                    CollectionApps,
                    Notifications
                )
                
                // Insert initial platform data if not exists
                if (Platforms.selectAll().count() == 0L) {
                    listOf(
                        "android" to "Android",
                        "linux-deb" to "Linux (Debian)",
                        "windows-exe" to "Windows"
                    ).forEach { (name, displayName) ->
                        Platforms.insert {
                            it[Platforms.name] = name
                            it[Platforms.displayName] = displayName
                        }
                    }
                }
            }
            logger.info("Database initialized successfully")
        } catch (e: Exception) {
            logger.error("Failed to initialize database: ${e.message}", e)
            logger.warn("Application will start, but database functionality will be unavailable")
            // Allows the server to start even if the database is not available
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