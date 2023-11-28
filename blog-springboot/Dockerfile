FROM maven:3.3.9-jdk-8-alpine AS test
USER root
COPY ./ /tmp/code
RUN cd /tmp/code/blog-feng-yu && mvn clean install -Dmaven.test.skip=true -Dmaven.javadoc.skip=true

FROM openjdk:8u181-stretch
ADD target/*.jar /application.jar
ENTRYPOINT ["java", "-jar","/application.jar"]
