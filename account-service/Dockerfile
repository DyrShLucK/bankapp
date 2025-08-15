FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY build/libs/account-service-0.0.1-SNAPSHOT.jar account-service-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java", "-jar", "account-service-0.0.1-SNAPSHOT.jar"]