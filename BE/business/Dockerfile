# syntax=docker/dockerfile:1

# ── 빌드 ──────────────────────────────────────────────────────────────────────
# 알파인 대신 우분투 기반 이미지를 쓴다. 시연 노트북이 애플 실리콘일 수도, 인텔일 수도,
# 리눅스일 수도 있는데 Temurin 알파인 태그는 아키텍처 커버리지가 들쭉날쭉하다.
# 여기서는 이미지 크기보다 "어느 컴퓨터에서나 똑같이 뜬다"가 우선이다.
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# 의존성만 먼저 받아 두면 소스만 고쳤을 때 이 레이어를 그대로 재사용한다.
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon --quiet dependencies > /dev/null 2>&1 || true

COPY src ./src
# 테스트는 Testcontainers를 쓴다. 빌드 컨테이너 안에는 도커 데몬이 없으므로 반드시 제외한다.
RUN ./gradlew --no-daemon clean bootJar -x test

# ── 실행 ──────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre
WORKDIR /app

# compose 헬스체크가 /api/health 를 부를 때 쓴다.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/build/libs/*.jar app.jar

ENV TZ=Asia/Seoul \
    JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
