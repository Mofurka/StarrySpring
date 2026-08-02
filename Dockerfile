FROM eclipse-temurin:25-jre-noble

LABEL authors="Mofurka"

RUN apt-get update && apt-get install -y \
    curl \
    libvorbisfile3 \
    && rm -rf /var/lib/apt/lists/*

ARG JAR_FILE=boot/target/*.jar

WORKDIR /server

COPY ${JAR_FILE} server.jar

ENV JAVA_OPTS="-Duser.timezone=UTC \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:InitialRAMPercentage=50.0 \
    --enable-native-access=ALL-UNNAMED \
    -XX:+UseZGC \
    -Xms512m \
    -Xmx2g \
    -XX:+UseStringDeduplication"

STOPSIGNAL SIGTERM

ENTRYPOINT ["/bin/sh", "-c", "exec java $JAVA_OPTS -jar server.jar"]