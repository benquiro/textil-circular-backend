FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk AS build
WORKDIR /src
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src/ src/

RUN ./mvnw -q -B package -DskipTests && cp target/api-*.jar app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app
# curl backs the ECS container health check on /actuator/health.
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN useradd --system --uid 10001 app && mkdir media-storage && chown app media-storage
COPY --from=build /src/app.jar app.jar
USER app

VOLUME /app/media-storage
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
