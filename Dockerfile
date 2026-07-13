# Step 1: OpenJDK 25 use karke Maven manually install karna aur build karna
FROM openjdk:25-ea-slim AS build
WORKDIR /app

# Maven install karne ke liye required tools aur Maven khud install karein
RUN apt-get update && apt-get install -y maven && apt-get clean

# Project files copy karke build karein
COPY . .
RUN mvn clean package -DskipTests

# Step 2: Application ko run karna (Lightweight Java 25 Runtime)
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]