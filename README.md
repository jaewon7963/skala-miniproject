# BizXray

사업계획서(문서)를 업로드하면 파싱·분석 결과를 바탕으로 검토 의견을 작성하고 공유할 수 있는 플랫폼입니다.

## 기술 스택

| 영역 | 스택 |
|---|---|
| Frontend | Vue 3 (script setup), Vite, Vue Router, Pinia |
| Backend | Spring Boot 3.5 (Java 17), Spring Security + OAuth2 Resource Server, JPA, Flyway |
| Database | PostgreSQL 16 |
| 문서 처리 | Apache PDFBox (PDF 파싱) |
| 인프라(로컬) | Docker Compose (PostgreSQL) |

## 저장소 구조

```
.
├─ FE/business/     프론트엔드 (Vue 3 + Vite)
├─ BE/business/     백엔드 (Spring Boot)
├─ DB/init/         PostgreSQL 초기화 스크립트 (docker-compose 연동)
├─ docs/            API 명세 (api-spec.md)
├─ DESIGN.md        디자인 시스템 / 컬러·컴포넌트 명세
├─ FRONTEND_TECH.md 프론트엔드 구현 기능 · 기술 상세
└─ docker-compose.yml  로컬 PostgreSQL 구동
```

## 빠른 시작

### 1) 프론트엔드만 확인하고 싶을 때

백엔드 없이 목업 데이터로 전체 화면을 확인할 수 있습니다.

```bash
cd FE/business
cp .env.example .env   # 기본값 VITE_USE_MOCK=true
npm install
npm run dev             # http://localhost:5173
```

자세한 내용은 [FE/business/README.md](FE/business/README.md) 참고.

### 2) 전체 스택(FE + BE + DB) 구동

```bash
# 1. DB 실행
docker compose up -d

# 2. 백엔드 실행 (BE/business)
cd BE/business
DB_PASSWORD=business JWT_SECRET=<32바이트 이상 임의 문자열> ./gradlew bootRun
# 서버: http://localhost:8081

# 3. 프론트엔드 실행 (FE/business)
cd FE/business
cp .env.example .env
# .env에서 VITE_USE_MOCK=false 로 변경 (실제 API 연동)
npm install
npm run dev
```

백엔드 필수 환경 변수:

| 변수 | 기본값 | 설명 |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/business` | PostgreSQL 접속 URL |
| `DB_USERNAME` | `business` | DB 사용자 |
| `DB_PASSWORD` | (필수, 기본값 없음) | DB 비밀번호 — `docker-compose.yml` 기본값과 맞추려면 `business` |
| `JWT_SECRET` | (필수, 기본값 없음) | JWT 서명 키 |
| `DOCUMENT_STORAGE_DIR` | `./data/documents` | 업로드 문서 저장 경로 |

## 문서

- [API 명세](docs/api-spec.md)
- [디자인 명세](DESIGN.md)
- [프론트엔드 기능/기술 상세](FRONTEND_TECH.md)
- [프론트엔드 실행 가이드](FE/business/README.md)
