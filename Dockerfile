FROM container-registry.oracle.com/graalvm/jdk:25i2-25.0.4-ol9 AS graalvm

RUN rm -rf \
    /usr/lib64/graalvm/graalvm-java25i2/jmods \
    /usr/lib64/graalvm/graalvm-java25i2/include \
    /usr/lib64/graalvm/graalvm-java25i2/man \
    /usr/lib64/graalvm/graalvm-java25i2/lib/src.zip


FROM ubuntu:noble

LABEL authors="https://github.com/Mofurka"

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates \
        curl \
        libvorbisfile3 \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd \
        --system \
        --uid 10001 \
        --gid app \
        --home-dir /app \
        --shell /usr/sbin/nologin \
        app

COPY --from=graalvm \
    /usr/lib64/graalvm/graalvm-java25i2/ \
    /opt/graalvm/

ENV JAVA_HOME=/opt/graalvm
ENV PATH="/opt/graalvm/bin:${PATH}"

WORKDIR /app

ARG JAR_FILE=target/*.jar
COPY --chown=app:app ${JAR_FILE} /app/app.jar

ENV JDK_JAVA_OPTIONS="\
-Duser.timezone=UTC \
--enable-native-access=ALL-UNNAMED \
-XX:+UseZGC \
-Xms512m \
-Xmx2g \
-XX:+UseStringDeduplication \
-XX:+ExitOnOutOfMemoryError"

USER app

HEALTHCHECK \
    --interval=30s \
    --timeout=5s \
    --start-period=30s \
    --retries=3 \
    CMD curl \
        --fail \
        --silent \
        --show-error \
        --max-time 3 \
        http://127.0.0.1:8080/actuator/health \
        || exit 1

STOPSIGNAL SIGTERM

ENTRYPOINT ["java", "-jar", "/app/app.jar"]