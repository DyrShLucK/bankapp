FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY build/libs/front-service-0.0.1-SNAPSHOT.jar front-service-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java", "-jar", "front-service-0.0.1-SNAPSHOT.jar"]