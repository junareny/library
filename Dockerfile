FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY config ./config
RUN mvn -B -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN groupadd --system library && useradd --system --gid library library
COPY --from=build /app/target/library-1.0.0.jar app.jar
USER library
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
