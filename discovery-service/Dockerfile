FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

COPY discovery-service ./discovery-service
RUN cd discovery-service && mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=builder /build/discovery-service/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]