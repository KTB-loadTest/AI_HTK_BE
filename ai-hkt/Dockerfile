FROM gradle:8.6-jdk21 AS builder
WORKDIR /workspace/app

COPY . .

RUN gradle clean build -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /workspace/app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
