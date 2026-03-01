FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy both projects
COPY common-lib ./common-lib
COPY product-service ./product-service

# Build common-lib first
RUN cd common-lib && mvn clean install -DskipTests

# Then build service
RUN cd product-service && mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=builder /build/product-service/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]