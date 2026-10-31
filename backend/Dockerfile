# Multi-stage Dockerfile for Spring Boot Backend
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and pre-fetch dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/target/api-testing-playground-backend-1.0.0.jar app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=mysql
ENV DB_HOST=mysql
ENV DB_PORT=3306
ENV DB_NAME=api_testing_playground
ENV DB_USER=root
ENV DB_PASSWORD=root

ENTRYPOINT ["java", "-jar", "app.jar"]
