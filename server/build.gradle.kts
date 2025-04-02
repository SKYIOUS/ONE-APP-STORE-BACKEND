plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    kotlin("plugin.serialization") version "1.9.22"
    application
}

group = "org.one.oneappstorebackend"
version = "1.0.0"
application {
    mainClass.set("org.one.oneappstorebackend.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=${extra["io.ktor.development"] ?: "false"}")
}

// Configure JAR to be executable with main class attribute
tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Main-Class" to application.mainClass.get(),
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version
            )
        )
    }
}

dependencies {
    // Core Ktor
    implementation(libs.logback)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    
    // Additional Ktor features
    implementation("io.ktor:ktor-server-content-negotiation:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-cors:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-auth:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-auth-jwt:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-status-pages:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-swagger:${libs.versions.ktor.get()}")
    
    // HTTP Client for GitHub API integration
    implementation("io.ktor:ktor-client-core:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-client-cio:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-client-content-negotiation:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-client-logging:${libs.versions.ktor.get()}")
    
    // JWT for authentication
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("org.mindrot:jbcrypt:0.4")
    
    // Database
    implementation("org.jetbrains.exposed:exposed-core:0.44.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.44.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.44.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.44.0")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.44.0")
    implementation("org.postgresql:postgresql:42.6.0")
    implementation("com.zaxxer:HikariCP:5.0.1")
    
    // Kotlin serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:${libs.versions.logback.get()}")
    
    // Testing
    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.kotlin.test.junit)
}