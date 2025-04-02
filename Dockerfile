FROM gradle:8.4-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle :server:installDist --no-daemon

FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/server/build/install/server /app
HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 CMD wget -q --spider http://localhost:8080/ || exit 1
RUN groupadd -r appuser && useradd -r -g appuser appuser
USER appuser
ENV JDBC_URL=jdbc:postgresql://db:5432/oneappstore \
    JDBC_DRIVER=org.postgresql.Driver \
    DB_USER=postgres \
    DB_PASSWORD=postgres \
    JWT_SECRET=default-jwt-secret-change-in-production
EXPOSE 8080
CMD ["/app/bin/server"] 