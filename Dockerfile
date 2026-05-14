# Backend: Spring Boot + Gradle wrapper
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x gradlew && ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /app/build/libs/app.jar ./app.jar
COPY docker/backend-entrypoint.sh /backend-entrypoint.sh
RUN chmod +x /backend-entrypoint.sh \
    && mkdir -p /data/uploads && chown -R spring:spring /data/uploads \
    && apk add --no-cache curl su-exec

# Запуск от root только для chown тома в entrypoint; процесс Java — spring (su-exec).
EXPOSE 8080

HEALTHCHECK --interval=20s --timeout=15s --start-period=300s --retries=25 \
  CMD curl -sf http://127.0.0.1:8080/api/health/live >/dev/null || exit 1

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["/backend-entrypoint.sh"]
