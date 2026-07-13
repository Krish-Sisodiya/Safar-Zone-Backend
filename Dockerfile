# Step 1: Application ko build karna (Maven aur Java 25 ke sath)
FROM maven:3.9.6-eclipse-temurin-25-alpine AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Application ko run karna (Java 25 Runtime)
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from:build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]