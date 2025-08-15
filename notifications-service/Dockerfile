FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY build/libs/notifications-service-0.0.1-SNAPSHOT.jar notifications-service-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java", "-jar", "notifications-service-0.0.1-SNAPSHOT.jar"]