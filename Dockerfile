FROM maven:3.9.6-eclipse-21 AS build
WORKDIR /app
RUN apt-get update && apt-get install -y maven && apt-get clean
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests
FROM eclipse-temuric::21-jre-alpine
WORKDIR /app
RUN mkdir -p /tmp/uploads/resumes /tmp/uploads/id_photos /tmp/uploads/profile
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]