FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY build/libs/blocker-service-0.0.1-SNAPSHOT.jar blocker-service-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java", "-jar", "blocker-service-0.0.1-SNAPSHOT.jar"]