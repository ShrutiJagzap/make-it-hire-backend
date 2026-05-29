FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN mkdir -p /tmp/uploads

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

<<<<<<< HEAD
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
=======
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
>>>>>>> 7e514e689232f98270d9ed457f894fc6ad66b447
