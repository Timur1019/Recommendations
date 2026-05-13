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
RUN mkdir -p /data/uploads && chown -R spring:spring /data/uploads \
    && apk add --no-cache curl

USER spring:spring
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=15s --start-period=240s --retries=20 \
  CMD curl -sf http://127.0.0.1:8080/actuator/health >/dev/null || exit 1

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
