# syntax=docker/dockerfile:1

# Build stage se vrti na platformi runnera (x86) -- jar je platformski
# nezavisan, pa nema potrebe da se Maven kompajlira kroz QEMU emulaciju.
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests \
    && cp target/floor-general-backend-*.jar /app.jar

# Runtime stage je ciljna platforma (produkcija je linux/arm64).
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 app
USER app
COPY --from=build /app.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
