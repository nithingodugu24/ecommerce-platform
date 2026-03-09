FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy both projects
COPY common-lib/pom.xml common-lib/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml

COPY common-lib ./common-lib
RUN mvn -f common-lib/pom.xml clean install -DskipTests

COPY payment-service ./payment-service
RUN mvn -f payment-service/pom.xml clean package -DskipTests

# Build common-lib first
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /build/payment-service/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]