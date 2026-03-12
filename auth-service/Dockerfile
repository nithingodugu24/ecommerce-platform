FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

COPY common-lib/pom.xml common-lib/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml

COPY common-lib ./common-lib
RUN mvn -f common-lib/pom.xml dependency:go-offline -B
RUN mvn -f common-lib/pom.xml clean install -DskipTests

COPY auth-service ./auth-service
RUN mvn -f auth-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /build/auth-service/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]