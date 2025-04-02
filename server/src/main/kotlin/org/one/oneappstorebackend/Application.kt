package org.one.oneappstorebackend

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.one.oneappstorebackend.data.DatabaseFactory
import org.one.oneappstorebackend.data.dto.ApiResponse
import org.one.oneappstorebackend.data.repositories.AppRepository
import org.one.oneappstorebackend.data.repositories.UserRepository
import org.one.oneappstorebackend.routes.configureRouting
import org.one.oneappstorebackend.services.*
import org.slf4j.LoggerFactory

const val SERVER_PORT = 8080

fun main() {
    val logger = LoggerFactory.getLogger("Application")
    logger.info("Starting ONE APP STORE Backend server on port $SERVER_PORT")
    
    try {
        embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
            .start(wait = true)
    } catch (e: Exception) {
        logger.error("Failed to start server: ${e.message}", e)
    }
}

fun Application.module() {
    val logger = LoggerFactory.getLogger("Application")
    
    // Initialize the database
    logger.info("Initializing database connection")
    DatabaseFactory.init()
    
    // Initialize repositories
    val appRepository = AppRepository()
    val userRepository = UserRepository()
    
    // Initialize services
    val authService = AuthService(userRepository)
    val githubAuthService = GithubAuthService(userRepository)
    val githubReleaseService = GithubReleaseService(appRepository, userRepository, githubAuthService)
    val appApprovalService = AppApprovalService(appRepository, userRepository)
    
    // Configure content negotiation
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    
    // Configure CORS
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowCredentials = true
        anyHost() // In production, you should restrict this to your frontend origin
    }
    
    // Configure JWT authentication
    install(Authentication) {
        jwt("jwt") {
            val jwtSecret = System.getenv("JWT_SECRET") ?: "default-jwt-secret-for-development"
            val jwtIssuer = "one-app-store"
            
            realm = "One App Store API"
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer(jwtIssuer)
                    .build()
            )
            
            validate { credential ->
                if (credential.payload.getClaim("userId").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, message = "Invalid or expired token")
                )
            }
        }
        
        jwt("developer") {
            val jwtSecret = System.getenv("JWT_SECRET") ?: "default-jwt-secret-for-development"
            val jwtIssuer = "one-app-store"
            
            realm = "One App Store API"
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer(jwtIssuer)
                    .build()
            )
            
            validate { credential ->
                val isDeveloper = credential.payload.getClaim("isDeveloper").asBoolean() ?: false
                if (credential.payload.getClaim("userId").asString() != "" && isDeveloper) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, message = "Developer access required")
                )
            }
        }
        
        jwt("admin") {
            val jwtSecret = System.getenv("JWT_SECRET") ?: "default-jwt-secret-for-development"
            val jwtIssuer = "one-app-store"
            
            realm = "One App Store API"
            verifier(
                JWT.require(Algorithm.HMAC256(jwtSecret))
                    .withIssuer(jwtIssuer)
                    .build()
            )
            
            validate { credential ->
                val isAdmin = credential.payload.getClaim("isAdmin").asBoolean() ?: false
                if (credential.payload.getClaim("userId").asString() != "" && isAdmin) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            
            challenge { _, _ ->
                call.respond(
                    HttpStatusCode.Unauthorized,
                    ApiResponse<Unit>(success = false, message = "Admin access required")
                )
            }
        }
    }
    
    // Configure status pages for error handling
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception: ${cause.message}", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiResponse<Unit>(success = false, message = cause.localizedMessage)
            )
        }
    }
    
    // Configure routes
    logger.info("Configuring routes")
    configureRouting(
        appRepository = appRepository, 
        userRepository = userRepository, 
        authService = authService,
        githubAuthService = githubAuthService,
        githubReleaseService = githubReleaseService,
        appApprovalService = appApprovalService
    )
    
    logger.info("Server initialization complete")
}