# BizXray API 명세서 (Light v0.1)

> 2026-09-03 회의 결정사항 + 현재 FE 구현(`FE/business/src/api`)을 기준으로 작성한 **Light 버전**입니다.
> 프론트가 먼저 진도를 낼 수 있도록 경로 · 요청 · 응답 · 상태코드만 확정하고,
> 상세 검증 규칙과 에러 세분화는 Max 버전에서 채웁니다.
>
> 이 문서가 확정되면 FE는 `src/api/endpoints.js` 한 파일만 수정하면 연결됩니다.

---

## 0. 공통 규약

### 0-1. 기본

| 항목 | 값 |
|---|---|
| Base URL | `/api` (로컬: `http://localhost:8081/api`) |
| 포맷 | `application/json; charset=utf-8` (업로드만 `multipart/form-data`) |
| 인증 | `Authorization: Bearer <accessToken>` |
| 날짜 | ISO-8601 문자열 (`2026-09-02T14:20:00+09:00`) |
| ID | 문자열(UUID 권장). FE는 ID를 문자열로만 취급합니다 |
| 시간대 | 서버 저장은 UTC, 응답은 `+09:00` 오프셋 포함 |

### 0-2. 상태 코드

| 코드 | 사용 상황 |
|---|---|
| `200 OK` | 조회 · 수정 성공 |
| `201 Created` | 리소스 생성 (회원가입, 문서 업로드, 주석 생성) |
| `202 Accepted` | **비동기 작업 접수** (분석 시작) |
| `204 No Content` | 삭제 성공 |
| `400 Bad Request` | 파라미터 형식 오류 |
| `401 Unauthorized` | 토큰 없음 · 만료 · 로그인 실패 |
| `403 Forbidden` | 타인 소유 리소스 접근 |
| `404 Not Found` | 리소스 없음 |
| `409 Conflict` | 이메일 중복, 이미 완료된 작업 재완료 |
| `413 Payload Too Large` | 파일 용량 초과 |
| `415 Unsupported Media Type` | PDF 아님 |
| `422 Unprocessable Entity` | 값 검증 실패 (비밀번호 규칙 등) |
| `500 Internal Server Error` | 서버 오류 |

### 0-3. 에러 응답 (전 엔드포인트 공통)

```json
{
  "code": "EMAIL_ALREADY_EXISTS",
  "message": "이미 가입된 이메일입니다",
  "details": { "field": "email" }
}
```

- `message`는 **사용자에게 그대로 노출**됩니다. 한국어로 작성해주세요.
- FE는 `http.js`에서 `ApiError { status, code, message, details }`로 정규화해 처리합니다.

### 0-4. 목록 응답 (페이지네이션)

```json
{
  "items": [],
  "total": 42,
  "page": 1,
  "size": 20
}
```

`page`는 1부터 시작합니다. 기본 `size = 20`.

### 0-5. Enum (FE `src/constants/enums.js`와 1:1)

| Enum | 값 |
|---|---|
| `DocumentStatus` | `IDLE` 미분석 · `PARSING` · `ANALYZING` · `REVIEWING` 검토 중 · `DONE` 검토 완료 · `FAILED` 파싱 실패 |
| `JobStatus` | `PENDING` · `RUNNING` · `PARTIAL` 부분 실패 · `DONE` · `FAILED` |
| `FindingType` | `ERROR` 오류 · `NEEDS_CHECK` 확인 필요 · `NO_EVIDENCE` 근거 부족 |
| `FindingMethod` | `DETERMINISTIC` 결정적 검산 · `RAG` 관계 판단 |
| `Verdict` | `PENDING` 미판정 · `ACCEPTED` 검토 반영 · `REJECTED` 오류 아님 |
| `AnnotationSource` | `VIEWER` 원문에서 직접 · `FINDING` 검토 항목에서 |

> Enum 값이 바뀌면 FE `constants/enums.js`도 같이 바꿔야 합니다. **문자열 값을 그대로 씁니다(숫자 코드 X).**

---

## 1. API 목록

| # | Method | Path | 설명 | 인증 | 단계 |
|---|---|---|---|---|---|
| 1 | POST | `/auth/signup` | 회원가입 | – | MVP1 |
| 2 | POST | `/auth/login` | 로그인 | – | MVP1 |
| 3 | POST | `/auth/logout` | 로그아웃 | ✔ | MVP1 |
| 4 | GET | `/auth/me` | 내 정보 | ✔ | MVP1 |
| 5 | PATCH | `/auth/me/password` | 비밀번호 변경 | ✔ | MVP2 |
| 6 | DELETE | `/auth/me` | 회원 탈퇴 | ✔ | MVP2 |
| 7 | GET | `/documents` | 문서 목록 (검색·필터·정렬·페이지) | ✔ | MVP1 |
| 8 | GET | `/documents/{documentId}` | 문서 단건 | ✔ | MVP1 |
| 9 | POST | `/documents` | PDF 업로드 | ✔ | MVP1 |
| 10 | PATCH | `/documents/{documentId}/name` | 문서명 변경 | ✔ | MVP1 |
| 11 | DELETE | `/documents/{documentId}` | 문서 삭제 | ✔ | MVP1 |
| 12 | POST | `/review-jobs` | **분석 시작 (비동기)** | ✔ | MVP1 |
| 13 | GET | `/review-jobs/{jobId}` | 분석 작업 상태 조회 (폴링) | ✔ | MVP1 |
| 14 | GET | `/documents/{documentId}/review-jobs/latest` | 문서의 최근 분석 작업 | ✔ | MVP1 |
| 15 | GET | `/review-jobs/{jobId}/sections` | 목차 | ✔ | MVP1 |
| 16 | GET | `/review-jobs/{jobId}/pages` | 원문 페이지(파싱 결과) | ✔ | MVP1 |
| 17 | GET | `/review-jobs/{jobId}/findings` | 검토 항목 목록 | ✔ | MVP1 |
| 18 | PATCH | `/review-jobs/{jobId}/findings/{findingId}/verdict` | 판정 | ✔ | MVP1 |
| 19 | GET | `/review-jobs/{jobId}/annotations` | 주석 목록 | ✔ | MVP1 |
| 20 | POST | `/review-jobs/{jobId}/annotations` | 주석 생성 | ✔ | MVP1 |
| 21 | PATCH | `/annotations/{annotationId}` | 주석 수정 | ✔ | MVP1 |
| 22 | DELETE | `/annotations/{annotationId}` | 주석 삭제 | ✔ | MVP1 |
| 23 | POST | `/review-jobs/{jobId}/complete` | 검토 완료 확정 | ✔ | MVP1 |
| 24 | GET | `/review-jobs/{jobId}/export` | 요약 내보내기 (주석·하이라이트 반영 PDF) | ✔ | MVP1 |
| 25 | POST | `/review-jobs/{jobId}/questions` | AI 질문 | ✔ | 미결 |
| 26 | GET | `/review-jobs/{jobId}/report` | 검토 의견서 데이터 | ✔ | MVP2 |

**회의 결정으로 제외된 것**: `GET /folders`(폴더 기능 제외), OAuth 로그인, 이름(`name`) 필드.

---

## 2. 인증 (AUTH)

### 2-1. `POST /auth/signup` — 회원가입

회의 결정: **이름 없음. 이메일 + 비밀번호만.** 이메일이 곧 아이디입니다.

**Request**
```json
{
  "email": "kim@company.com",
  "password": "logic1234",
  "agreeTerms": true,
  "agreePrivacy": true
}
```

| 필드 | 타입 | 필수 | 제약 |
|---|---|---|---|
| `email` | string | ✔ | 이메일 형식, 최대 255, **unique** |
| `password` | string | ✔ | 8자 이상, 영문 + 숫자 각 1자 이상 |
| `agreeTerms` / `agreePrivacy` | boolean | ✔ | `true` 필수 |

**Response `201`**
```json
{
  "accessToken": "eyJhbGci...",
  "user": { "id": "u-1", "email": "kim@company.com", "createdAt": "2026-09-03T09:00:00+09:00" }
}
```

| 코드 | 상황 |
|---|---|
| `409` | `EMAIL_ALREADY_EXISTS` — 이미 가입된 이메일 |
| `422` | `INVALID_PASSWORD` — 비밀번호 규칙 위반 |

### 2-2. `POST /auth/login` — 로그인

**Request**
```json
{ "email": "kim@company.com", "password": "logic1234" }
```

**Response `200`** — signup과 동일한 형태

| 코드 | 상황 |
|---|---|
| `401` | `INVALID_CREDENTIALS` — 메시지는 이메일/비밀번호 구분 없이 `"이메일 또는 비밀번호를 확인해주세요"` |

> `keepSignedIn`(로그인 상태 유지)은 회의에서 축소 결정 → **MVP1 제외**. FE는 UI만 유지하고 값을 보내지 않습니다.
> 토큰 만료는 우선 24h 고정, refresh token 없음.

### 2-3. `POST /auth/logout` — `204`
서버가 상태를 안 가지면 FE 토큰 폐기만으로 충분합니다. 엔드포인트는 유지하되 no-op 허용.

### 2-4. `GET /auth/me` — `200`
```json
{ "id": "u-1", "email": "kim@company.com", "createdAt": "2026-09-03T09:00:00+09:00" }
```

### 2-5. `PATCH /auth/me/password` (MVP2)
```json
{ "currentPassword": "logic1234", "newPassword": "logic5678" }
```
`200 { "ok": true }` · `401 INVALID_CREDENTIALS` · `422 INVALID_PASSWORD`

### 2-6. `DELETE /auth/me` (MVP2)
```json
{ "password": "logic1234" }
```
`200 { "ok": true, "purgeAfterDays": 30 }` — 문서 · 분석 작업 · 검토 항목 · 주석 연쇄 삭제

---

## 3. 문서 (DOCUMENT)

### 3-1. Document 모델

```json
{
  "id": "doc-1",
  "name": "AI 매장 안내 로봇 사업계획서",
  "version": 3,
  "pageCount": 21,
  "sizeBytes": 4404019,
  "status": "REVIEWING",
  "latestJobId": "job-1",
  "updatedAt": "2026-09-02T14:20:00+09:00",
  "createdAt": "2026-09-01T10:00:00+09:00"
}
```

> `folderId` 제외(회의 결정). `summary`는 검색 대상에서 빠지므로 응답에서도 제외합니다.

### 3-2. `GET /documents` — 문서 목록

**Query**

| 파라미터 | 타입 | 기본값 | 설명 | MVP1 |
|---|---|---|---|---|
| `q` | string | – | **문서명 부분 일치 검색** | ✔ |
| `period` | `ALL`\|`D7`\|`D30`\|`D90` | `D30` | `updatedAt` 기준 기간 필터 | ✔ |
| `status` | DocumentStatus\|`ALL` | `ALL` | 상태 필터 | △ |
| `sort` | `UPDATED_DESC`\|`NAME_ASC` | `UPDATED_DESC` | 정렬 | △ |
| `page` / `size` | int | `1` / `20` | 페이지네이션 | ✔ |

> △ 회의 결정: **상태 필터와 정렬 중 하나만 실제 구현**합니다(사이드바와 기능 중복).
> UI는 둘 다 노출하므로 **미구현 쪽은 파라미터를 받되 무시**하고 200을 반환해주세요. 400을 주면 FE가 깨집니다.
> 어느 쪽을 구현할지 결정되면 이 표에 ✔로 갱신합니다.

**Response `200`**
```json
{
  "items": [ /* Document[] */ ],
  "total": 4,
  "page": 1,
  "size": 20,
  "counts": { "ALL": 4, "REVIEWING": 1, "DONE": 2, "FAILED": 1 }
}
```

- `counts`는 **필터와 무관한 전체 기준** 값입니다. 좌측 사이드바 배지에 씁니다.
- 성능: `owner_id`, `updated_at`, `status` 복합 인덱스 + `name` 검색 인덱스 필요 (만 건 이상 대비, 발표 포인트).

### 3-3. `GET /documents/{documentId}` — `200` Document · `403` · `404`

### 3-4. `POST /documents` — PDF 업로드

**Request** `multipart/form-data`

| 파트 | 타입 | 설명 |
|---|---|---|
| `file` | binary | PDF 파일 |

검증은 회의 결정대로 **형식 + 용량 두 가지만**:

| 코드 | 상황 |
|---|---|
| `415` | `UNSUPPORTED_FILE_TYPE` — PDF가 아님 |
| `413` | `FILE_TOO_LARGE` — 50MB 초과 |

**Response `201`** — Document (`status: "IDLE"`, `latestJobId: null`)

> 업로드 시점에는 파싱하지 않습니다. `pageCount`는 파싱 후 채워지므로 `null` 허용.
> 중복 해시 검사 · 페이지 수 제한 · 암호화 검사는 제외(회의 결정).

### 3-5. `PATCH /documents/{documentId}/name`
```json
{ "name": "AI 매장 안내 로봇 사업계획서 v3" }
```
`200` Document · `400` 빈 문자열

### 3-6. `DELETE /documents/{documentId}` — `204`
문서 · 분석 작업 · 검토 항목 · 주석 연쇄 삭제. 원본 PDF 파일도 삭제.

---

## 4. 분석 (REVIEW JOB) — 비동기

### 4-1. ReviewJob 모델

```json
{
  "id": "job-1",
  "documentId": "doc-1",
  "status": "RUNNING",
  "phase": "PARSE",
  "parseProgress": 68,
  "analyzeProgress": 0,
  "steps": [
    { "key": "validate", "label": "파일 검증", "state": "DONE" },
    { "key": "split",    "label": "페이지 분할 · 텍스트 레이어 추출", "state": "RUNNING" },
    { "key": "outline",  "label": "목차 · 섹션 인식", "state": "WAIT" },
    { "key": "extract",  "label": "표 · 수치 · 주장 · 기술 · KPI 추출", "state": "WAIT" },
    { "key": "persist",  "label": "구조화 저장", "state": "WAIT" }
  ],
  "partialFailures": [
    { "page": 14, "reason": "이미지형 표는 텍스트 레이어가 없어 해당 구간을 제외하고 진행했습니다" }
  ],
  "summary": {
    "total": 8,
    "byType": { "ERROR": 3, "NEEDS_CHECK": 3, "NO_EVIDENCE": 2 },
    "decided": 0, "accepted": 0, "rejected": 0
  },
  "startedAt": "2026-09-02T14:20:00+09:00",
  "completedAt": null
}
```

- `steps[].state`: `WAIT` · `RUNNING` · `DONE` · `FAILED`
- `phase`: `PARSE` → `ANALYZE`. FE 진행 화면이 이 값으로 문구를 바꿉니다.
- `summary.score`는 **정책 미확정**이라 응답에서 뺍니다. 확정되면 `summary.score: number` 추가.

### 4-2. `POST /review-jobs` — 분석 시작

```json
{ "documentId": "doc-1" }
```

**Response `202 Accepted`** — 생성된 ReviewJob (`status: "PENDING"`)

```
Location: /api/review-jobs/job-1
```

- 파싱(룰 기반) → 요소 추출 → AI 분석까지 **백그라운드 실행**. 요청은 즉시 반환합니다.
- 동일 문서에 진행 중인 작업이 있으면 `409 JOB_ALREADY_RUNNING`.
- 비동기인 이유(발표용): PDF 파싱 + LLM 호출은 수 초~수십 초가 걸려 HTTP 타임아웃·리트라이 사고를 유발하고,
  진행률을 사용자에게 보여줄 수 없습니다.

### 4-3. `GET /review-jobs/{jobId}` — 상태 폴링

**Response `200`** — ReviewJob

- FE는 `status`가 `DONE`/`FAILED`가 될 때까지 **400ms 간격 폴링**합니다 (`stores/review.js:pollJob`).
- 분석 중 발견된 항목을 실시간으로 보여주기 위해 **`discovered: Finding[]`(지금까지 발견분)** 을 함께 내려주면 좋습니다. 어려우면 생략 가능(FE는 빈 배열 처리).
- 폴링 부하가 문제되면 MVP2에서 SSE(`GET /review-jobs/{jobId}/stream`)로 교체합니다.

### 4-4. `GET /documents/{documentId}/review-jobs/latest`
`200` ReviewJob · `404 NO_REVIEW_JOB` (아직 분석하지 않은 문서 → FE가 업로드 화면으로 보냅니다)

---

## 5. 검토 (REVIEW)

### 5-1. `GET /review-jobs/{jobId}/sections` — 목차

```json
[
  { "id": "sec-5", "title": "매출 · 재무 계획", "level": 1, "page": 11, "findingCount": 2 }
]
```

### 5-2. `GET /review-jobs/{jobId}/pages` — 원문 페이지

파싱 결과를 페이지 단위로 반환합니다. FE 원문 뷰어가 이 데이터를 렌더링합니다.

```json
[
  {
    "page": 11,
    "sectionId": "sec-5",
    "size": { "width": 595, "height": 842 },
    "blocks": [
      { "id": "b-11-2", "kind": "p", "text": "2027년 매출 24억 원, 영업이익률 18% 달성",
        "bbox": { "x": 0.12, "y": 0.31, "w": 0.66, "h": 0.03 } },
      { "id": "b-11-3", "kind": "table", "caption": "[표 5-1] 연도별 매출 추정",
        "head": ["구분", "2025", "2026", "2027", "합계"],
        "rows": [["매출", "3.2억", "9.6억", "24억", "36.8억"]],
        "bbox": { "x": 0.12, "y": 0.38, "w": 0.76, "h": 0.18 } }
    ]
  }
]
```

- `kind`: `h2` · `p` · `table` · `figure`
- `blocks[].id`가 **하이라이트 앵커의 기준**입니다. Finding · Annotation이 이 id를 참조합니다.
- 페이지 수가 많으면 `?from=&to=` 범위 조회를 MVP2에 추가합니다.

### 5-3. 앵커(Anchor) — 좌표 + 텍스트 **둘 다** 저장

회의 결정: 좌표 기반과 텍스트 기반을 모두 JSON에 담습니다. 공통 스키마:

```json
{
  "blockId": "b-11-2",
  "page": 11,
  "bbox": { "x": 0.12, "y": 0.31, "w": 0.66, "h": 0.03 },
  "quote": "2027년 매출 24억 원",
  "prefix": "본문 5. 매출 · 재무 계획 ",
  "suffix": ", 영업이익률 18%",
  "charStart": 1240,
  "charEnd": 1256
}
```

| 필드 | 용도 |
|---|---|
| `bbox` | 뷰어 하이라이트 렌더링. **페이지 크기 대비 0~1 비율**로 저장 |
| `quote` / `prefix` / `suffix` | 좌표가 어긋났을 때 텍스트로 재탐색하는 폴백 |
| `blockId` / `charStart` / `charEnd` | 파싱 결과 안에서의 정확한 위치 |

> 회의에서 지적된 "페이지·좌표가 가변이라 XY로 못 찍을 수 있다" 문제 때문에 **절대 픽셀 대신 비율**로 저장하고,
> 렌더 시 `bbox` → 실패하면 `quote` 재탐색 순서로 복원합니다. 최소 `blockId + quote`만 있어도 하이라이트가 됩니다.

### 5-4. `GET /review-jobs/{jobId}/findings` — 검토 항목

**Query** (선택): `type`, `verdict`, `sort=CONFIDENCE_DESC|PAGE_ASC`
→ MVP1은 **전체 반환**으로 충분합니다. 정렬·필터는 FE가 처리하고 있습니다.

```json
[
  {
    "id": "fd-01",
    "type": "ERROR",
    "page": 11,
    "sectionId": "sec-5",
    "title": "매출 합계와 표 5-1 소계가 불일치합니다",
    "description": "본문 '2027년 매출 24억 원'과 표 5-1 합계 36.8억 원의 연도별 값이 맞지 않습니다 · 차이 3,200만 원",
    "confidence": 0.96,
    "method": "DETERMINISTIC",
    "calculation": { "expression": "3.2 + 9.6 + 24", "expected": "36.8억", "actual": "24억", "diff": "3,200만 원" },
    "verdict": "PENDING",
    "decidedAt": null,
    "evidence": [
      { "blockId": "b-11-2", "page": 11, "label": "본문 문단 · 2027년 매출 24억 원", "bbox": {}, "quote": "" }
    ]
  }
]
```

- `calculation`은 `DETERMINISTIC`일 때만 채웁니다 (RAG면 `null`).
- `evidence[]`는 5-3 앵커 스키마 + `label`(FE 표시용 문구).

### 5-5. `PATCH /review-jobs/{jobId}/findings/{findingId}/verdict` — 판정

```json
{ "verdict": "ACCEPTED" }
```

`200` 갱신된 Finding · `400 INVALID_VERDICT`

- `PENDING`으로 되돌리는 것이 **되돌리기** 동작입니다.
- 판정 변경 이력은 별도 테이블(`finding_verdict_history`)에 append. 조회 API는 MVP2.
- ⚠ 회의에서 나온 "우측 장표 주석은 **검토 반영 버튼 클릭 시 저장**" 요구는
  **이 판정 API + 주석 생성 API를 한 액션에서 순차 호출**하는 것으로 처리합니다. (6-2 참고)

---

## 6. 주석 (ANNOTATION)

회의 결정: **주석 테이블 1개, 주석 1건 = 1 row.** 리스트를 통째로 저장하지 않습니다.

### 6-1. Annotation 모델

```json
{
  "id": "an-1",
  "jobId": "job-1",
  "documentId": "doc-1",
  "source": "VIEWER",
  "findingId": null,
  "body": "이 수치는 재무팀 확인 필요",
  "color": "ORANGE",
  "anchor": { "blockId": "b-11-2", "page": 11, "bbox": {}, "quote": "2027년 매출 24억 원" },
  "createdAt": "2026-09-03T10:12:00+09:00",
  "updatedAt": "2026-09-03T10:12:00+09:00"
}
```

| 필드 | 설명 |
|---|---|
| `source` | `VIEWER` = 원문 위에서 직접 단 주석 (**자동 저장**) · `FINDING` = 검토 항목에서 단 주석 (**버튼 클릭 시 저장**) |
| `findingId` | `source=FINDING`일 때만 값이 있음 |
| `anchor` | 5-3 스키마. `source=FINDING`이면 해당 Finding의 첫 evidence를 복사 |

> 저장 시점 차이는 **FE가 언제 POST를 보내느냐**의 문제이고 API는 하나로 통일합니다.
> BE는 `source` 값으로 어느 경로에서 생성됐는지만 구분해 저장하면 됩니다.

### 6-2. `POST /review-jobs/{jobId}/annotations` — 주석 생성

```json
{
  "source": "FINDING",
  "findingId": "fd-01",
  "body": "표 5-1 재계산 결과 첨부 요청",
  "color": "ORANGE",
  "anchor": { "blockId": "b-11-2", "page": 11, "bbox": {}, "quote": "2027년 매출 24억 원" }
}
```

`201` Annotation · `404` 잘못된 `findingId`

**검토 반영 버튼 클릭 시 FE 동작 순서**
1. `PATCH .../findings/{id}/verdict` `{ "verdict": "ACCEPTED" }`
2. 입력한 메모가 있으면 `POST .../annotations` `{ "source": "FINDING", "findingId": ... }`

> 회의에서 나온 버튼명 논의(검토 반영 → 주석 추가)는 **화면 문구만의 문제**로 API에는 영향이 없습니다.
> 두 호출을 하나로 묶고 싶으면 `PATCH verdict` 요청 본문에 `annotationBody`를 옵션으로 받는 방식도 가능합니다. 결정 필요.

### 6-3. `GET /review-jobs/{jobId}/annotations` — `200` `Annotation[]`
검토 화면 진입 시 1회 조회합니다. 필터 불필요.

### 6-4. `PATCH /annotations/{annotationId}` — `{ "body": "...", "color": "RED" }` → `200`
### 6-5. `DELETE /annotations/{annotationId}` — `204`

---

## 7. 완료 · 내보내기

### 7-1. `POST /review-jobs/{jobId}/complete` — 검토 완료

Request body 없음.

**Response `200`**
```json
{
  "id": "job-1",
  "status": "DONE",
  "completedAt": "2026-09-03T11:00:00+09:00",
  "summary": { "total": 8, "byType": {}, "decided": 8, "accepted": 5, "rejected": 3 }
}
```

- 문서 상태를 `DONE`으로 갱신하고 요약을 **스냅샷으로 저장**합니다.
- 이미 완료된 작업이면 `409 JOB_ALREADY_COMPLETED`.

### 7-2. `GET /review-jobs/{jobId}/export` — 요약 내보내기

**Query**: `format=pdf` (기본, MVP1은 pdf만) · `csv`는 MVP2

**Response `200`** — `application/pdf` 바이너리
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="bizxray-review-{documentId}.pdf"
```

회의 결정대로 **원본 PDF에 하이라이트 + 주석을 반영한 결과물**입니다. 검토 의견서 양식이 아닙니다.

> ⚠ 브라우저에서 `Authorization` 헤더를 붙여 받아야 하므로 FE는 `fetch` → `blob` → 다운로드로 처리합니다.
> 단순 `<a href>`로 열 수 있게 하려면 단기 토큰이 붙은 다운로드 URL을 별도로 발급해야 합니다. 결정 필요.

### 7-3. `GET /review-jobs/{jobId}/report` — 검토 의견서 (**MVP2**)
현재 FE에 화면(`ReportView.vue`)이 이미 있으나, 회의에서 MVP2로 미뤘습니다.
MVP1 기간에는 FE가 Mock을 계속 사용합니다. 스키마는 Max 버전에서 확정.

---

## 8. AI 연동 계약

> 평가 항목입니다. **모델만 끼우면 바로 동작하는 수준**으로 입출력을 고정합니다.
> 개발은 목업으로 진행하되, 목업이 반환하는 JSON은 아래 스키마를 **그대로** 따릅니다.

### 8-1. 파이프라인 위치

```
PDF 업로드
   └─ 룰 기반 파싱 (AI 미사용)         → Section / Page / Block
        └─ 요소 추출 (수치·주장·기술·KPI) → ExtractedElement
             ├─ 결정적 검산 (코드)        → Finding(method=DETERMINISTIC)
             └─ LLM 관계 판단 (AI)        → Finding(method=RAG)
```

### 8-2. LLM 입력 (BE → 모델)

```json
{
  "jobId": "job-1",
  "documentTitle": "AI 매장 안내 로봇 사업계획서 v3",
  "criteria": ["NUMERIC_CONSISTENCY", "CLAIM_EVIDENCE", "TECH_KPI_FIT", "CROSS_ITEM_CONFLICT"],
  "sections": [{ "id": "sec-5", "title": "매출 · 재무 계획", "page": 11 }],
  "elements": [
    { "id": "el-101", "type": "NUMBER", "sectionId": "sec-5", "page": 11,
      "text": "2027년 매출 24억 원", "blockId": "b-11-2" },
    { "id": "el-102", "type": "TABLE", "sectionId": "sec-5", "page": 11,
      "text": "[표 5-1] 매출 3.2억 / 9.6억 / 24억 / 합계 36.8억", "blockId": "b-11-3" }
  ]
}
```
`type`: `NUMBER` · `CLAIM` · `TECH` · `KPI` · `TABLE`

### 8-3. LLM 출력 (모델 → BE) — **이 스키마를 벗어나면 파싱 실패 처리**

```json
{
  "modelVersion": "sllm-x-0.1",
  "promptVersion": "review-v1",
  "generatedAt": "2026-09-03T10:00:00+09:00",
  "findings": [
    {
      "type": "ERROR",
      "method": "RAG",
      "sectionId": "sec-5",
      "page": 11,
      "title": "매출 합계와 표 5-1 소계가 불일치합니다",
      "description": "본문 24억 원과 표 합계 36.8억 원의 연도별 값이 맞지 않습니다",
      "confidence": 0.96,
      "calculation": null,
      "evidence": [
        { "elementId": "el-101", "blockId": "b-11-2", "page": 11, "quote": "2027년 매출 24억 원" }
      ]
    }
  ]
}
```

**모델에 강제할 규칙**
1. `evidence`가 비어 있는 Finding은 **반환 금지**. 근거를 못 찾으면 그 항목을 만들지 않습니다.
2. `quote`는 **원문에 존재하는 문자열 그대로**. 요약·재작성 금지 (BE가 원문 대조로 검증하고, 불일치 시 해당 Finding을 폐기).
3. `confidence`는 0~1 실수.
4. 사업 성공 가능성 판정 금지. 판단 근거가 부족하면 `type: "NO_EVIDENCE"`.
5. 출력은 JSON 단일 객체. 마크다운 코드펜스·설명 문장 금지.

### 8-4. 산출물

| 파일 | 내용 |
|---|---|
| `docs/ai/prompt-review.md` | 검토 분석 프롬프트 (system + user 템플릿, few-shot) |
| `docs/ai/schema-finding.json` | 8-3 응답 JSON Schema (검증용) |
| `docs/ai/prompt-chat.md` | AI 질문용 프롬프트 (미결 확정 후) |

### 8-5. `POST /review-jobs/{jobId}/questions` — AI 질문 (**미결**)

FE에 화면(`AskPanel.vue`)은 있으나 **엔드포인트 설계 범위 미결**입니다. 잠정안:

```json
{ "question": "이 매출 추정이 앞 시장 진입 계획과 맞는지 확인해줘", "selection": { "page": 11, "quote": "..." } }
```
```json
{
  "answer": "p.9는 40개 매장, p.11은 62개 매장을 전제합니다.",
  "evidences": [{ "blockId": "b-9-2", "page": 9, "label": "2027년 3개 지역 40개 매장 확보" }],
  "promotable": true
}
```
근거를 못 찾으면 `answer`에 `"이 문서 안에서 관련 근거를 찾지 못했습니다. 추측하지 않습니다."`, `evidences: []`.

---

## 9. FE 연동 시 바뀌는 부분

이 명세가 확정되면 FE에서 손대야 하는 곳입니다.

| 파일 | 변경 |
|---|---|
| `src/api/endpoints.js` | `documents.folders` 삭제, `annotations` 4개 추가, `export` 추가 |
| `src/api/modules/documents.js` | `folders()` 제거, `list` 파라미터에서 `folderId` 제거 |
| `src/api/modules/reviews.js` | `annotations` CRUD · `export` 추가 |
| `src/constants/enums.js` | `AnnotationSource` 추가 |
| `src/views/auth/SignupView.vue` | **이름 입력 필드 제거** |
| `src/views/library/LibraryView.vue` | **폴더 사이드바 제거**, 검색 placeholder를 "문서명 검색"으로 |
| `src/views/settings/SettingsView.vue` | 이름 변경 UI 제거 (이메일만 노출) |
| `src/components/common/TheHeader.vue` | 아바타 이니셜을 `name` → `email` 첫 글자로 |
| `src/stores/auth.js` | `user.name` 제거 |

로그인 응답 필드명도 하나 다릅니다. FE 목업은 `{ token, user }`, 이 명세는 `{ accessToken, user }` 입니다.
**명세를 기준**으로 FE를 맞추겠습니다.

---

## 10. 미결 사항

| # | 항목 | 결정 필요 |
|---|---|---|
| 1 | 상태 필터 vs 정렬 | `GET /documents`에서 어느 쪽을 실제 구현할지 |
| 2 | 판정 + 주석 | 별도 2회 호출 유지 vs `PATCH verdict`에 `annotationBody` 합치기 |
| 3 | 내보내기 인증 | blob 다운로드 vs 단기 토큰 URL |
| 4 | AI 질문 | 엔드포인트 채택 여부 및 sLLM 공유 방식 |
| 5 | 목차 추출 | 룰 기반 vs AI |
| 6 | 진행률 전달 | 폴링 유지 vs SSE |
| 7 | 문서 버전 | `version` 컬럼의 의미 (재업로드 시 새 문서 vs 버전 증가) — ERD 확인 필요 |
| 8 | 검토 점수 | 산식 확정 시 `summary.score` 추가 |
| 9 | 원본 PDF 저장 위치 | DB LOB vs 파일시스템 vs 오브젝트 스토리지 |
