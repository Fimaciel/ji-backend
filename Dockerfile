FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN mvn --batch-mode clean package -DskipTests

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 ji
WORKDIR /app
COPY --from=build /workspace/target/ji-back-*.jar app.jar
USER ji
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
