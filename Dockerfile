# 构建阶段
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q package -DskipTests

# 运行阶段
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /build/target/community-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
# 凭据一律通过环境变量注入（MYSQL_PASSWORD / REDIS_PASSWORD / JWT_KEY / S3_ACCESS_KEY / S3_SECRET_KEY）
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]