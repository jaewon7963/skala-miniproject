# BizXray 백엔드

사업계획서 PDF를 파싱해 검토 항목을 뽑고, 사람이 판정·완료까지 진행하는 서비스의 API 서버다.
프런트엔드(`FE/business`)가 부르는 경로와 응답 모양을 그대로 맞춰 두었다.

- Java 17 · Spring Boot 3.5 · PostgreSQL 16 · Flyway
- 포트 `8081`, 모든 경로는 `/api` 아래

## 실행

DB부터 띄운다.

```bash
docker compose up -d postgres          # 저장소 루트에서
```

이미 로컬에 PostgreSQL이 5432를 쓰고 있다면 도커 대신 그 서버에 롤과 DB만 만들어도 된다.

```sql
CREATE ROLE business LOGIN PASSWORD 'business';
CREATE DATABASE business OWNER business;
```

### 비밀번호와 서명 키

`spring.datasource.password` 와 `jwt.secret` 은 **기본값을 두지 않는다.** 설정 파일에 값을
적어 두면 저장소에 그대로 남기 때문이다. 둘 중 편한 쪽으로 넣는다.

**① 로컬 설정 파일** — 한 번 만들어 두면 그 뒤로는 신경 쓸 게 없다.

`src/main/resources/application-local.properties` (`.gitignore` 되어 커밋되지 않는다)

```properties
spring.datasource.password=business
jwt.secret=bizxray-local-development-secret-key-32b
```

```bash
cd BE/business && ./gradlew bootRun --args='--spring.profiles.active=local'
```

**② 환경변수** — CI나 배포에서 쓴다.

```bash
DB_PASSWORD=business JWT_SECRET='32바이트 이상 문자열' ./gradlew bootRun
```

둘 다 없으면 기동 단계에서 placeholder 를 못 채워 실패한다.

그 밖에 덮어쓸 수 있는 값: `DB_URL` · `DB_USERNAME` · `DOCUMENT_STORAGE_DIR`.
`jwt.secret` 은 32바이트 이상이어야 하며, 짧으면 기동 시 거부한다.

## 프런트엔드와 함께 띄우기

`FE/business/.env` 를 만들면 화면이 내부 목업 대신 이 서버를 쓴다. 없으면 목업 모드로 돈다.

```
VITE_USE_MOCK=false
VITE_API_BASE_URL=/api
VITE_API_PROXY_TARGET=http://localhost:8081
```

```bash
cd FE/business && npm install && npm run dev   # http://localhost:5173
```

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
