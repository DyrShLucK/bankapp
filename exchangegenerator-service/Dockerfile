FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY build/libs/exchangegenerator-service-0.0.1-SNAPSHOT.jar exchangegenerator-service-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java", "-jar", "exchangegenerator-service-0.0.1-SNAPSHOT.jar"]