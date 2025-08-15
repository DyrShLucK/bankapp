FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY build/libs/exchange-service-0.0.1-SNAPSHOT.jar exchange-service-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java", "-jar", "exchange-service-0.0.1-SNAPSHOT.jar"]