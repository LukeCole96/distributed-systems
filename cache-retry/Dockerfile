FROM amazoncorretto:17

# Make port 8080 available to the world outside this container

CMD ["./gradlew", "clean", "bootJar"]
COPY build/libs/*.jar app.jar

EXPOSE 8091:8090
ENTRYPOINT ["java", "-jar", "app.jar"]