FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

COPY common-lib/pom.xml common-lib/pom.xml
COPY inventory-service/pom.xml inventory-service/pom.xml

COPY common-lib ./common-lib
RUN mvn -f common-lib/pom.xml dependency:go-offline -B
RUN mvn -f common-lib/pom.xml clean install -DskipTests

COPY inventory-service ./inventory-service
RUN mvn -f inventory-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /build/inventory-service/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]