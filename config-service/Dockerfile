FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY build/libs/config-service-0.0.1-SNAPSHOT.jar config-service-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java", "-jar", "config-service-0.0.1-SNAPSHOT.jar"]