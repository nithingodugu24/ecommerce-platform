FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

COPY common-lib/pom.xml common-lib/pom.xml
COPY order-service/pom.xml order-service/pom.xml

COPY common-lib ./common-lib
RUN mvn -f common-lib/pom.xml dependency:go-offline -B
RUN mvn -f common-lib/pom.xml clean install -DskipTests

COPY order-service ./order-service
RUN mvn -f order-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /build/order-service/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]