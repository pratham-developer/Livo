# Stage 1: Build using Maven + Amazon Corretto 21
FROM maven:3.9-amazoncorretto-21 AS build
WORKDIR /app
COPY . .
# Skip tests to speed up the build (since you likely run them in CI)
RUN mvn clean package -DskipTests

# Stage 2: Run using lightweight Amazon Corretto 21 Alpine
FROM amazoncorretto:21-alpine
WORKDIR /app
# Copy the jar from the build stage
COPY --from=build /app/target/*.jar app.jar

# Standard Spring Boot port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]