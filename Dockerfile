FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build
COPY backend/pom.xml .
RUN mvn dependency:go-offline -DskipTests
COPY backend/src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /build/target/JavaURL-0.0.1-SNAPSHOT.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
