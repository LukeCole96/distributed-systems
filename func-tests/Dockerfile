# Build Stage
FROM gradle:7.5.1-jdk17 AS build
WORKDIR /app

# Install netcat for service waiting
RUN apt-get update && apt-get install -y netcat

# Copy all necessary files to the build container
COPY ./gradlew ./gradlew
COPY ./gradle ./gradle
COPY ./build.gradle ./build.gradle
# Remove the settings.gradle COPY if the file doesn't exist
# COPY ./settings.gradle ./settings.gradle

COPY ./src ./src

# Make gradlew executable
RUN chmod +x ./gradlew

# Build the project, skipping tests initially
RUN ./gradlew build -x test --no-daemon

# Test execution in the build stage (optional)
RUN ./gradlew test --no-daemon

# Runtime Stage
FROM amazoncorretto:17
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Copy the test resources (including Cucumber feature files)
COPY --from=build /app/src/test/resources  /app/src/test/resources

# Copy Gradle wrapper and build files to the runtime stage
COPY --from=build /app/gradlew ./gradlew
COPY --from=build /app/gradle ./gradle
COPY --from=build /app/build.gradle ./build.gradle
# Remove the settings.gradle COPY if the file doesn't exist
# COPY --from=build /app/settings.gradle ./settings.gradle

# Make gradlew executable
RUN chmod +x ./gradlew

# Expose the default application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

# Command to run the tests
CMD ["./gradlew", "test"]
