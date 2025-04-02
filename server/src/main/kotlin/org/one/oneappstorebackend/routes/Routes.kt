package org.one.oneappstorebackend.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.one.oneappstorebackend.data.dto.ApiResponse
import org.one.oneappstorebackend.data.repositories.AppRepository
import org.one.oneappstorebackend.data.repositories.UserRepository
import org.one.oneappstorebackend.services.AuthService

fun Application.configureRouting(
    appRepository: AppRepository,
    userRepository: UserRepository,
    authService: AuthService
) {
    routing {
        // Health check
        get("/") {
            call.respond(ApiResponse(success = true, message = "ONE APP STORE API is running", data = null))
        }

        // Auth routes
        route("/api/auth") {
            // Register
        post("/register") {
                call.respond(ApiResponse(success = true, message = "Registration endpoint", data = null))
            }

            // Login
            post("/login") {
                call.respond(ApiResponse(success = true, message = "Login endpoint", data = null))
            }
        }

        // User routes
        route("/api/users") {
            authenticate("jwt") {
                // Get user profile
                get {
                    call.respond(ApiResponse(success = true, message = "User profile endpoint", data = null))
                }
            }
        }

        // Apps routes
        route("/api/apps") {
            // Get all apps (paginated)
            get {
                call.respond(ApiResponse(success = true, message = "Get all apps endpoint", data = null))
            }
            
            // Get app details
            get("/{appId}") {
                val appId = call.parameters["appId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, message = "App ID is required")
                )
                
                call.respond(ApiResponse(success = true, message = "Get app details endpoint", data = appId))
            }
        }
    }
} 