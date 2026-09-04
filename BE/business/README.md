# BizXray 백엔드

사업계획서 PDF를 파싱해 검토 항목을 뽑고, 사람이 판정·완료까지 진행하는 서비스의 API 서버다.
프런트엔드(`FE/business`)가 부르는 경로와 응답 모양을 그대로 맞춰 두었다.

- Java 17 · Spring Boot 3.5 · PostgreSQL 16 · Flyway
- 포트 `8081`, 모든 경로는 `/api` 아래

## 실행

가장 빠른 길은 컨테이너로 통째로 띄우는 것이다. PostgreSQL도 함께 뜨고, 데모 계정
(`kim@company.com` / `logic1234`)과 AI 검토가 끝난 문서 1건까지 심긴다.

```bash
docker compose up -d --build backend   # 저장소 루트에서
docker compose logs -f backend
```

아래는 코드를 고쳐 가며 재시작할 때 쓰는 로컬 실행이다. DB부터 띄운다.

```bash
# 저장소 루트에서 — PostgreSQL을 호스트 15432로 열어 준다
docker compose up -d postgres

cd BE/business && ./gradlew bootRun    # http://localhost:8081
```

**환경변수를 넘길 필요가 없다.** `bootRun` 은 `dev` 프로파일을 자동으로 켜고
(`build.gradle` 의 `tasks.named('bootRun')`), `src/main/resources/application-dev.properties` 가
DB 접속 정보·JWT 키·데모 시드 기본값을 채운다. 그래서 컨테이너로 띄웠을 때와 **똑같이**
데모 계정(`kim@company.com` / `logic1234`)과 검토가 끝난 문서 1건이 들어 있다.

PostgreSQL을 5432가 아니라 15432로 여는 이유는, 개발자 노트북에 로컬 PostgreSQL이 이미
5432에 떠 있는 경우가 흔해서다. 15432마저 겹치면 포트를 바꾼다.

```bash
POSTGRES_PORT=5433 docker compose up -d postgres
cd BE/business && DB_URL=jdbc:postgresql://localhost:5433/business ./gradlew bootRun
```

이미 로컬에 PostgreSQL이 있다면 도커 대신 그 서버에 롤과 DB만 만들어도 된다.

```sql
CREATE ROLE business LOGIN PASSWORD 'business';
CREATE DATABASE business OWNER business;
```

```bash
cd BE/business && DB_URL=jdbc:postgresql://localhost:5432/business ./gradlew bootRun
```

### 비밀번호와 서명 키

`application.properties` 의 `spring.datasource.password` 와 `jwt.secret` 은 **기본값을 두지 않는다.**
컨테이너나 배포에서 값을 깜빡했을 때 시연용 시크릿으로 조용히 뜨는 것을 막기 위해서다.
로컬 `bootRun` 에서만 `application-dev.properties` 가 그 자리를 채운다.

덮어쓰는 방법은 셋이다.

**① 환경변수** — 가장 간단하다. CI·배포도 이 방식이다.

```bash
DB_PASSWORD=... JWT_SECRET='32바이트 이상 문자열' ./gradlew bootRun
```

**② 개인 설정 파일** — 내 값만 계속 쓰고 싶을 때.

`src/main/resources/application-local.properties` (`.gitignore` 되어 커밋되지 않는다)

```properties
spring.datasource.password=business
jwt.secret=bizxray-local-development-secret-key-32b
```

```bash
# dev 위에 local 을 얹는다. local 만 켜면 dev 기본값이 빠져 기동에 실패한다.
SPRING_PROFILES_ACTIVE=dev,local ./gradlew bootRun
```

**③ 커맨드라인 인자** — 일회성.

```bash
./gradlew bootRun --args='--spring.profiles.active=dev,local'
```

그 밖에 덮어쓸 수 있는 값: `DB_URL` · `DB_USERNAME` · `DOCUMENT_STORAGE_DIR` ·
`CORS_ALLOWED_ORIGINS` · `DEMO_SEED_ENABLED`.
`jwt.secret` 은 32바이트 이상이어야 하며, 짧으면 기동 시 거부한다.

데모 데이터 없이 빈 DB로 시작하고 싶으면 `DEMO_SEED_ENABLED=false ./gradlew bootRun`.

## 프런트엔드와 함께 띄우기

프런트는 기본값이 이미 실제 API라 별도 설정이 없다.

```bash
cd FE/business && npm install && npm run dev   # http://localhost:5173
```

백엔드 없이 화면만 볼 때는 `npm run dev:mock` 으로 내부 목업을 쓴다.

개발 서버가 `/api` 를 8081로 넘겨주므로 CORS 설정 없이 그대로 붙는다.

## 화면이 부르는 경로

| 분류 | 경로 |
|---|---|
| 인증 | `POST /api/auth/signup` · `POST /api/auth/login` · `POST /api/auth/logout` · `GET /api/auth/me` · `PATCH /api/auth/me/password` · `DELETE /api/auth/me` |
| 문서 | `GET/POST /api/documents` · `GET/DELETE /api/documents/{id}` · `PATCH /api/documents/{id}/name` |
| 분석 | `POST /api/review-jobs` · `GET /api/review-jobs/{jobId}` · `GET /api/documents/{documentId}/review-jobs/latest` |
| 검토 | `.../sections` · `.../pages` · `.../findings` · `PATCH .../findings/{id}/verdict` · `POST .../complete` · `GET .../report` · `POST .../questions` |

성공 응답은 리소스를 래퍼 없이 그대로 돌려준다. 실패는 `{ code, message, details }` 이고
`message` 는 화면에 그대로 노출되므로 한국어로 쓴다.

## 분석 파이프라인

업로드하면 곧바로 파싱이 시작되고, 분석을 요청하면 백그라운드에서 이어 돈다.
요청은 기다리지 않고 즉시 돌아오며, 화면은 작업 조회를 반복해 진행률을 그린다.

```
PDF 업로드 → 페이지 텍스트 추출 → 제목·문단·표 블록으로 분해 → 목차 복원
          → 규칙 기반 검토 항목 생성 → 근거를 원문 블록에 연결
```

검토 항목의 근거는 항상 원문에 실제로 있는 블록을 가리킨다. 근거를 못 붙이는 항목은
아예 만들지 않는다. 어디를 보라는 것인지 알려주지 못하는 지적은 화면에서 쓸모가 없기 때문이다.

`review.parse-duration-ms` · `review.analyze-duration-ms` 로 각 구간의 최소 소요 시간을 조절한다.
진행 화면이 단계를 따라갈 수 있도록 둔 값이다.

## 검토 항목을 찾는 방식

`review.ai.provider=stub`(기본값)일 때는 모델 없이 규칙으로 찾는다.

- 표의 합계 행을 다시 계산해 항목 합과 어긋나면 오류로 올린다
- 같은 대상을 세는 숫자가 페이지마다 다르면 전제 불일치로 올린다
- 수치를 단정했는데 출처가 없거나, 목표만 있고 측정 방법이 빈 지표를 확인 대상으로 올린다

같은 문서를 다시 분석하면 항상 같은 결과가 나온다. 확신도까지 블록 식별자에서 파생시켰다.

실제 모델을 붙일 때는 `com.logiccheck.ai.ReviewAiClient` 를 구현하는 클래스를 하나 더 만들고
`review.ai.provider` 값만 바꾸면 된다. 저장하는 쪽은 인터페이스만 알고 있다.

## 테스트

```bash
./gradlew test
```

DB를 쓰는 테스트는 Testcontainers로 PostgreSQL을 띄우므로 도커가 실행 중이어야 한다.
테스트는 자체 설정으로 돌기 때문에 위의 비밀번호·서명 키 설정이 없어도 된다.
`ReviewJobFlowIntegrationTest` 가 업로드부터 검토 완료·의견서까지를 화면이 부르는 순서 그대로 태운다.
