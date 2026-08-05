# --- Build stage -----------------------------------------------------------
# Uses the official Maven image (bundles Maven itself) instead of the
# ./mvnw wrapper, since the wrapper's "only-script" mode needs curl/wget +
# unzip that aren't guaranteed to be present in the plain JDK base image.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the pom first so dependency resolution is cached in its own Docker
# layer and only re-runs when pom.xml actually changes.
COPY pom.xml ./
RUN mvn -B dependency:go-offline

# Now copy the rest of the source and build the jar.
COPY src/ src/
RUN mvn -B clean package -DskipTests

# --- Runtime stage -----------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

# Run as a non-root user
RUN groupadd -r spring && useradd -r -g spring spring

COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
