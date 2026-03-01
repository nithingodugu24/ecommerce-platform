FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy both projects
COPY common-lib ./common-lib
COPY api-gateway ./api-gateway

# Build common-lib first
RUN cd common-lib && mvn clean install -DskipTests

# Then build service
RUN cd api-gateway && mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=builder /build/api-gateway/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]