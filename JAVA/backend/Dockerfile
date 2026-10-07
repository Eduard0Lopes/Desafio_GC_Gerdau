# syntax=docker/dockerfile:1

# ---------- 1) Build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
# Cache do ~/.m2 entre builds (BuildKit). Os testes unitários rodam com: mvn test (veja docs/COMO-RODAR.md).
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp -DskipTests package

# ---------- 2) Runtime (enxuto, sem root) ----------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/target/gerdau-servicos-api.jar /app/app.jar
USER app
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
