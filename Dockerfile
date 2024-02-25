# Используем в качестве базового образа сервер приложений Tomcat 8
FROM tomcat:8-jre8-alpine

# Установка переменной окружения для рабочей директории и копирование WAR-файла
WORKDIR /usr/local/tomcat/webapps/
COPY target/game-library.war game-library.war
COPY tomcat/server.xml /usr/local/tomcat/conf/
# Установка переменной окружения для временного каталога веб-приложения Tomcat
ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom"

# Монтирование каталога gameLibrary в контейнере
VOLUME /gameLibrary

# Монтирование каталога torrentDir в контейнере
VOLUME /torrentDir

# Запуск Tomcat
CMD ["catalina.sh", "run"]