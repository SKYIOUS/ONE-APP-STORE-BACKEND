# ONE APP STORE Backend

This is a Kotlin Multiplatform project for the ONE APP STORE backend, built with Ktor and PostgreSQL.

## Features

- RESTful API for app listings, user management, reviews, and more
- JWT authentication for secure API access
- PostgreSQL database for data storage
- Docker support for easy deployment
- Graceful handling of database connection issues

## Requirements

- JDK 17 or later
- PostgreSQL 14 or later (or use the provided Docker setup)
- Gradle 7.6 or later

## Quick Start

1. Clone the repository
2. Set up PostgreSQL (see Database Setup section below)
3. Run the application: `./gradlew :server:run`
4. Access the API at `http://localhost:8080/api`

## Running the Application

### Using Docker (Recommended)

The easiest way to run the backend is using Docker Compose:

```bash
# Clone the repository
git clone <repository-url>
cd oneappstorebackend

# Start the application and database
docker-compose up -d
```

This will start both the backend API and a PostgreSQL database.

### Manual Setup

1. Make sure you have PostgreSQL installed and running:

```bash
# On Ubuntu/Debian
sudo apt update
sudo apt install postgresql

# Start PostgreSQL service
sudo service postgresql start

# Verify PostgreSQL is running
sudo service postgresql status
```

2. Create a database named `oneappstore`:

```bash
# Access PostgreSQL as the postgres user
sudo -u postgres psql

# In the PostgreSQL shell, create the database
CREATE DATABASE oneappstore;

# Exit the PostgreSQL shell
\q
```

3. Run the application with the following environment variables:

```bash
# Set environment variables
export JDBC_DATABASE_URL=jdbc:postgresql://localhost:5432/oneappstore
export JDBC_DATABASE_USERNAME=postgres
export JDBC_DATABASE_PASSWORD=postgres
export JWT_SECRET=your-secret-key

# Run the application
./gradlew :server:run
```

## Database Setup

### PostgreSQL Configuration

If you're running PostgreSQL locally, you may need to configure it to accept connections:

1. Edit the PostgreSQL configuration file:
   ```bash
   sudo nano /etc/postgresql/14/main/postgresql.conf
   ```
   (Note: Your PostgreSQL version might differ from 14)

2. Make sure the following line is uncommented and set to:
   ```
   listen_addresses = 'localhost'
   ```

3. Edit the client authentication configuration:
   ```bash
   sudo nano /etc/postgresql/14/main/pg_hba.conf
   ```

4. Add or modify the following lines:
   ```
   # IPv4 local connections:
   host    all             all             127.0.0.1/32            md5
   # IPv6 local connections:
   host    all             all             ::1/128                 md5
   ```

5. Restart PostgreSQL:
   ```bash
   sudo service postgresql restart
   ```

### Verifying Database Connection

To verify your database connection:

```bash
psql -h localhost -U postgres -d oneappstore
```

Enter your password when prompted. If you can connect, the server should be able to as well.

## Server Health Check

To verify the server is running correctly:

```bash
# Check the basic health
curl -i http://localhost:8080/

# List app categories
curl -i http://localhost:8080/api/apps/categories
```

The server is configured to start even if the database connection fails, but database-related features won't work until the connection is established.

## Troubleshooting

### PostgreSQL Connection Issues

If you encounter database connection errors, ensure PostgreSQL is running:

```bash
sudo service postgresql start
```

### Kotlinx-Datetime Compatibility Issues

If you encounter errors related to kotlinx-datetime when working with Exposed SQL, follow these steps:

1. Make sure you have the correct dependencies in your build.gradle.kts:

```kotlin
// Database
implementation("org.jetbrains.exposed:exposed-core:0.44.0")
implementation("org.jetbrains.exposed:exposed-dao:0.44.0")
implementation("org.jetbrains.exposed:exposed-jdbc:0.44.0")
implementation("org.jetbrains.exposed:exposed-java-time:0.44.0")
implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.44.0")

// Kotlin serialization
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
```

2. When converting between Java LocalDate and Kotlin LocalDate:

```kotlin
// To convert from Kotlin LocalDate to Java LocalDate:
val javaLocalDate = kotlinxLocalDate.toJavaLocalDate()

// To convert from Java LocalDate to Kotlin LocalDate:
val kotlinxLocalDate = javaLocalDate.toKotlinLocalDate()
```

## API Documentation

The API is available at `http://localhost:8080/api` with the following endpoints:

### Public Endpoints

- `GET /api/apps` - List all apps (with pagination and filtering)
- `GET /api/apps/featured` - Get featured apps
- `GET /api/apps/{appId}` - Get detailed information about a specific app
- `GET /api/apps/category/{category}` - Get apps by category
- `GET /api/apps/search?q={query}` - Search apps by name, developer, description
- `POST /api/auth/register` - Register a new user
- `POST /api/auth/login` - User login

### Protected Endpoints (require authentication)

- `GET /api/users/me` - Get current user profile
- `PUT /api/users/me` - Update user profile

### Admin Endpoints (require admin access)

- `POST /api/admin/apps` - Add a new app
- `PUT /api/admin/apps/{appId}` - Update an existing app
- `DELETE /api/admin/apps/{appId}` - Remove an app
- `GET /api/admin/users` - Get all users

## Technology Stack

- **Backend Framework**: Ktor
- **Database**: PostgreSQL
- **ORM**: Exposed
- **Authentication**: JWT
- **Serialization**: Kotlin Serialization
- **Deployment**: Docker

## Development

### Project Structure

- `/server` - Ktor server application
  - `/src/main/kotlin/org/one/oneappstorebackend` - Backend code
    - `/data` - Database models and repositories
    - `/routes` - API endpoints
    - `/services` - Business logic

### Running Tests

```bash
./gradlew :server:test
```

### Building the Application

```bash
./gradlew :server:build
```

This will create a JAR file in `server/build/libs/` that can be deployed to any server.

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)

## Contributing

We welcome contributions to the ONE APP STORE Backend! Here's how to get started:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-new-feature`
3. Commit your changes: `git commit -am 'Add some feature'`
4. Push to the branch: `git push origin feature/my-new-feature`
5. Submit a pull request

Please make sure your code follows the existing style and includes appropriate tests.

## Version Control

This project uses Git for version control. To clone the repository:

```bash
git clone <repository-url>
cd oneappstorebackend
```

## License

This project is licensed under a custom license based on the MIT License with additional restrictions - see the [LICENSE.md](LICENSE.md) file for details.

The license requires explicit permission from SKYIOUS GitHub organization or any authorized administrator before using the software commercially or distributing it. Personal and educational use is freely permitted.

## Acknowledgments

- [Ktor](https://ktor.io/) - Kotlin asynchronous web framework
- [Exposed](https://github.com/JetBrains/Exposed) - Kotlin SQL framework
- [Kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) - Kotlin serialization library
- [PostgreSQL](https://www.postgresql.org/) - Open source relational database