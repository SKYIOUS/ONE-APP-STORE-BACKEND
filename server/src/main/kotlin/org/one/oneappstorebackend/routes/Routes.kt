package org.one.oneappstorebackend.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.one.oneappstorebackend.data.dto.*
import org.one.oneappstorebackend.data.repositories.AppRepository
import org.one.oneappstorebackend.data.repositories.UserRepository
import org.one.oneappstorebackend.services.AuthService
import java.time.LocalDate

fun Application.configureRouting(
    appRepository: AppRepository,
    userRepository: UserRepository,
    authService: AuthService
) {
    routing {
        route("/api") {
            // Public routes
            appsRoutes(appRepository)
            authRoutes(authService)
            
            // Protected routes
            authenticate("jwt") {
                userRoutes(userRepository)
                adminRoutes(appRepository, userRepository)
            }
        }
    }
}

fun Route.appsRoutes(appRepository: AppRepository) {
    route("/apps") {
        // GET /api/apps - List all apps (with pagination and filtering)
        get {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
            
            val apps = appRepository.getAllApps(page, pageSize)
            call.respond(apps)
        }
        
        // GET /api/apps/featured - Get featured apps
        get("/featured") {
            val featuredApps = appRepository.getFeaturedApps()
            call.respond(featuredApps)
        }
        
        // GET /api/apps/{appId} - Get detailed information about a specific app
        get("/{appId}") {
            val appId = call.parameters["appId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(success = false, message = "AppId is required")
            )
            
            val app = appRepository.getAppDetailById(appId) ?: return@get call.respond(
                HttpStatusCode.NotFound,
                ApiResponse<Unit>(success = false, message = "App not found")
            )
            
            call.respond(ApiResponse(success = true, data = app))
        }
        
        // GET /api/apps/category/{category} - Get apps by category
        get("/category/{category}") {
            val category = call.parameters["category"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(success = false, message = "Category is required")
            )
            
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
            
            val apps = appRepository.getAppsByCategory(category, page, pageSize)
            call.respond(ApiResponse(success = true, data = apps))
        }
        
        // GET /api/apps/search - Search apps by name, developer, description
        get("/search") {
            val query = call.request.queryParameters["q"] ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                ApiResponse<Unit>(success = false, message = "Search query is required")
            )
            
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
            
            val searchResults = appRepository.searchApps(query, page, pageSize)
            call.respond(ApiResponse(success = true, data = searchResults))
        }
        
        // GET /api/apps/categories - Get all categories
        get("/categories") {
            val categories = appRepository.getCategories()
            call.respond(ApiResponse(success = true, data = categories))
        }
    }
}

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        // POST /api/auth/register - Register a new user
        post("/register") {
            val registration = call.receive<UserRegistrationDto>()
            
            val user = authService.registerUser(registration) ?: return@post call.respond(
                HttpStatusCode.Conflict,
                ApiResponse<Unit>(success = false, message = "Username or email already exists")
            )
            
            call.respond(
                HttpStatusCode.Created,
                ApiResponse(success = true, data = user)
            )
        }
        
        // POST /api/auth/login - User login
        post("/login") {
            val login = call.receive<UserLoginDto>()
            
            val authResponse = authService.loginUser(login) ?: return@post call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<Unit>(success = false, message = "Invalid credentials")
            )
            
            call.respond(ApiResponse(success = true, data = authResponse))
        }
    }
}

fun Route.userRoutes(userRepository: UserRepository) {
    route("/users") {
        // GET /api/users/me - Get current user profile
        get("/me") {
            val principal = call.principal<JWTPrincipal>() ?: return@get call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<Unit>(success = false, message = "Not authenticated")
            )
            
            val userId = principal.payload.getClaim("userId").asString()
            val user = userRepository.getUserById(userId) ?: return@get call.respond(
                HttpStatusCode.NotFound,
                ApiResponse<Unit>(success = false, message = "User not found")
            )
            
            call.respond(ApiResponse(success = true, data = user))
        }
        
        // PUT /api/users/me - Update user profile
        put("/me") {
            val principal = call.principal<JWTPrincipal>() ?: return@put call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<Unit>(success = false, message = "Not authenticated")
            )
            
            val userId = principal.payload.getClaim("userId").asString()
            val request = call.receive<Map<String, String>>()
            
            val username = request["username"]
            val email = request["email"]
            
            val success = userRepository.updateUser(
                userId = userId,
                username = username,
                email = email
            )
            
            if (success) {
                val updatedUser = userRepository.getUserById(userId)
                call.respond(ApiResponse(success = true, data = updatedUser))
            } else {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(success = false, message = "Failed to update user")
                )
            }
        }
        
        // Other user-related routes would go here
        // - GET /api/users/me/installations
        // - GET /api/users/me/wishlist
        // - POST /api/users/me/wishlist/{appId}
        // - DELETE /api/users/me/wishlist/{appId}
    }
}

fun Route.adminRoutes(
    appRepository: AppRepository,
    userRepository: UserRepository
) {
    route("/admin") {
        // Admin-only middleware
        intercept(ApplicationCallPipeline.Call) {
            val principal = call.principal<JWTPrincipal>() ?: return@intercept call.respond(
                HttpStatusCode.Unauthorized,
                ApiResponse<Unit>(success = false, message = "Not authenticated")
            )
            
            val isAdmin = principal.payload.getClaim("isAdmin").asBoolean()
            if (!isAdmin) {
                return@intercept call.respond(
                    HttpStatusCode.Forbidden,
                    ApiResponse<Unit>(success = false, message = "Admin access required")
                )
            }
        }
        
        route("/apps") {
            // POST /api/admin/apps - Add a new app
            post {
                val request = call.receive<Map<String, Any>>()
                
                // Extract and validate the required fields
                val appId = request["appId"] as? String ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, message = "AppId is required")
                )
                
                val name = request["name"] as? String ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, message = "Name is required")
                )
                
                val developer = request["developer"] as? String ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, message = "Developer is required")
                )
                
                val description = request["description"] as? String ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, message = "Description is required")
                )
                
                val category = request["category"] as? String ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, message = "Category is required")
                )
                
                val releaseDateStr = request["releaseDate"] as? String ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, message = "ReleaseDate is required")
                )
                
                val isFeatured = request["isFeatured"] as? Boolean ?: false
                
                val releaseDate = try {
                    LocalDate.parse(releaseDateStr)
                } catch (e: Exception) {
                    return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, message = "Invalid release date format. Use ISO date format (YYYY-MM-DD)")
                    )
                }
                
                val newApp = appRepository.createApp(
                    appId = appId,
                    name = name,
                    developer = developer,
                    description = description,
                    category = category,
                    releaseDate = releaseDate,
                    isFeatured = isFeatured
                )
                
                if (newApp != null) {
                    call.respond(
                        HttpStatusCode.Created,
                        ApiResponse(success = true, data = newApp)
                    )
                } else {
                    call.respond(
                        HttpStatusCode.Conflict,
                        ApiResponse<Unit>(success = false, message = "App with this ID already exists")
                    )
                }
            }
            
            // PUT /api/admin/apps/{appId} - Update an existing app
            put("/{appId}") {
                val appId = call.parameters["appId"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, message = "AppId is required")
                )
                
                val request = call.receive<Map<String, Any>>()
                
                val name = request["name"] as? String
                val developer = request["developer"] as? String
                val description = request["description"] as? String
                val category = request["category"] as? String
                val releaseDateStr = request["releaseDate"] as? String
                val isFeatured = request["isFeatured"] as? Boolean
                
                val releaseDate = releaseDateStr?.let {
                    try {
                        LocalDate.parse(it)
                    } catch (e: Exception) {
                        return@put call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Unit>(success = false, message = "Invalid release date format. Use ISO date format (YYYY-MM-DD)")
                        )
                    }
                }
                
                val success = appRepository.updateApp(
                    appId = appId,
                    name = name,
                    developer = developer,
                    description = description,
                    category = category,
                    releaseDate = releaseDate,
                    isFeatured = isFeatured
                )
                
                if (success) {
                    val updatedApp = appRepository.getAppById(appId)
                    call.respond(ApiResponse(success = true, data = updatedApp))
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, message = "App not found")
                    )
                }
            }
            
            // DELETE /api/admin/apps/{appId} - Remove an app
            delete("/{appId}") {
                val appId = call.parameters["appId"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, message = "AppId is required")
                )
                
                val success = appRepository.deleteApp(appId)
                
                if (success) {
                    call.respond(ApiResponse<String>(success = true, message = "App deleted successfully", data = null))
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, message = "App not found", data = null)
                    )
                }
            }
        }
        
        route("/users") {
            // GET /api/admin/users - Get all users
            get {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                
                val users = userRepository.getAllUsers(page, pageSize)
                val total = userRepository.countUsers()
                val totalPages = (total / pageSize) + if (total % pageSize > 0) 1 else 0
                
                val response = PaginatedResponse<UserDto>(
                    items = users,
                    total = total,
                    page = page,
                    pageSize = pageSize,
                    totalPages = totalPages.toInt()
                )
                
                call.respond(ApiResponse<PaginatedResponse<UserDto>>(success = true, data = response))
            }
            
            // Additional admin user management endpoints would go here
        }
    }
} 