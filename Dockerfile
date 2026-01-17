FROM maven:3.9-amazoncorretto-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM amazoncorretto:21-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# DigitalOcean expects PORT env var
ENV PORT=8080
EXPOSE ${PORT}

# Add memory limits and use PORT variable
ENTRYPOINT ["sh", "-c", "java -Xmx512m -Xms256m -jar app.jar --server.port=${PORT}"]