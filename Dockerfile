# [1단계: 빌드 환경] Gradle을 이용해 소스 코드를 빌드합니다.
FROM gradle:jdk17-alpine AS builder
WORKDIR /app
COPY . .
# gradlew 실행 권한 부여 및 빌드 (테스트 제외하여 속도 향상)
RUN chmod +x ./gradlew
RUN ./gradlew bootJar -x test

# [2단계: 실행 환경] 빌드된 결과물(Jar)만 가져와서 가볍게 실행합니다.
FROM openjdk:17-jdk-slim
WORKDIR /app
# 빌드 단계에서 생성된 jar 파일을 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]