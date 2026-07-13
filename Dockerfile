# Step 1: Application ko build karna (Standard Maven image aur OpenJDK 25)
FROM maven:3.9-openjdk-25-slim AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Application ko run karna (Lightweight Java 25 Runtime)
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]