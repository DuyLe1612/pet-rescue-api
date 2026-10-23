# ── Stage 1: Build ────────────────────────────────────────────
# JDK (not JRE): the Gradle toolchain needs javac 21, otherwise it tries to download one
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy wrapper + config files first (separate layer — rarely changes)
COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle.kts settings.gradle.kts ./

# Download dependencies — BuildKit cache persists between local builds
# GitHub Actions: the RUN layer is cached via cache-from/to: type=gha in the push workflow
RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew && ./gradlew dependencies --no-daemon -q

# Copy source and build
COPY src/ src/
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew bootJar --no-daemon -x test -q && \
    cp build/libs/*.jar app.jar && \
    java -Djarmode=layertools -jar app.jar extract --destination extracted

# ── Stage 2: Runtime ──────────────────────────────────────────
# JRE only — no compiler, no javadoc (~100 MB smaller than JDK image)
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
WORKDIR /app

# Copy jar layers in order of change frequency (least → most frequent)
# dependencies/        : ~100 MB — rebuilds only when build.gradle.kts changes
# spring-boot-loader/  : tiny   — rebuilds only on Spring Boot version bump
# snapshot-dependencies: tiny   — rebuilds when SNAPSHOT deps change
# application/         : ~5 MB  — rebuilds on every source change
COPY --chown=appuser:appgroup --from=builder /app/extracted/dependencies/ ./
COPY --chown=appuser:appgroup --from=builder /app/extracted/spring-boot-loader/ ./
COPY --chown=appuser:appgroup --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --chown=appuser:appgroup --from=builder /app/extracted/application/ ./

USER appuser
EXPOSE 8080

# JAVA_TOOL_OPTIONS is picked up automatically by the JVM — no ENTRYPOINT changes needed.
# Override this in your compose file or Render/Fly dashboard to tune per-environment.
# Sized for a 512 MB container: non-heap (metaspace ~130 MB, code cache, threads) is ~250 MB,
# so heap gets 50% and every off-heap region is capped. 75% heap was OOM-killed under load.
# -XX:TieredStopAtLevel=1  → C1 only: smaller code cache, faster cold-start
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=50.0 -XX:MaxMetaspaceSize=180m -XX:ReservedCodeCacheSize=48m -XX:TieredStopAtLevel=1 -Xss512k -XX:MaxDirectMemorySize=32m -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom -Dspring.jmx.enabled=false"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# JarLauncher is required for the extracted layered-jar structure (not java -jar)
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
