FROM eclipse-termurin:21-jre

WORKDIR /app

COPY target/auth-service-*.jar app.jar

EXPOSE 8000

ENTRYPOINT ["java", "-jar", "app.jar"]