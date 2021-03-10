FROM openjdk:8-jdk-alpine
MAINTAINER czy <1183728554@qq.com>

COPY /czystudy/czystudyback/czystudy.jar /app.jar
EXPOSE 9000
ENTRYPOINT ["java","-jar -Dspring.profiles.active=prod","app.jar"]