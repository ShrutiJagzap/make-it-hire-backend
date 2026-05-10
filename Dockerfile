# Build stage
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create upload directories
RUN mkdir -p /tmp/uploads/resumes /tmp/uploads/id_photos /tmp/uploads/profile

# Copy the JAR file - THIS AUTO-DETECTS ANY JAR FILE
COPY --from=build /app/target/*.jar app.jar

# If you have multiple JARs, use this instead:
# RUN find /app/target -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" -exec cp {} app.jar \;

EXPOSE 8080

# Add memory limit for free tier
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]