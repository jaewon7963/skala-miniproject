# BizXray

사업계획서(문서)를 업로드하면 파싱·분석 결과를 바탕으로 검토 의견을 작성하고 공유할 수 있는 플랫폼입니다.

## 기술 스택

| 영역 | 스택 |
|---|---|
| Frontend | Vue 3 (script setup), Vite, Vue Router, Pinia |
| Backend | Spring Boot 3.5 (Java 17), Spring Security + OAuth2 Resource Server, JPA, Flyway |
| Database | PostgreSQL 16 |
| 문서 처리 | Apache PDFBox (PDF 파싱) |
| 인프라(로컬) | Docker Compose (FE nginx + BE + PostgreSQL) |

## 저장소 구조

```
.
├─ FE/business/     프론트엔드 (Vue 3 + Vite) — Dockerfile · nginx.conf 포함
├─ BE/business/     백엔드 (Spring Boot) — Dockerfile 포함
├─ DB/init/         PostgreSQL 초기화 스크립트 (docker-compose 연동)
├─ docs/            API 명세 (api-spec.md), samples/ 시연용 PDF
├─ DESIGN.md        디자인 시스템 / 컬러·컴포넌트 명세
├─ FRONTEND_TECH.md 프론트엔드 구현 기능 · 기술 상세
└─ docker-compose.yml      전체 스택(FE + BE + DB) 구동 · DB만 따로 띄우기
```

## 시연·평가용 원클릭 실행 (권장)

클론 직후 아래 한 줄이면 프런트·백엔드·DB가 전부 뜨고, **데모 계정과 AI 검토가 끝난 사업계획서 1건까지** 준비된다. `.env` 파일도 필요 없다.

```bash
docker compose up -d --build
```

| | |
|---|---|
| 프런트엔드 | <http://localhost:5173> |
| 백엔드 | <http://localhost:8081/api/health> |
| **데모 계정** | **`kim@company.com` / `logic1234`** |

로그인하면 문서함에 `AI 매장 안내 로봇 사업계획서`(8쪽)가 **검토 중** 상태로 들어 있다. 검토 화면에서 AI가 찾아낸 5건을 확인할 수 있다.

| 검토 항목 | 유형 | 위치 |
|---|---|---|
| 표 합계가 항목 합과 불일치 (7,000 vs 6,300) | 오류 · 결정적 검산 | p.6 |
| 매장 수 전제 충돌 (p.1 120개 ↔ p.7 150개) | 오류 · 결정적 검산 | p.7 |
| 성장률(CAGR 32%)의 산출 근거 없음 | 확인 필요 | p.4 |
| 시장 전망 수치의 출처 없음 | 근거 부족 | p.2 |
| KPI 표의 측정 주기·산식 칸이 비어 있음 | 근거 부족 | p.8 |

`2.1 참고 자료`(p.3)는 같은 종류의 주장을 하면서 출처를 밝힌 문단이라 **일부러 지적이 붙지 않는다.** 규칙이 무차별로 걸지 않는다는 것을 보여주는 대조군이다.

업로드부터 진행률 화면까지 라이브로 보여주려면 `docs/samples/bizxray-demo-plan.pdf`를 **새로 가입한 계정**으로 올리면 된다. (같은 계정에 같은 파일을 다시 올리면 중복으로 막힌다.)

### 자주 쓰는 명령

```bash
docker compose logs -f backend      # 시드 로그 확인
docker compose down                 # 중지 (데이터 유지)
docker compose down -v              # 중지 + 초기화 — 다시 올리면 데모 데이터가 새로 심긴다
docker compose exec postgres psql -U business -d business   # DB 직접 조회
```

> PostgreSQL은 5432가 아니라 **15432**로 열린다. 개발자 노트북에 로컬 PostgreSQL이 이미 5432에 떠 있는 경우가 흔해서, 겹치지 않는 번호를 쓴다.
> 5173 / 8081 / 15432가 이미 쓰이고 있다면 `lsof -nP -iTCP:5173 -iTCP:8081 -iTCP:15432 -sTCP:LISTEN` 으로 확인하거나, `.env.example`을 `.env`로 복사해 `FRONTEND_PORT` / `BACKEND_PORT` / `POSTGRES_PORT`를 바꾼다.
> `JWT_SECRET`은 시연용 고정값이 들어 있다. 실제 배포에서는 반드시 바꾼다.

## 개발용 실행 — 코드를 고쳐 가며 보기

컨테이너 밖에서 BE·FE를 직접 띄운다. **아래 세 줄이 전부다.** `.env`도, 환경변수도, 추가 플래그도 없다.

```bash
# 1) DB만 띄운다 (호스트 15432로 열린다)
docker compose up -d postgres

# 2) 백엔드 — 새 터미널
cd BE/business && ./gradlew bootRun          # http://localhost:8081

# 3) 프런트 — 새 터미널
cd FE/business && npm install && npm run dev  # http://localhost:5173
```

로그인하면 원클릭 실행과 **똑같이** 데모 계정 `kim@company.com` / `logic1234` 와 검토가 끝난 문서 1건이 들어 있다.
`bootRun` 이 `dev` 프로파일을 자동으로 켜서 DB 비밀번호·JWT 키·데모 시드를 채워 주기 때문이다
(`BE/business/src/main/resources/application-dev.properties`).

> **전체 스택 컨테이너와 동시에는 못 띄운다.** `docker compose up -d --build` 로 띄운 프런트·백엔드가
> 이미 5173·8081을 쓰고 있어서 충돌한다. 개발용으로 넘어갈 때는 `docker compose down` 후 위 세 줄을 쓴다.
>
> 컨테이너 백엔드와 호스트 `bootRun` 을 오갈 때는 **`docker compose down -v` 로 한 번 비우는 것이 안전하다.**
> 업로드된 PDF가 컨테이너는 `document_data` 볼륨에, 호스트는 `BE/business/data/documents` 에 따로 쌓여서
> DB에는 문서 행이 있는데 파일이 없는 상태가 될 수 있다.

### 백엔드 없이 화면만 보기

```bash
cd FE/business && npm install && npm run dev:mock
```

`src/api/mock/` 픽스처로 전 화면이 돈다. 로그인은 아무 이메일 + 8자 이상 비밀번호면 통과한다.

### 포트가 겹칠 때

| 겹치는 포트 | 해결 |
|---|---|
| 15432 (DB) | `POSTGRES_PORT=5433 docker compose up -d postgres` 후 백엔드에 `DB_URL=jdbc:postgresql://localhost:5433/business` 를 넘긴다 |
| 8081 (BE) | 컨테이너 백엔드가 떠 있는지 확인 — `docker compose down` |
| 5173 (FE) | 컨테이너 프런트가 떠 있는지 확인 — `docker compose down` |

`lsof -nP -iTCP:5173 -iTCP:8081 -iTCP:15432 -sTCP:LISTEN` 으로 누가 쓰는지 볼 수 있다.

### 테스트

```bash
# 전체 스택이 떠 있는 상태에서 — API 기능 63건 점검 (업로드→분석→판정→리포트 전체 흐름)
docker compose up -d --build
./scripts/smoke-test.sh

cd BE/business
./gradlew test                                   # 전체 (Testcontainers — 도커 데몬 필요)
./gradlew test --tests '*DemoPdfFindingsTest'    # 시연 PDF 검증만 (도커 불필요)
```

`smoke-test.sh` 는 데모 계정을 읽기만 하고 쓰기 흐름은 매번 새 임시 계정으로 돌린 뒤 탈퇴시키므로,
몇 번을 돌려도 시연 상태가 망가지지 않는다.

### 사전 준비물

| | 버전 | 언제 필요한가 |
|---|---|---|
| Docker Desktop | Compose v2 이상 | 원클릭 실행, DB 구동 |
| JDK | 17 이상 | 백엔드 직접 실행, 테스트 |
| Node.js | `^20.19` 또는 `>=22.12` | 프런트 직접 실행 |

### 백엔드 환경 변수

`./gradlew bootRun` 은 `dev` 프로파일이 기본값을 채우므로 **아무것도 넘기지 않아도 된다.**
아래는 그 기본값을 덮어쓰고 싶을 때만 쓴다. 컨테이너(`java -jar`)에는 `dev` 프로파일이 적용되지 않아
`DB_PASSWORD` · `JWT_SECRET` 이 없으면 뜨지 않는다 — compose 가 넣어 준다.

| 변수 | `bootRun` 기본값 | 설명 |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:15432/business` | PostgreSQL 접속 URL |
| `DB_USERNAME` | `business` | DB 사용자 |
| `DB_PASSWORD` | `business` | DB 비밀번호 (컨테이너에서는 필수) |
| `JWT_SECRET` | 시연용 고정값 | JWT 서명 키 — **32바이트 이상** (컨테이너에서는 필수) |
| `DOCUMENT_STORAGE_DIR` | `./data/documents` | 업로드 문서 저장 경로 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | 브라우저가 8081을 직접 호출할 때 허용할 오리진 |
| `DEMO_SEED_ENABLED` | `true` | 기동 시 데모 계정·문서를 심을지 |

개인 시크릿으로 덮어쓰려면 `BE/business/src/main/resources/application-local.properties`(`.gitignore` 대상)를
만들고 `SPRING_PROFILES_ACTIVE=dev,local ./gradlew bootRun` 으로 함께 켠다.

### 프런트엔드 환경 변수

기본값만으로 `npm run dev` 가 실제 백엔드에 붙으므로 `.env` 는 필요 없다. 바꾸려면 `cp .env.example .env`.

| 변수 | 기본값 | 설명 |
|---|---|---|
| `VITE_USE_MOCK` | `false` (`dev:mock` 은 `true`) | `true` = 목업 데이터, `false` = 실제 API |
| `VITE_API_BASE_URL` | `/api` | API 기본 경로 |
| `VITE_API_PROXY_TARGET` | `http://localhost:8081` | dev 서버가 `/api` 를 넘길 대상 |

`npm run build` 로 만든 정적 파일에는 `VITE_*` 값이 **박혀서** 나온다. 런타임 환경변수로는 못 바꾼다.

## 문서

- [API 명세](docs/api-spec.md)
- [디자인 명세](DESIGN.md)
- [프론트엔드 기능/기술 상세](FRONTEND_TECH.md)
- [프론트엔드 실행 가이드](FE/business/README.md)

## 시연용 PDF 다시 만들기

본문은 `BE/business/src/test/java/com/logiccheck/demo/DemoPdfContent.java`에 있다. 고친 뒤:

```bash
cd BE/business
./gradlew generateDemoPdf                        # PDF 재생성 (한글 폰트를 한 번 내려받는다)
./gradlew test --tests '*DemoPdfFindingsTest'    # 의도한 검토 항목이 그대로 나오는지 확인
```

문장 하나하나가 파서(`PageBlockExtractor`)와 검토 규칙(`StubReviewAiClient`)을 겨냥해 쓰여 있다. `DemoPdfWriter`가 표 행·본문 줄이 의도한 종류로 읽히는지 생성 시점에 검사하고, 위 테스트가 검토 결과 5건을 고정한다.
