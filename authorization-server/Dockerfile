FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

COPY authorization-server/pom.xml authorization-server/
COPY authorization-server/src authorization-server/src
RUN apk add --no-cache maven && \
    mvn -f authorization-server/pom.xml dependency:go-offline -B -q && \
    mvn -f authorization-server/pom.xml package -DskipTests -B -q

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache wget && adduser -D -s /bin/sh app
WORKDIR /app
COPY --from=builder /build/authorization-server/target/*.jar app.jar
RUN chown -R app:app /app
USER app
EXPOSE 9000
ENTRYPOINT ["java", "-jar", "app.jar"]
