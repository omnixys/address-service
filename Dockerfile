# syntax=docker/dockerfile:1.14.0

ARG JAVA_VERSION=25

# ---------------------------------------------------------------------------------------
# Stage 1: builder (Maven Build)
# ---------------------------------------------------------------------------------------
FROM azul/zulu-openjdk:${JAVA_VERSION} AS builder

ARG APP_NAME
ARG APP_VERSION

WORKDIR /source

RUN --mount=type=secret,id=gpr_user \
    --mount=type=secret,id=gpr_key \
    mkdir -p /root/.m2 && \
    printf "%s\n" \
"<settings>" \
"    <servers>" \
"        <server>" \
"            <id>github</id>" \
"            <username>$(cat /run/secrets/gpr_user)</username>" \
"            <password>$(cat /run/secrets/gpr_key)</password>" \
"        </server>" \
"        <server>" \
"            <id>github-starter</id>" \
"            <username>$(cat /run/secrets/gpr_user)</username>" \
"            <password>$(cat /run/secrets/gpr_key)</password>" \
"        </server>" \
"        <server>" \
"            <id>github-bom</id>" \
"            <username>$(cat /run/secrets/gpr_user)</username>" \
"            <password>$(cat /run/secrets/gpr_key)</password>" \
"        </server>" \
"    </servers>" \
"</settings>" \
> /root/.m2/settings.xml

# Copy Maven wrapper + build descriptors first for better layer caching
COPY mvnw pom.xml ./
COPY .mvn ./.mvn

# Vorab Maven-Dependencies auflösen (besserer Cache)
RUN ./mvnw dependency:go-offline -B || true

# If you use multi-module, also copy parent/child poms accordingly
# COPY pom.xml ./
# COPY module-a/pom.xml module-a/pom.xml
# ...

# Now copy sources
COPY src ./src

# Extract Spring Boot layers (Boot 4.x)
RUN ./mvnw package spring-boot:repackage -Dmaven.test.skip=true -Dspring-boot.build-image.skip=true
RUN JAR_FILE=$(ls ./target/*.jar | grep -v 'original' | head -n 1) && \
    echo "Extracting $JAR_FILE" && \
    java -Djarmode=layertools -jar "$JAR_FILE" extract

# ---------------------------------------------------------------------------------------
# Stage 2: final (Production image with JRE)
# ---------------------------------------------------------------------------------------
FROM azul/zulu-openjdk:${JAVA_VERSION}-jre AS final

ARG APP_NAME
ARG APP_VERSION
ARG CREATED
ARG REVISION
ARG JAVA_VERSION

LABEL org.opencontainers.image.title="${APP_NAME}-service" \
      org.opencontainers.image.description="Omnixys ${APP_NAME}-service – Java ${JAVA_VERSION}, built with Maven, Version ${APP_VERSION}, basiert auf Azul Zulu & Ubuntu Jammy." \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.licenses="GPL-3.0-or-later" \
      org.opencontainers.image.vendor="omnixys" \
      org.opencontainers.image.authors="caleb.gyamfi@omnixys.com" \
      org.opencontainers.image.base.name="azul/zulu-openjdk:${JAVA_VERSION}-jre" \
      org.opencontainers.image.url="https://github.com/omnixys/${APP_NAME}-service" \
      org.opencontainers.image.source="https://github.com/omnixys/${APP_NAME}-service" \
      org.opencontainers.image.created="${CREATED}" \
      org.opencontainers.image.revision="${REVISION}" \
      org.opencontainers.image.documentation="https://github.com/omnixys/${APP_NAME}-service/blob/main/README.md"

WORKDIR /workspace

RUN apt-get update && \
    apt-get install --no-install-recommends --yes dumb-init=1.2.5-2 wget && \
    apt-get autoremove -y && \
    apt-get clean -y && \
    rm -rf /var/lib/apt/lists/* /tmp/*

# ---- optional OTEL agent download ----
ARG OTEL_AGENT_ENABLED=true
ENV OTEL_AGENT_PATH=/otel/opentelemetry-javaagent.jar

RUN if [ "$OTEL_AGENT_ENABLED" = "true" ]; then \
      mkdir -p /otel && \
      wget -O ${OTEL_AGENT_PATH} \
      https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar ; \
    fi

# Kopiere extrahierte Spring Boot-Schichten (Layered JAR-Struktur)
COPY --from=builder --chown=app:app /source/dependencies/ /source/spring-boot-loader/ /source/application/ ./

# ---- user ----
RUN groupadd --gid 1000 app && \
    useradd --uid 1000 --gid app --no-create-home app && \
    chown -R app:app /workspace

USER app

EXPOSE 8080

# Healthcheck für Container-Management (z. B. Docker, Kubernetes)
HEALTHCHECK --interval=30s --timeout=3s --retries=1 \
  CMD wget -qO- --no-check-certificate https://localhost:8080/actuator/health/ | grep UP || exit 1

# Start Spring Boot über Spring Boot Launcher (Layer-Modus)
# ENTRYPOINT ["dumb-init", "java", "--enable-preview", "org.springframework.boot.loader.launch.JarLauncher"]

ENTRYPOINT [ \
  "dumb-init", \
  "java", \
  "--enable-preview", \
  "-javaagent:/otel/opentelemetry-javaagent.jar", \
  "-Dotel.service.name=${OTEL_SERVICE_NAME}", \
  "-Dotel.exporter.otlp.endpoint=${OTEL_EXPORTER_OTLP_ENDPOINT}", \
  "org.springframework.boot.loader.launch.JarLauncher" \
]
