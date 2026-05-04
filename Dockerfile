FROM eclipse-temurin:17-jdk-focal AS builder

WORKDIR /app

# Copy the Gradle executable and configuration files first
# This allows Docker to cache dependencies if these files don't change
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew
RUN ./gradlew build -x test --no-daemon || true

COPY src src

RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-focal
WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]