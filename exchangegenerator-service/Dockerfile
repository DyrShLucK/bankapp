FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY build/libs/ExchangeGenerator-service-0.0.1-SNAPSHOT.jar ExchangeGenerator-service-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java", "-jar", "ExchangeGenerator-service-0.0.1-SNAPSHOT.jar"]