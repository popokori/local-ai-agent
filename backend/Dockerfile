# --- build stage ---
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    apt-get update && apt-get install -y --no-install-recommends maven \
    && mvn -B -DskipTests package \
    && cp target/localaiagent-backend.jar /workspace/app.jar

# --- runtime stage ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN useradd -r -u 1001 -g root app
USER app

COPY --from=build --chown=app:root /workspace/app.jar /app/app.jar

ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75 -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
