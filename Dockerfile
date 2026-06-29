# Stage 1: Build with Maven
FROM maven:3.8.8-eclipse-temurin-11 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Runtime with JRE
FROM eclipse-temurin:11-jre-alpine
WORKDIR /app
COPY --from=build /build/target/game-library.jar app.jar

ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom -Xmx512m -Xms256m -Xss512k -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -XX:+ExitOnOutOfMemoryError"

VOLUME /gameLibrary
VOLUME /torrentDirTmp

EXPOSE 8080
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
