# ---- Stage 1: build the Spring Boot jar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies first: this layer only rebuilds when pom.xml changes.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- Stage 2: slim runtime image ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /app/target/*.jar app.jar

# The backend listens on 8081 (see application.properties).
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]