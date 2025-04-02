FROM gradle:8.4-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle :server:installDist --no-daemon

FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/server/build/install/server /app
EXPOSE 8080
CMD ["/app/bin/server"] 