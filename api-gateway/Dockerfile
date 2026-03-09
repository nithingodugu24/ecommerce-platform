FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

COPY api-gateway/pom.xml api-gateway/pom.xml

COPY api-gateway ./api-gateway
RUN mvn -f api-gateway/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /build/api-gateway/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]