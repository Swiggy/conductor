FROM gradle:4.8.1 AS build
COPY --chown=gradle:gradle . /home/gradle/conductor
WORKDIR /home/gradle/conductor
RUN gradle build -x test

FROM openjdk:8-jre-alpine
WORKDIR /opt
COPY --from=build /home/gradle/conductor/server/build/libs/conductor-server-*.*.*-SNAPSHOT-all.jar .
ENTRYPOINT [ "sh", "-c",  "java -jar conductor-server-*.*.*-SNAPSHOT-all.jar"]

