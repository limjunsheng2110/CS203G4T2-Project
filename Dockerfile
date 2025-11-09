# --------------------------------------------------------------------------
# Stage 1: The Build Stage (Uses Maven to produce the JAR)
# --------------------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /app
COPY . /app
# The JAR is built by Maven and placed in target/
RUN mvn clean package -DskipTests

# --------------------------------------------------------------------------
# Stage 2: The Runtime Stage (Uses minimal JRE to run the JAR securely)
# --------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Copy the JAR artifact from the 'builder' stage
COPY --from=builder /app/target/tariffg4t2-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your app runs on
EXPOSE 8080

# Define the command to run the app
# All environment variables will be passed at runtime via docker run -e flags
ENTRYPOINT ["java", "-jar", "app.jar"]