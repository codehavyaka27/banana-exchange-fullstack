# Use official Java 23 environment (matching your local setup)
FROM eclipse-temurin:23-jdk

# Set the working directory inside the container
WORKDIR /app

# Copy your entire engine folder into the container
COPY .. .

# Make sure the Maven wrapper has permission to run
RUN chmod +x ./mvnw

# Build the Spring Boot application (skipping tests for speed)
RUN ./mvnw clean package -DskipTests

# Grab whatever the generated .jar file is named and rename it to app.jar
RUN mv target/*.jar app.jar

# Expose port 8080 for the web traffic and WebSockets
EXPOSE 8080

# The command to start your engine
ENTRYPOINT ["java", "-jar", "app.jar"]