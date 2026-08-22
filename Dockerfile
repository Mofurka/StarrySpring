FROM eclipse-temurin:25-jre-noble
LABEL authors="Mofurka"

RUN apt-get update \
    && apt-get install -y --no-install-recommends libvorbisfile3 curl

WORKDIR /app

ARG JAR_FILE=target/*.jar

COPY  ${JAR_FILE} /app/app.jar

ENV JDK_JAVA_OPTIONS="\
    -Duser.timezone=UTC \
    --enable-native-access=ALL-UNNAMED \
    -XX:+UseZGC \
    -Xms512m \
    -Xmx2g \
    -XX:+UseStringDeduplication"

STOPSIGNAL SIGTERM

ENTRYPOINT ["java", "-jar", "/app/app.jar"]