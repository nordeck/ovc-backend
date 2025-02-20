FROM bellsoft/liberica-openjdk-alpine-musl:17 as builder
WORKDIR /app

#resolve deps
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline

#build
COPY ./src ./src
ARG VERSION=1.0-SNAPSHOT
RUN ./mvnw versions:set -DnewVersion=${VERSION}
RUN ./mvnw -f /app/pom.xml clean package -DskipTests=true

# ----------------------------------------------------------------------------------------------------------------------
FROM bellsoft/liberica-openjdk-alpine-musl:17
WORKDIR /app
COPY --from=builder /app/target/*.jar /app/backend.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/backend.jar" ]
