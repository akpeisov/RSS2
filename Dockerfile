#FROM openjdk:17-jdk-slim
FROM openjdk:17-jdk-alpine

#ENV TZ="Asia/Almaty"
ENV TZ="Asia/Tashkent"

WORKDIR /app

COPY target/*.jar /app/

ENTRYPOINT ["java", "-jar", "rss.jar"]

# docker build -t akpeisov/rss:prod .
# docker push akpeisov/rss:prod 