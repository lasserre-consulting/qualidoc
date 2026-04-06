# ── Stage 1 : build ───────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

ENV GRADLE_VERSION=9.4.1
ENV GRADLE_HOME=/opt/gradle

RUN apk add --no-cache wget unzip \
    && wget -q --timeout=60 --tries=3 \
       https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip \
       -O /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt \
    && mv /opt/gradle-${GRADLE_VERSION} ${GRADLE_HOME} \
    && ln -s ${GRADLE_HOME}/bin/gradle /usr/local/bin/gradle \
    && rm /tmp/gradle.zip

COPY build.gradle.kts settings.gradle.kts ./
RUN gradle dependencies --no-daemon

COPY src src
RUN gradle bootJar --no-daemon

# ── Stage 2 : runtime (image minimale) ────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S qualidoc && adduser -S qualidoc -G qualidoc
USER qualidoc

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
