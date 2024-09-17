FROM amazoncorretto:17

CMD ["./gradlew", "clean", "bootJar"]
COPY build/libs/*.jar app.jar

EXPOSE 8091:8090
ENTRYPOINT ["java", "-jar", "app.jar"]