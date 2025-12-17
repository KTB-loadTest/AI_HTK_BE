# ---------- build stage ----------
FROM gradle:8.6-jdk21 AS builder
WORKDIR /workspace/app

# Gradle wrapper 관련 파일 먼저 복사 (캐시 최적화)
COPY gradle gradle
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew
RUN ./gradlew dependencies

# 소스 복사
COPY src src

# 실행 가능한 Spring Boot JAR 생성
RUN ./gradlew clean bootJar -x test

# ---------- runtime stage ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /workspace/app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
