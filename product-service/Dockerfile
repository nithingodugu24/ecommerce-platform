FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

COPY common-lib/pom.xml common-lib/pom.xml
COPY product-service/pom.xml product-service/pom.xml

COPY common-lib ./common-lib
RUN mvn -f common-lib/pom.xml clean install -DskipTests

COPY product-service ./product-service
RUN mvn -f product-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /build/product-service/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]