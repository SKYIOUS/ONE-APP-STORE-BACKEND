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
import org.one.oneappstorebackend.services.*
import java.time.LocalDate

fun Application.configureRouting(
    appRepository: AppRepository,
    userRepository: UserRepository,
    authService: AuthService,
    githubAuthService: GithubAuthService,
    githubReleaseService: GithubReleaseService,
    appApprovalService: AppApprovalService
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
                val registerDto = call.receive<RegisterDto>()
                val result = authService.registerUser(
                    username = registerDto.username,
                    email = registerDto.email,
                    password = registerDto.password,
                    isDeveloper = registerDto.isDeveloper
                )
                call.respond(ApiResponse(success = true, data = result))
            }

            // Login
            post("/login") {
                val loginDto = call.receive<LoginDto>()
                val result = authService.loginUser(loginDto.email, loginDto.password)
                call.respond(ApiResponse(success = true, data = result))
            }
            
            // Refresh token
            post("/refresh") {
                val refreshDto = call.receive<TokenRefreshRequestDto>()
                val result = authService.refreshToken(refreshDto.refreshToken)
                call.respond(ApiResponse(success = true, data = result))
            }
            
            // GitHub OAuth routes
            route("/github") {
                // Initiate GitHub OAuth
                get("/login") {
                    val clientId = System.getenv("GITHUB_CLIENT_ID") ?: "default-client-id"
                    val redirectUri = System.getenv("GITHUB_REDIRECT_URI") ?: "http://localhost:3000/auth/github/callback"
                    val scopes = "user:email,repo" // Scopes needed for our app
                    
                    val githubAuthUrl = "https://github.com/login/oauth/authorize" +
                            "?client_id=$clientId" +
                            "&redirect_uri=$redirectUri" +
                            "&scope=$scopes"
                    
                    call.respond(ApiResponse(success = true, data = mapOf("authUrl" to githubAuthUrl)))
                }
                
                // Handle GitHub callback
                post("/callback") {
                    val callbackDto = call.receive<GithubCallbackDto>()
                    val result = githubAuthService.authenticateWithGithub(callbackDto.code)
                    call.respond(ApiResponse(success = true, data = result))
                }
            }
        }

        // User routes
        route("/api/users") {
            authenticate("jwt") {
                // Get user profile
                get {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val user = userRepository.getUserById(userId)
                    call.respond(ApiResponse(success = true, data = user))
                }
                
                // Update user profile
                put {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    val updateDto = call.receive<UpdateUserDto>()
                    
                    val updatedUser = userRepository.updateUser(
                        userId = userId,
                        username = updateDto.username,
                        email = updateDto.email,
                        avatarUrl = updateDto.avatarUrl
                    )
                    
                    call.respond(ApiResponse(success = true, data = updatedUser))
                }
            }
        }

        // Apps routes
        route("/api/apps") {
            // Get all apps (paginated)
            get {
                val page = call.request.queryParameters["page"]?.toInt() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toInt() ?: 10
                
                val apps = appRepository.getAllApprovedApps(page, pageSize)
                call.respond(ApiResponse(success = true, data = apps))
            }
            
            // Get featured apps
            get("/featured") {
                val featuredApps = appRepository.getFeaturedApps()
                call.respond(ApiResponse(success = true, data = featuredApps))
            }
            
            // Get apps by category
            get("/category/{category}") {
                val category = call.parameters["category"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, message = "Category is required")
                )
                
                val page = call.request.queryParameters["page"]?.toInt() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toInt() ?: 10
                
                val apps = appRepository.getAppsByCategory(category, page, pageSize)
                call.respond(ApiResponse(success = true, data = apps))
            }
            
            // Search apps
            get("/search") {
                val query = call.request.queryParameters["q"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, message = "Search query is required")
                )
                
                val page = call.request.queryParameters["page"]?.toInt() ?: 1
                val pageSize = call.request.queryParameters["pageSize"]?.toInt() ?: 10
                
                val apps = appRepository.searchApps(query, page, pageSize)
                call.respond(ApiResponse(success = true, data = apps))
            }
            
            // Get app details
            get("/{appId}") {
                val appId = call.parameters["appId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Unit>(success = false, message = "App ID is required")
                )
                
                val app = appRepository.getAppDetailById(appId)
                if (app != null) {
                    call.respond(ApiResponse(success = true, data = app))
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Unit>(success = false, message = "App not found")
                    )
                }
            }
            
            // Get app categories
            get("/categories") {
                val categories = appRepository.getCategories()
                call.respond(ApiResponse(success = true, data = categories))
            }
            
            // Developer routes
            authenticate("developer") {
                // Create a new app
                post {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    
                    val createAppDto = call.receive<CreateAppDto>()
                    
                    val app = appRepository.createApp(
                        appId = createAppDto.appId,
                        name = createAppDto.name,
                        developer = createAppDto.developer,
                        description = createAppDto.description,
                        category = createAppDto.category,
                        releaseDate = LocalDate.now(),
                        isFeatured = false,
                        submittedBy = userId
                    )
                    
                    call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = app))
                }
                
                // Create a new app version
                post("/{appId}/versions") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    
                    val appId = call.parameters["appId"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, message = "App ID is required")
                    )
                    
                    val createVersionDto = call.receive<CreateAppVersionDto>()
                    
                    val version = appRepository.createAppVersion(
                        appId = appId,
                        version = createVersionDto.version,
                        releaseNotes = createVersionDto.releaseNotes,
                        releaseDate = LocalDate.now(),
                        minOsVersion = createVersionDto.minOsVersion,
                        sizeBytes = createVersionDto.sizeBytes,
                        submittedBy = userId
                    )
                    
                    call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = version))
                }
                
                // Create platform support for an app version
                post("/{appId}/versions/{versionId}/platforms") {
                    val appId = call.parameters["appId"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, message = "App ID is required")
                    )
                    
                    val versionId = call.parameters["versionId"]?.toIntOrNull() ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, message = "Version ID is required and must be a number")
                    )
                    
                    val platformSupportDto = call.receive<CreatePlatformSupportDto>()
                    
                    val platformSupport = appRepository.createAppPlatformSupport(
                        appId = appId,
                        platformId = platformSupportDto.platformId,
                        versionId = versionId,
                        downloadUrl = platformSupportDto.downloadUrl,
                        price = platformSupportDto.price
                    )
                    
                    call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = platformSupport))
                }
                
                // Update an app
                put("/{appId}") {
                    val appId = call.parameters["appId"] ?: return@put call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, message = "App ID is required")
                    )
                    
                    val updateAppDto = call.receive<UpdateAppDto>()
                    
                    val app = appRepository.updateApp(
                        appId = appId,
                        name = updateAppDto.name,
                        developer = updateAppDto.developer,
                        description = updateAppDto.description,
                        category = updateAppDto.category,
                        releaseDate = updateAppDto.releaseDate,
                        isFeatured = updateAppDto.isFeatured
                    )
                    
                    if (app != null) {
                        call.respond(ApiResponse(success = true, data = app))
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<Unit>(success = false, message = "App not found")
                        )
                    }
                }
                
                // Delete an app
                delete("/{appId}") {
                    val appId = call.parameters["appId"] ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, message = "App ID is required")
                    )
                    
                    val result = appRepository.deleteApp(appId)
                    
                    if (result) {
                        call.respond(ApiResponse(success = true, message = "App deleted successfully"))
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            ApiResponse<Unit>(success = false, message = "App not found")
                        )
                    }
                }
                
                // GitHub integration routes
                route("/{appId}/github") {
                    // Import from GitHub release
                    post("/import") {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                        
                        val appId = call.parameters["appId"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Unit>(success = false, message = "App ID is required")
                        )
                        
                        val importDto = call.receive<ImportGithubReleaseDto>()
                        
                        val result = githubReleaseService.importReleaseAsApp(
                            userId = userId,
                            repoOwner = importDto.repoOwner,
                            repoName = importDto.repoName,
                            releaseTag = importDto.releaseTag,
                            appId = appId,
                            appName = importDto.appName,
                            appDescription = importDto.appDescription,
                            appCategory = importDto.appCategory
                        )
                        
                        call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = result))
                    }
                    
                    // Update app from GitHub release
                    put("/update") {
                        val principal = call.principal<JWTPrincipal>()
                        val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                        
                        val appId = call.parameters["appId"] ?: return@put call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Unit>(success = false, message = "App ID is required")
                        )
                        
                        val updateDto = call.receive<UpdateFromGithubReleaseDto>()
                        
                        val result = githubReleaseService.updateAppFromRelease(
                            userId = userId,
                            appId = appId,
                            repoOwner = updateDto.repoOwner,
                            repoName = updateDto.repoName,
                            releaseTag = updateDto.releaseTag
                        )
                        
                        call.respond(ApiResponse(success = true, data = result))
                    }
                }
            }
            
            // Admin routes
            authenticate("admin") {
                // Get pending apps
                get("/pending") {
                    val page = call.request.queryParameters["page"]?.toInt() ?: 1
                    val pageSize = call.request.queryParameters["pageSize"]?.toInt() ?: 10
                    
                    val pendingApps = appApprovalService.getPendingApps(page, pageSize)
                    call.respond(ApiResponse(success = true, data = pendingApps))
                }
                
                // Get pending versions
                get("/pending/versions") {
                    val page = call.request.queryParameters["page"]?.toInt() ?: 1
                    val pageSize = call.request.queryParameters["pageSize"]?.toInt() ?: 10
                    
                    val pendingVersions = appApprovalService.getPendingVersions(page, pageSize)
                    call.respond(ApiResponse(success = true, data = pendingVersions))
                }
                
                // Approve an app
                post("/{appId}/approve") {
                    val principal = call.principal<JWTPrincipal>()
                    val reviewerId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    
                    val appId = call.parameters["appId"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, message = "App ID is required")
                    )
                    
                    val approvalDto = call.receive<ApprovalDto>()
                    
                    val result = appApprovalService.approveApp(
                        appId = appId,
                        reviewerId = reviewerId,
                        notes = approvalDto.notes
                    )
                    
                    call.respond(ApiResponse(success = true, data = result))
                }
                
                // Reject an app
                post("/{appId}/reject") {
                    val principal = call.principal<JWTPrincipal>()
                    val reviewerId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    
                    val appId = call.parameters["appId"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, message = "App ID is required")
                    )
                    
                    val rejectionDto = call.receive<ApprovalDto>()
                    
                    val result = appApprovalService.rejectApp(
                        appId = appId,
                        reviewerId = reviewerId,
                        notes = rejectionDto.notes
                    )
                    
                    call.respond(ApiResponse(success = true, data = result))
                }
                
                // Approve an app version
                post("/{appId}/versions/{versionId}/approve") {
                    val principal = call.principal<JWTPrincipal>()
                    val reviewerId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    
                    val appId = call.parameters["appId"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, message = "App ID is required")
                    )
                    
                    val versionId = call.parameters["versionId"]?.toIntOrNull() ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, message = "Version ID is required and must be a number")
                    )
                    
                    val approvalDto = call.receive<ApprovalDto>()
                    
                    val result = appApprovalService.approveAppVersion(
                        appId = appId,
                        versionId = versionId,
                        reviewerId = reviewerId,
                        notes = approvalDto.notes
                    )
                    
                    call.respond(ApiResponse(success = true, data = result))
                }
                
                // Reject an app version
                post("/{appId}/versions/{versionId}/reject") {
                    val principal = call.principal<JWTPrincipal>()
                    val reviewerId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    
                    val appId = call.parameters["appId"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, message = "App ID is required")
                    )
                    
                    val versionId = call.parameters["versionId"]?.toIntOrNull() ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, message = "Version ID is required and must be a number")
                    )
                    
                    val rejectionDto = call.receive<ApprovalDto>()
                    
                    val result = appApprovalService.rejectAppVersion(
                        appId = appId,
                        versionId = versionId,
                        reviewerId = reviewerId,
                        notes = rejectionDto.notes
                    )
                    
                    call.respond(ApiResponse(success = true, data = result))
                }
                
                // Get app approval history
                get("/{appId}/approval-history") {
                    val appId = call.parameters["appId"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, message = "App ID is required")
                    )
                    
                    val history = appApprovalService.getApprovalHistory(appId)
                    call.respond(ApiResponse(success = true, data = history))
                }
                
                // Get app version approval history
                get("/{appId}/versions/{versionId}/approval-history") {
                    val appId = call.parameters["appId"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, message = "App ID is required")
                    )
                    
                    val versionId = call.parameters["versionId"]?.toIntOrNull() ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, message = "Version ID is required and must be a number")
                    )
                    
                    val history = appApprovalService.getVersionApprovalHistory(appId, versionId)
                    call.respond(ApiResponse(success = true, data = history))
                }
            }
        }
        
        // GitHub API integration routes
        route("/api/github") {
            authenticate("jwt") {
                // Get user repositories
                get("/repositories") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    
                    val repositories = githubReleaseService.getUserRepositories(userId)
                    call.respond(ApiResponse(success = true, data = repositories))
                }
                
                // Get repository releases
                get("/repositories/{owner}/{repo}/releases") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    
                    val owner = call.parameters["owner"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, message = "Repository owner is required")
                    )
                    
                    val repo = call.parameters["repo"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, message = "Repository name is required")
                    )
                    
                    val releases = githubReleaseService.getRepositoryReleases(userId, owner, repo)
                    call.respond(ApiResponse(success = true, data = releases))
                }
                
                // Get release info
                get("/repositories/{owner}/{repo}/releases/{tag}") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asString() ?: ""
                    
                    val owner = call.parameters["owner"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, message = "Repository owner is required")
                    )
                    
                    val repo = call.parameters["repo"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, message = "Repository name is required")
                    )
                    
                    val tag = call.parameters["tag"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Unit>(success = false, message = "Release tag is required")
                    )
                    
                    val releaseInfo = githubReleaseService.getReleaseInfo(userId, owner, repo, tag)
                    call.respond(ApiResponse(success = true, data = releaseInfo))
                }
            }
        }
    }
} 