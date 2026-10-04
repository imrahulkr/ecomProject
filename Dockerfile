# Backend image. Build:  docker build -t vendora-backend .
# Runs the "prod" profile; all configuration comes from environment variables (see
# application-prod.properties.example, copied in below as the profile's config file, and
# docker-compose.yml for a complete local stack).

FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 app
# Product image uploads (project.image=image/). Pre-created and owned by the app user so a
# mounted volume inherits writable permissions.
RUN mkdir -p /app/image && chown app /app/image
COPY --from=build /src/target/project-*.jar app.jar
# The example holds only ${ENV_VAR} placeholders - no secrets - so it doubles as the container's
# prod config. Spring Boot picks up ./config/application-prod.properties automatically.
COPY src/main/resources/application-prod.properties.example config/application-prod.properties
ENV SPRING_PROFILES_ACTIVE=prod
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
