FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw ./
RUN chmod +x mvnw
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -DskipTests clean package

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /workspace/target/monolith-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
