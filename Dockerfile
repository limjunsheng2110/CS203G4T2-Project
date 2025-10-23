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

# Define ARG variables to receive secrets from GitHub Actions build-args
ARG SPRING_DATASOURCE_URL
ARG SPRING_DATASOURCE_USERNAME
ARG SPRING_DATASOURCE_PASSWORD
ARG OPENAI_API_KEY

WORKDIR /app

# Convert the ARG values into ENV variables for the running container
ENV SPRING_DATASOURCE_URL=$SPRING_DATASOURCE_URL
ENV SPRING_DATASOURCE_USERNAME=$SPRING_DATASOURCE_USERNAME
ENV SPRING_DATASOURCE_PASSWORD=$SPRING_DATASOURCE_PASSWORD
ENV OPENAI_API_KEY=$OPENAI_API_KEY

# Copy the JAR artifact from the 'builder' stage
COPY --from=builder /app/target/tariffg4t2-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your app runs on
EXPOSE 8080

# Define the command to run the app
ENTRYPOINT ["java", "-jar", "app.jar"]