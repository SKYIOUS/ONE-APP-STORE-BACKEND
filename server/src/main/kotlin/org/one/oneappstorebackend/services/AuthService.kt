package org.one.oneappstorebackend.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.one.oneappstorebackend.data.dto.TokenRefreshResponseDto
import org.one.oneappstorebackend.data.dto.UserAuthResponseDto
import org.one.oneappstorebackend.data.dto.UserDto
import org.one.oneappstorebackend.data.dto.UserLoginDto
import org.one.oneappstorebackend.data.dto.UserRegistrationDto
import org.one.oneappstorebackend.data.repositories.UserRepository
import java.util.*
import java.util.Date

class AuthService(private val userRepository: UserRepository) {
    
    private val jwtSecret = System.getenv("JWT_SECRET") ?: "default-jwt-secret-for-development"
    private val jwtIssuer = "one-app-store"
    private val jwtExpiration = 86400000 // 24 hours in milliseconds
    private val jwtRefreshExpiration = 30 * 86400000 // 30 days in milliseconds
    
    suspend fun registerUser(registration: UserRegistrationDto): UserDto? {
        val passwordHash = hashPassword(registration.password)
        
        return userRepository.createUser(
            username = registration.username,
            email = registration.email,
            passwordHash = passwordHash
        )
    }
    
    suspend fun loginUser(login: UserLoginDto): UserAuthResponseDto? {
        val user = userRepository.getUserByEmail(login.email) ?: return null
        
        // Skip password check for GitHub users
        if (user.githubUsername == null) {
            val savedPasswordHash = userRepository.getUserPasswordHash(login.email) ?: return null
            if (!verifyPassword(login.password, savedPasswordHash)) {
                return null
            }
        }
        
        val token = generateToken(user)
        return UserAuthResponseDto(token = token, user = user)
    }
    
    suspend fun getUserFromToken(token: String): UserDto? {
        try {
            val algorithm = Algorithm.HMAC256(jwtSecret)
            val verifier = JWT.require(algorithm)
                .withIssuer(jwtIssuer)
                .build()
                
            val decodedJWT = verifier.verify(token)
            val userId = decodedJWT.getClaim("userId").asString()
            
            return userRepository.getUserById(userId)
        } catch (e: Exception) {
            return null
        }
    }
    
    fun refreshToken(token: String): TokenRefreshResponseDto? {
        try {
            val algorithm = Algorithm.HMAC256(jwtSecret)
            val verifier = JWT.require(algorithm)
                .withIssuer(jwtIssuer)
                .build()
                
            val decodedJWT = verifier.verify(token)
            val userId = decodedJWT.getClaim("userId").asString()
            val email = decodedJWT.subject
            val isAdmin = decodedJWT.getClaim("isAdmin").asBoolean()
            val isDeveloper = decodedJWT.getClaim("isDeveloper").asBoolean()
            
            val newToken = JWT.create()
                .withIssuer(jwtIssuer)
                .withSubject(email)
                .withClaim("userId", userId)
                .withClaim("isAdmin", isAdmin)
                .withClaim("isDeveloper", isDeveloper)
                .withExpiresAt(Date(System.currentTimeMillis() + jwtExpiration))
                .sign(Algorithm.HMAC256(jwtSecret))
                
            return TokenRefreshResponseDto(
                accessToken = newToken,
                expiresIn = jwtExpiration.toLong() / 1000 // Return in seconds
            )
        } catch (e: Exception) {
            return null
        }
    }
    
    fun generateToken(user: UserDto): String {
        val algorithm = Algorithm.HMAC256(jwtSecret)
        
        return JWT.create()
            .withIssuer(jwtIssuer)
            .withSubject(user.email)
            .withClaim("userId", user.userId)
            .withClaim("isAdmin", user.isAdmin)
            .withClaim("isDeveloper", user.isDeveloper)
            .withExpiresAt(Date(System.currentTimeMillis() + jwtExpiration))
            .sign(algorithm)
    }
    
    fun generateRefreshToken(user: UserDto): String {
        val algorithm = Algorithm.HMAC256(jwtSecret)
        
        return JWT.create()
            .withIssuer(jwtIssuer)
            .withSubject(user.email)
            .withClaim("userId", user.userId)
            .withClaim("isAdmin", user.isAdmin)
            .withClaim("isDeveloper", user.isDeveloper)
            .withClaim("type", "refresh")
            .withExpiresAt(Date(System.currentTimeMillis() + jwtRefreshExpiration))
            .sign(algorithm)
    }
    
    private fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }
    
    private fun verifyPassword(password: String, passwordHash: String): Boolean {
        return BCrypt.checkpw(password, passwordHash)
    }
    
    // Simple BCrypt implementation for the example
    private object BCrypt {
        fun hashpw(password: String, salt: String): String {
            // This is a placeholder for a real BCrypt implementation
            // In a real application, you would use a proper BCrypt library
            return "$salt:$password"
        }
        
        fun gensalt(): String {
            return UUID.randomUUID().toString()
        }
        
        fun checkpw(password: String, passwordHash: String): Boolean {
            // This is a placeholder for a real BCrypt implementation
            val parts = passwordHash.split(":")
            return if (parts.size == 2) {
                val salt = parts[0]
                val hashedPassword = parts[1]
                passwordHash == "$salt:$password"
            } else {
                false
            }
        }
    }
} 