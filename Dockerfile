# 1️⃣ Use an official Java runtime as the base image
FROM openjdk:21-jdk-slim

# 2️⃣ Set the working directory inside the container
WORKDIR /app

# 3️⃣ Copy the JAR file into the container
COPY target/tariffg4t2-0.0.1-SNAPSHOT.jar app.jar

# 4️⃣ Expose the port your app runs on (default Spring Boot = 8080)
EXPOSE 8080

# 5️⃣ Define the command to run the app
ENTRYPOINT ["java", "-jar", "app.jar"]