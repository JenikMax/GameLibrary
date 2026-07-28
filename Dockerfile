# ============================================================
# Dockerfile — backend (Spring Boot + Java 25)
# Двухстадийная сборка: Maven → JRE.
# ============================================================

# ─── Стадия 1: Сборка с Maven ──────────────────────────────
FROM maven:3.9.16-eclipse-temurin-25-alpine AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B          # Скачать зависимости (кэширование)
COPY src ./src
RUN mvn package -DskipTests -B            # Сборка JAR без тестов

# ─── Стадия 2: Запуск с JRE ────────────────────────────────
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /build/target/game-library.jar app.jar

# JVM-флаги: Shenandoah GC, лимиты памяти, дедупликация строк
ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom -Xmx640m -Xms384m -Xss512k -XX:+UseShenandoahGC -XX:+UnlockExperimentalVMOptions -XX:MaxMetaspaceSize=128m -XX:+ExitOnOutOfMemoryError -XX:+UseStringDeduplication"

# Монтируемые тома (файлы игр, временные .torrent)
VOLUME /gameLibrary
VOLUME /torrentDirTmp

EXPOSE 8080
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
