# [1단계: 빌드 환경] Gradle을 이용해 소스 코드를 빌드합니다.
FROM gradle:jdk17-alpine AS builder
WORKDIR /app
COPY . .
# gradlew 실행 권한 부여 및 빌드 (테스트 제외하여 속도 향상)
RUN chmod +x ./gradlew
RUN ./gradlew bootJar -x test

# [2단계: 실행 환경] 더 안정적인 eclipse-temurin 이미지 사용
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
# 빌드 단계에서 생성된 jar 파일을 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]