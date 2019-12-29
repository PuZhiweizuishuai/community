FROM maven:latest AS maven
COPY . /app/
WORKDIR /app/
RUN mvn -DskipTests=true package

FROM openjdk:8
COPY --from=maven /app/target/community-0.0.1-SNAPSHOT.jar /app/
WORKDIR /app/
EXPOSE 8080
ENTRYPOINT [ "java", "-jar", "community-0.0.1-SNAPSHOT.jar" ]
