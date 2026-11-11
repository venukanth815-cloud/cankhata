# ---------- Stage 1: build the WAR with Maven (free, runs inside Docker) ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---------- Stage 2: run it on Tomcat 10.1 ----------
FROM tomcat:10.1-jdk17-temurin
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /build/target/cankhata.war /usr/local/tomcat/webapps/ROOT.war

ENV LOG_DIR=/usr/local/tomcat/logs
# Memory limits, a heap dump if memory runs out, and a GC log you can read later.
ENV CATALINA_OPTS="-Duser.timezone=Asia/Kolkata -Xms256m -Xmx512m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/usr/local/tomcat/logs -Xlog:gc*:file=/usr/local/tomcat/logs/gc.log:time,uptime:filecount=5,filesize=5m"
EXPOSE 8080
