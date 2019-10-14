FROM gradle:4.8.1 AS build
COPY --chown=gradle:gradle . /home/gradle/conductor
WORKDIR /home/gradle/conductor
RUN gradle build -x test

FROM openjdk:8-jre-alpine
WORKDIR /opt
COPY --from=build /home/gradle/conductor/server/build/libs/conductor-server-*.*.*-SNAPSHOT-all.jar .
COPY --from=build /home/gradle/conductor/newrelic.yml .

RUN apk add wget && apk add unzip && wget --no-check-certificate https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip
RUN unzip newrelic-java.zip     
RUN rm newrelic-java.zip
COPY newrelic.yml newrelic/

ENTRYPOINT [ "sh", "-c",  "java -javaagent:newrelic/newrelic.jar -jar conductor-server-*.*.*-SNAPSHOT-all.jar"]

