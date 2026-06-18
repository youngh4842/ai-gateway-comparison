FROM gradle:8.10.2-jdk17 AS builder
WORKDIR /workspace
COPY settings.gradle build.gradle ./
COPY app-cs-service ./app-cs-service
COPY app-mock-llm ./app-mock-llm
RUN gradle :app-mock-llm:bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /workspace/app-mock-llm/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
