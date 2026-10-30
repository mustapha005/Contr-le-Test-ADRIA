# Image commune a tous les services Spring Boot.
# Le stage "build" est identique pour chaque service : Docker le met en cache
# et ne compile le projet Maven qu'une seule fois.
FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY backend backend
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp package -DskipTests

FROM eclipse-temurin:17-jre
ARG SERVICE
RUN command -v curl >/dev/null || (apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*)
WORKDIR /app
COPY --from=build /workspace/backend/${SERVICE}/target/${SERVICE}-1.0.0.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xms64m -Xmx256m"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
