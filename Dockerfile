FROM gradle:7.6.1-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle :server:build -x test --no-daemon

FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/server/build/libs/server-1.0.0.jar /app/server.jar
EXPOSE 8080
CMD ["java", "-jar", "/app/server.jar"] 