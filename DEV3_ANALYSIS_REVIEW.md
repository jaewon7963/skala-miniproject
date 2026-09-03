# 개발자 3 — Analysis / Review

> **LogicCheck 백엔드 · Analysis/Review 도메인 구현 지침**
> 사용법: 이 파일과 `API_명세서_v2.md`, `AGENTS.md` 를 Claude에 함께 준다.
> 이 파일은 담당 범위와 팀 계약을 정의하고, 상세 요청·응답 스펙은 명세서를 따른다.
> 두 문서가 충돌하면 **명세서가 우선**이며, 불일치를 발견하면 구현하지 말고 팀에 보고한다.

---

## A. 공유 계약 (3개 파일 공통)

> **이 절은 `DEV1` · `DEV2` · `DEV3` 세 파일에 글자 그대로 동일하게 들어 있다.**
> 혼자 수정하면 세 사람의 구현이 갈라진다. 변경이 필요하면 팀 논의 후 세 파일을 동시에 고친다.

### A-1. 담당 분할

| 개발자 | 도메인 | 담당 API | 담당 테이블 |
|---|---|---|---|
| **1** | Identity / Auth | 1~5 (5건) | `users`, `sessions` |
| **2** | Document | 6~15 (10건) | `tags`, `documents`, `document_tags`, `pages`, `sections`, `extracted_elements` |
| **3** | Analysis / Review | 16~27 (12건) | `review_jobs`, `findings`, `finding_evidence`, `finding_elements`, `finding_decisions`, `annotations`, `validation_rules` |

**개발자 1은 추가로 프로젝트 뼈대를 소유한다.** `global/` 패키지 전체(Security, JWT, 예외 처리, 공통 응답)와 Flyway baseline이 여기 포함된다. 2·3번은 이 뼈대가 올라오기 전까지 컨트롤러를 붙일 수 없으므로, 1번의 Day 0 산출물이 팀 전체의 선행 조건이다.

### A-2. 패키지 구조 및 소유권

```
com.logiccheck
├─ global/                      ★ 개발자1 소유 — 2·3번은 읽기만 한다
│  ├─ config/     SecurityConfig · CorsConfig · JpaConfig · AsyncConfig
│  ├─ security/   JwtTokenProvider · JwtAuthenticationFilter · UserPrincipal · @CurrentUser
│  ├─ exception/  ErrorCode · BusinessException · GlobalExceptionHandler
│  └─ common/     ErrorResponse · PageResponse · BaseTimeEntity
│
├─ user/                        ★ 개발자1
├─ auth/                        ★ 개발자1
│
├─ tag/                         ★ 개발자2
├─ document/                    ★ 개발자2
│  ├─ parse/                      비동기 파싱 파이프라인
│  ├─ storage/                    파일 저장 추상화
│  ├─ structure/                  Page · Section · ExtractedElement
│  └─ port/                       ★ 개발자2가 정의, 개발자3이 소비
│
├─ review/                      ★ 개발자3
│  ├─ job/                        ReviewJob
│  ├─ finding/                     Finding · Evidence · Element · Decision
│  ├─ annotation/                  Annotation
│  └─ port/                       ★ 개발자3이 정의, 개발자2가 소비
└─ ai/                          ★ 개발자3 — AI 서버 연동 클라이언트
```

**규칙**

- 자기 소유 패키지 밖의 파일은 **읽기만 한다.** 수정이 필요하면 멈추고 담당자에게 요청한다.
- 다른 도메인의 Repository·Entity를 직접 import 하지 않는다. 반드시 A-5의 Port를 거친다.
- 다른 도메인의 DTO를 재사용하지 않는다. 필요하면 자기 패키지에 정의한다.

### A-3. 공통 규약 (명세 1-2)

| 항목 | 규칙 |
|---|---|
| 성공/실패 판별 | **HTTP 상태 코드**로만. 응답 본문에 `success` 플래그를 두지 않는다 |
| 성공 응답 | 리소스 객체 또는 배열을 **래퍼 없이 그대로** 반환. `ApiResponse<T>` 로 감싸지 않는다 |
| 목록 응답 | 페이징이 있는 API만 `{ items, total, page, size }` |
| 실패 응답 | `{ code, message, details }` |
| ID 표현 | DB는 `BIGINT`, **요청·응답에서는 문자열**. `@JsonSerialize(using = ToStringSerializer.class)` 또는 DTO에서 `String.valueOf()` |
| 날짜 | 서버는 UTC 저장, 응답은 `+09:00` 오프셋 포함 ISO-8601 |
| 페이지 번호 | 1부터 시작. 기본 크기 20 |
| Content-Type | 요청 `application/json` (PDF 업로드만 `multipart/form-data`), 응답 `application/json; charset=utf-8` |
| 인증 헤더 | `Authorization: Bearer {accessToken}` |
| Base URL | `http://localhost:8081/api` |

`ApiResponse` 래퍼를 쓰지 않는다는 점을 특히 주의한다. 습관적으로 감싸면 FE 전체가 깨진다.

### A-4. ErrorCode — 개발자1이 Day 0에 **전량** 생성

명세 1-3에 코드가 전부 정의되어 있으므로 **처음에 다 만들어 두고 이후 아무도 이 파일을 수정하지 않는다.** 3명이 나눠서 추가하면 반드시 충돌한다.

```java
public enum ErrorCode {
    // 공통
    INVALID_REQUEST(400, "요청 값을 확인해주세요."),
    UNAUTHORIZED(401, "로그인이 필요합니다."),
    FORBIDDEN(403, "접근 권한이 없습니다."),
    NOT_FOUND(404, "요청하신 데이터를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(500, "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),

    // 개발자1 — 회원/인증
    EMAIL_ALREADY_EXISTS(409, "이미 가입된 이메일입니다."),
    INVALID_PASSWORD(422, "비밀번호는 8자 이상, 영문과 숫자를 각각 1자 이상 포함해야 합니다."),
    INVALID_CREDENTIALS(401, "이메일 또는 비밀번호를 확인해주세요."),
    INVALID_REFRESH_TOKEN(401, "다시 로그인해주세요."),

    // 개발자2 — 문서
    UNSUPPORTED_FILE_TYPE(415, "PDF 파일만 업로드할 수 있습니다."),
    FILE_TOO_LARGE(413, "최대 50MB까지 업로드할 수 있습니다."),
    DUPLICATE_FILE(409, "이미 업로드한 파일입니다."),

    // 개발자3 — 분석/검토
    DOCUMENT_NOT_READY(409, "문서 파싱이 완료되지 않았습니다."),
    JOB_ALREADY_RUNNING(409, "이미 분석이 진행 중입니다."),
    NO_REVIEW_JOB(404, "아직 분석하지 않은 문서입니다."),
    JOB_ALREADY_COMPLETED(409, "이미 검토가 완료된 문서입니다."),
    EXPORT_FAILED(500, "PDF 생성에 실패했습니다.");
    ...
}
```

새 코드가 필요하다고 판단되면 **추가하지 말고** 팀에 먼저 묻는다. 명세에 없는 코드를 반환하면 FE가 처리하지 못한다.

### A-5. 도메인 간 인터페이스 (Port)

도메인 경계를 넘는 접근은 **오직 아래 3개 Port로만** 한다. 시그니처는 확정이며 임의 변경 금지다.

**Port ①** — 개발자2 정의 · 개발자3 소비 · 분석 착수 검증과 Job 조회 응답 조합에 사용

```java
package com.logiccheck.document.port;

public interface DocumentQueryPort {
    /** 소유자가 아니거나 soft delete 상태면 Optional.empty() */
    Optional<DocumentMetaView> findMetaForOwner(Long documentId, Long userId);

    record DocumentMetaView(
        Long    documentId,
        Long    ownerId,
        String  title,
        Integer pageCount,     // 파싱 전 null
        String  parseStatus    // PENDING·PARSING·EXTRACTING·DONE·FAILED
    ) {}
}
```

**Port ②** — 개발자2 정의 · 개발자3 소비 · 분석 파이프라인의 입력

```java
package com.logiccheck.document.port;

public interface DocumentStructurePort {
    List<PageView>    findPages(Long documentId);
    List<SectionView> findSections(Long documentId);
    List<ElementView> findElements(Long documentId);

    record PageView(int pageNo, Double width, Double height, String textLayer) {}
    record SectionView(Long id, Long parentId, String title, Integer pageNo,
                       int ordering, String source) {}          // source: ORIGINAL·EXTRACTED
    record ElementView(Long id, int pageNo, String kind, String rawText,
                       BigDecimal numericValue, String unit, BBox bbox) {}
    record BBox(double x, double y, double w, double h) {}      // 0~1 상대 좌표
}
```

**Port ③** — 개발자3 정의 · 개발자2 소비 · `displayStatus` 와 `counts` 계산에 사용

```java
package com.logiccheck.review.port;

public interface ReviewJobQueryPort {
    /** 문서별 최신 Job 1건. Job이 없는 문서는 Map에 키가 없다 */
    Map<Long, LatestJobView> findLatestByDocumentIds(Collection<Long> documentIds);

    record LatestJobView(
        Long   jobId,
        Long   documentId,
        String status,        // PENDING·RUNNING·DONE·FAILED
        String reviewStatus   // IN_REVIEW·COMPLETED
    ) {}
}
```

> Port ②·③이 서로를 필요로 하므로 **개발자2와 3은 양방향 의존**이다. 이 때문에 A-7의 스텁 전략이 필수다.

### A-6. 마이그레이션 정책

- **초기 스키마는 개발자1이 ERD ver2 전체 DDL을 baseline 한 파일로 커밋한다.**
  `V20260904_0900__baseline.sql`
  FK 순서(`users` → `documents` → `review_jobs`) 때문에 나눠 만들면 순서가 꼬인다.
- baseline 이후 변경은 각자 타임스탬프 파일로 추가한다.
  `V{YYYYMMDD}_{HHmm}__{설명}.sql` — 순번 방식(`V1__`, `V2__`)은 3명 동시 작업 시 충돌 확정.
- **머지된 마이그레이션 파일은 절대 수정하지 않는다.** 새 파일을 추가한다.
- `ddl-auto` 는 `validate`. 임의로 `update` 로 바꾸지 않는다.

### A-7. 스텁 전략 — 남을 기다리지 않고 개발하기

Port 소비자는 **자기 패키지 안에** 임시 구현을 만들어 혼자 개발한다.

1. **Day 1: Port 인터페이스 파일만 먼저 커밋한다** (구현 없이). 그래야 소비자 쪽이 컴파일된다.
2. 소비자는 아래 형태의 스텁을 자기 패키지에 만든다.

```java
@Primary
@Profile("stub")
@Component
public class StubDocumentQueryPort implements DocumentQueryPort {
    @Override
    public Optional<DocumentMetaView> findMetaForOwner(Long documentId, Long userId) {
        return Optional.of(new DocumentMetaView(documentId, userId, "스텁 문서", 21, "DONE"));
    }
}
```

3. 실행: `--spring.profiles.active=local,stub`
4. 상대 구현이 머지되면 `stub` 프로파일을 빼고 통합 검증한다.
5. **스텁 파일은 통합 완료 후 삭제한다.** 남겨두면 `@Primary` 가 운영에서 터진다.

### A-8. 절대 금지

- `ApiResponse<T>` 등 래퍼로 성공 응답 감싸기 (A-3 위반)
- `global/` 패키지 수정 (개발자1 외)
- 다른 도메인의 Entity·Repository 직접 import
- `ErrorCode` 에 코드 추가
- 명세에 없는 필드를 응답에 추가 (FE가 무시해도 계약 위반이다)
- `build.gradle` 의존성 추가 (팀 합의 후)
- 머지된 마이그레이션 파일 수정
- `git add -A` / `git add .`
- 커밋 메시지·PR·주석에 AI 도구 관련 문자열

---

## B. 담당 범위

### B-1. 담당 API (MVP1 11건 + MVP2 1건)

**[분석]**

| # | 기능 | Method | Path |
|---|---|---|---|
| 16 | 분석 시작 (비동기) | `POST` | `/api/review-jobs` |
| 17 | 분석 작업 조회 (폴링·검토화면 진입) | `GET` | `/api/review-jobs/{jobId}` |
| 18 | 최근 분석 작업 조회 | `GET` | `/api/documents/{documentId}/review-jobs/latest` |
| 19 | 검토 완료 처리 | `POST` | `/api/review-jobs/{jobId}/complete` |
| 20 | 검토 결과 PDF 내보내기 | `GET` | `/api/review-jobs/{jobId}/export` — **MVP2** |

**[검토사항]**

| # | 기능 | Method | Path |
|---|---|---|---|
| 21 | 검토사항 목록 조회 | `GET` | `/api/review-jobs/{jobId}/findings` |
| 22 | 검토사항 상세 조회 | `GET` | `/api/findings/{findingId}` |
| 23 | 검토사항 판정 | `POST` | `/api/findings/{findingId}/decisions` |

**[주석]**

| # | 기능 | Method | Path |
|---|---|---|---|
| 24 | 주석 목록 조회 | `GET` | `/api/review-jobs/{jobId}/annotations` |
| 25 | 주석 생성 | `POST` | `/api/review-jobs/{jobId}/annotations` |
| 26 | 주석 수정 | `PATCH` | `/api/annotations/{annotationId}` |
| 27 | 주석 삭제 (soft) | `DELETE` | `/api/annotations/{annotationId}` |

여기에 **AI 서버 연동**과 **검증 파이프라인**이 포함된다.

**보류 (구현하지 않는다)** — AI 질문(P3), 검토 의견서(P3), 판정 이력 조회(미결).

### B-2. 담당 테이블

`review_jobs` · `findings` · `finding_evidence` · `finding_elements` · `finding_decisions` · `annotations` · `validation_rules`

### B-3. 경로 주의 — 내 것인 `/api/documents/**`

**18번은 경로가 `/api/documents/{documentId}/review-jobs/latest` 지만 내 담당이다.** 리소스가 ReviewJob이기 때문이다.

- 컨트롤러는 `review/job` 패키지에 두고 경로만 이 값을 쓴다.
- 개발자2의 `DocumentController` 와 같은 prefix를 공유하므로, **양쪽 다 와일드카드 매핑을 쓰지 않기로 합의**했다. 정확한 경로만 명시한다.

---

## C. 구현 순서

개발자1의 Day 0 뼈대, 개발자2의 Port 인터페이스(Day 1)를 확인한 뒤 시작한다.

### 1단계 — 계약 먼저 (Day 1, 반나절)

**`ReviewJobQueryPort` 인터페이스 파일을 구현 없이 먼저 커밋한다.** 개발자2가 이걸 기다린다(문서 목록의 `displayStatus` 계산).

```
review/port/ReviewJobQueryPort.java
```

내용은 A-5 그대로. 커밋 후 2번에게 알린다.

### 2단계 — Job 뼈대

1. `ReviewJob` Entity + Repository
2. 분석 시작 (16) — **AI 없이 상태 전이만** 먼저. `PENDING` 생성 후 202 반환
3. 분석 작업 조회 (17) — `terminal` 파생, `summary` 는 아직 `null`
4. 최근 작업 조회 (18)
5. `ReviewJobQueryPort` 실제 구현 → 2번에게 통합 요청

### 3단계 — Finding 읽기 경로

6. `Finding` · `FindingEvidence` · `FindingElement` Entity
7. 시드 데이터로 Finding 몇 건을 수동 INSERT (AI 없이 21·22를 검증하기 위함)
8. 검토사항 목록 (21) · 상세 (22)
9. `summary` 집계 계산 → 17번에 반영

### 4단계 — 판정과 주석

10. `FindingDecision` + 판정 (23)
11. `Annotation` + 주석 CRUD (24~27)
12. 검토 완료 (19)

### 5단계 — AI 파이프라인

13. `ValidationRule` + `ruleset_version` 스냅샷
14. 결정적 검산 (rule 기반 Finding)
15. AI 서버 연동 클라이언트
16. RAG 판단 (LLM 기반 Finding)
17. 인용문 검증 및 폐기 로직

> **AI를 마지막에 붙이는 순서가 중요하다.** 11개 API 중 AI가 필요한 건 16번의 백그라운드 파이프라인뿐이다. 먼저 붙이면 나머지 10개를 검증할 수 없다.

---

## D. 도메인 핵심 규칙

### D-1. 두 상태를 혼동하지 않는다

```
review_jobs.status        PENDING → RUNNING → DONE / FAILED     (AI 분석)
review_jobs.review_status IN_REVIEW → COMPLETED                 (사람의 검토)
```

**`status = DONE` 은 AI 분석이 끝난 것이고, `review_status = COMPLETED` 는 사람이 검토를 끝낸 것이다.** 두 상태는 서로 독립이며, 검토 완료 시 `status` 와 `parse_status` 는 변경하지 않는다.

`terminal` 은 `status ∈ {DONE, FAILED}` 일 때만 `true` 다. FE 폴링 중단 신호이므로 `review_status` 와 무관하다.

### D-2. 분석 시작 선행 조건 (16)

| 조건 | 위반 시 |
|---|---|
| `documents.parse_status = DONE` | `409 DOCUMENT_NOT_READY` |
| 같은 문서에 `status ∈ {PENDING, RUNNING}` Job 없음 | `409 JOB_ALREADY_RUNNING` |
| 대상 문서의 소유자 | `403 FORBIDDEN` |

세 조건 모두 `DocumentQueryPort.findMetaForOwner()` 로 확인한다. **`documents` 테이블을 직접 조회하지 않는다.**

동시 실행 차단은 애플리케이션 검사만으로 부족하다. ERD의 `UQ(document_id) WHERE status='RUNNING'` 부분 유니크 인덱스가 최종 방어선이므로, 제약 위반 예외를 `409 JOB_ALREADY_RUNNING` 으로 변환한다.

**202 Accepted 를 즉시 반환한다.** `Location: /api/review-jobs/{id}` 헤더를 붙이고, 파이프라인 완료를 기다리지 않는다. 응답 시점의 `startedAt`·`finishedAt`·`rulesetVersion` 은 전부 `null` 이다.

### D-3. Job 조회 응답 조합 (17)

`documentTitle` 과 `pageCount` 는 **개발자2의 `documents` 에서 온다.** `DocumentQueryPort` 로 조회해 조합한다.

`summary` 는 **`status = DONE` 일 때만 채우고 그 외에는 `null`** 이다.

```json
"summary": {
  "total": 8,
  "bySeverity": { "ERROR": 3, "WARNING": 3, "INFO": 2 },
  "decided": 2, "accepted": 1, "rejected": 1, "open": 6
}
```

**별도 스냅샷을 저장하지 않고 조회 시 계산한다.** `decided = accepted + rejected`, `open = status = OPEN` 개수다.

응답에서 제외: `reviewScore`(산식 미확정), `steps`·`parseProgress`·`analyzeProgress`·`partialFailures`·`discovered`(ERD에 컬럼 없음, FE 목업 확정).

### D-4. 검토사항 목록 (21)

- **Query 파라미터가 없다. 전체를 반환한다.** 정렬·필터는 FE가 처리한다(`stores/review.js:visibleFindings`). 서버 필터를 넣으면 판정할 때마다 재조회가 필요해진다.
- 정렬: `severity` → `confidence DESC`. `findings(job_id, severity, confidence DESC)` 인덱스를 활용한다.
- **분석 미완료 Job(`PENDING`·`RUNNING`)은 빈 배열 `[]`** 을 반환한다. 404가 아니다.

`method` 는 저장 컬럼이 아니라 **파생**이다. `rule_id != null` → `DETERMINISTIC`, `null` → `RAG`.
`calculation` 은 `DETERMINISTIC` 일 때만 채우고 RAG면 `null`. `embedding` 은 응답에서 제외한다.

### D-5. 좌표와 근거 — 이 프로젝트의 핵심 가치

> **모든 Finding은 최소 1건의 `finding_evidence` 를 가진다. 인용문이 원문과 불일치하면 서버가 해당 항목을 폐기한다.**

- `bbox` 는 **절대 픽셀이 아니라 페이지 크기 대비 0~1 상대 좌표**다. 페이지 변동·줌 변경에 견디기 위함이다. 정규화 기준은 개발자2가 채우는 `pages.width`·`height` 다.
- 렌더링 복원 순서는 `bbox` → 실패 시 `quote` 재탐색이다. **`quote` 만 있어도 하이라이트가 가능해야 하므로 `quote` 를 반드시 채운다.**
- **`evidence[].id` 가 하이라이트 앵커 키다.** ERD에 Block 테이블이 없어 FE의 `anchorId` 를 이 값으로 대체한다. 응답에서 빠뜨리면 원문↔항목 점프가 동작하지 않는다.
- AI가 생성한 인용문을 원문(`pages.text_layer`)과 대조해 불일치하면 Finding을 저장하지 않는다. 근거 없는 지적을 반환하지 않는 것이 이 서비스의 전제다.

### D-6. 판정 (23)

```
findings.status:  OPEN ──ACCEPT──→ ACCEPTED
                       ──REJECT──→ REJECTED
```

- 판정 이력은 `finding_decisions` 에 누적한다. `findings.status` 만 갱신하고 이력을 남기지 않으면 안 된다.
- **`review_status = COMPLETED` 인 Job의 Finding은 판정을 변경할 수 없다** → `409 JOB_ALREADY_COMPLETED`.
- FE의 되돌리기(`undoVerdict`) 버튼은 비활성화된다. 되돌리기 API를 만들지 않는다.
- 미결 #5: 판정과 주석을 한 번에 저장하는 `annotationBody` 필드 채택 여부. 채택하면 트랜잭션 하나로 처리하고, 분리하면 FE가 23·25를 연달아 호출한다. **착수 전 확정한다.**

### D-7. 주석 (24~27)

ERD 기준 `annotations` 단일 테이블이다.

| 구분 | `findingId` | `pageNo`/`bbox` | 생성 시점 |
|---|---|---|---|
| PDF 자유 주석 | `null` | 사용 | 원문 드래그 후 저장 |
| Finding 주석 | 값 존재 | 선택 | 판정 클릭 시 |

- **`source`/`origin` 필드를 두지 않는다.** `finding_id` 유무로 완전히 구분된다.
- **`color` 필드를 두지 않는다.** MVP1 미지원이며 ERD에 컬럼이 없다.
- `job_id` 가 NOT NULL이므로 **ReviewJob 생성 이후에만 주석을 만들 수 있다.** 분석 전 자유 주석은 지원하지 않는다.
- `author_id` 는 JWT 사용자로 고정한다. 요청에서 받지 않는다.
- soft delete(`deleted_at`). 조회 시 `deleted_at IS NULL`.

### D-8. 검토 완료 (19)

- `review_status` 를 `COMPLETED` 로, `completed_at` 을 기록한다.
- `status`·`parse_status` 는 건드리지 않는다.
- 재완료 요청은 `409 JOB_ALREADY_COMPLETED`.
- 미결 #10: `status != DONE` 인 Job에 complete 요청 시 코드를 `DOCUMENT_NOT_READY` 재사용할지 신규로 둘지 미확정. **새 ErrorCode를 임의 추가하지 말고 팀에 묻는다.**

### D-9. 재현 가능성

> 검증 규칙을 DB에 버전으로 두고(`validation_rules`), 분석 시점 버전을 `review_jobs.ruleset_version` 에 **스냅샷**으로 남겨 과거 판정을 그대로 재현한다.

파이프라인 착수 시 현재 ruleset 버전을 Job에 기록한다. 규칙이 나중에 바뀌어도 과거 Job의 판정 근거가 유지된다.

### D-10. AI 파이프라인 (16번 백그라운드)

```
1. 문서 구조 조회       DocumentStructurePort 로 Page·Section·ExtractedElement 조회
2. 검증 규칙 조회       ValidationRule (ruleset_version 스냅샷)
3. 결정적 검산          계산식·허용 오차 → Finding(rule_id 존재)
4. 관련 요소·문맥 검색   임베딩·RAG
5. AI 관계 판단         → Finding(rule_id = null)
6. 검토사항 저장        Finding
7. 관련 요소 연결       FindingElement
8. 원문 근거 저장       FindingEvidence
```

**AI 연동 시 보안 규칙** (`.ai/BACKEND_GUIDE.md` §8.10)

- API Key는 서버 측 보관, 환경변수 분리. 코드·로그에 남기지 않는다.
- Timeout과 실패 처리 정책을 반드시 설정한다. 실패 시 `status = FAILED` + `error_code` 기록.
- **AI 응답을 신뢰 데이터처럼 바로 저장하지 않는다.** JSON Schema 검증 + 인용문 원문 대조를 거친다.
- 모델명·Temperature 등은 코드가 아닌 설정으로 분리한다.

---

## E. 다른 개발자와의 접점

### E-1. 내가 제공하는 것

| 대상 | 산출물 | 언제 |
|---|---|---|
| 2번 | `ReviewJobQueryPort` 인터페이스 | Day 1 (구현 없이 먼저) |
| 2번 | 실제 구현 | 2단계 |

2번은 이걸로 문서 목록의 `displayStatus` 와 `counts` 를 계산한다. **복수 조회 시그니처를 단건으로 바꾸면 2번에서 N+1이 발생한다.**

### E-2. 내가 소비하는 것

`DocumentQueryPort` · `DocumentStructurePort` (개발자2 제공).

2번 구현 전까지 내 패키지에 스텁을 둔다.

```java
@Primary
@Profile("stub")
@Component
public class StubDocumentQueryPort implements DocumentQueryPort {
    @Override
    public Optional<DocumentMetaView> findMetaForOwner(Long documentId, Long userId) {
        return Optional.of(new DocumentMetaView(documentId, userId, "스텁 문서", 21, "DONE"));
    }
}
```

`parseStatus = "DONE"` 을 반환하면 16번의 선행 조건을 통과해 Job 생성부터 검증할 수 있다. `DOCUMENT_NOT_READY` 경로를 테스트할 때만 `"PARSING"` 으로 바꾼다.

`DocumentStructurePort` 스텁은 빈 리스트를 반환해도 된다. AI 파이프라인은 5단계라 그때는 2번 구현이 나와 있을 것이다.

### E-3. 개발자2와 함께 결정할 것

- **미결 #4 원문 뷰어 렌더링 방식** — PDF.js 원본 렌더링을 택하면 `bbox` 좌표계 기준이 바뀐다. 내 `finding_evidence.bbox` 가 직접 영향을 받으므로 2번과 같이 정한다.
- **미결 #3 목록에 `latestJobId` 포함 여부** — 포함하면 FE가 18번을 건너뛴다.

---

## F. FE 호환 포인트 (부록 A-6)

| FE 파일 | 변경 내용 |
|---|---|
| `api/endpoints.js` | `verdict` → `findings/{id}/decisions`. `annotations`(4건)·`export` 추가 |
| `api/modules/reviews.js` | `updateVerdict` → `createDecision`. `annotations` CRUD 추가 |
| `constants/enums.js` | `FINDING_TYPE` → `SEVERITY(ERROR/WARNING/INFO)`. `VERDICT.PENDING` → `FINDING_STATUS.OPEN`. `JOB_STATUS.PARTIAL` 제거 |
| `stores/review.js` | `annotations` 를 localStorage → API로 전환. `undoVerdict` 비활성화. `pollJob` 종료 조건을 `terminal` 로 |
| `components/review/DocumentViewer.vue` | `blocks[].id` 앵커 → `evidence.id` + `bbox` 좌표 기반 하이라이트 |
| `components/review/FindingCard.vue` | 되돌리기 버튼 비활성화. `method` 파생 필드 사용 |

**검토 화면은 5개 API를 병렬 호출한다**(17, 14, 15, 21, 24). 이 중 3개가 내 담당이다. 한 개라도 느리면 화면 전체가 늦어지므로 21번의 인덱스를 확인한다.

---

## G. 내가 결정해야 할 미결 사항 (부록 A-7)

| # | 항목 | 결정할 내용 | 영향 |
|---|---|---|---|
| 5 | 판정 + 주석 API 통합 | 23번의 `annotationBody` 채택(1회 호출·트랜잭션 보장) vs 분리 2회 호출 | 23, 25 |
| 9 | `PARTIAL` 상태 | ERD `review_jobs.status` 에 없음. 부분 실패 표현하려면 컬럼 추가 필요 | 17 |
| 10 | 완료 선행조건 오류 코드 | `status != DONE` Job에 complete 요청 시 코드 | 19 |
| 11 | 내보내기 인증 방식 | blob 다운로드 유지 vs 단기 서명 URL | 20 (MVP2) |
| 21 | RAG `chunk_id` 생성 규칙 | 검색 단위를 `extracted_elements` vs 별도 Chunk | 파이프라인 |
| 22 | AI 실행 메타 저장 여부 | 모델·프롬프트·임베딩 버전 저장 | MVP2 |

5번과 10번은 착수 전에 확정한다. 9번은 FE가 이미 `JOB_STATUS.PARTIAL` 을 제거하기로 했으므로 **컬럼을 추가하지 않는 쪽이 기본**이다.

---

## H. 완료 기준

- [ ] 11개 엔드포인트(MVP1) Method·Path·Status Code가 명세와 일치
- [ ] 16번이 202 + `Location` 헤더 반환, 파이프라인을 기다리지 않음
- [ ] 16번 선행 조건: 파싱 미완료 409 `DOCUMENT_NOT_READY`, 중복 실행 409 `JOB_ALREADY_RUNNING`
- [ ] 동시 요청 2건에서 Job이 1개만 생성됨 (DB 제약 포함)
- [ ] `terminal` 이 `status ∈ {DONE, FAILED}` 에만 `true`
- [ ] `summary` 가 `status = DONE` 일 때만 채워지고 그 외 `null`
- [ ] `documentTitle`·`pageCount` 를 `DocumentQueryPort` 로 조합 (documents 직접 조회 없음)
- [ ] 21번이 Query 파라미터를 받지 않고 전체 반환
- [ ] 분석 미완료 Job의 findings가 빈 배열
- [ ] `method` 가 `rule_id` 유무로 파생됨
- [ ] `calculation` 이 DETERMINISTIC 일 때만 채워짐
- [ ] 모든 Finding에 `evidence` 가 1건 이상, 각 evidence에 `id`·`quote` 존재
- [ ] `bbox` 가 0~1 상대 좌표
- [ ] `embedding` 이 응답에 없음
- [ ] 판정 시 `finding_decisions` 에 이력이 쌓임
- [ ] 완료된 Job의 Finding 판정 시 409 `JOB_ALREADY_COMPLETED`
- [ ] 19번이 `status`·`parse_status` 를 변경하지 않음
- [ ] 주석에 `source`·`color` 필드가 없음
- [ ] `author_id` 를 요청에서 받지 않음
- [ ] 주석 soft delete 후 목록에서 제외됨
- [ ] AI API Key가 코드·로그에 없음
- [ ] 타인 리소스 접근 시 403
- [ ] `id` 필드가 문자열, 날짜에 `+09:00`
- [ ] 응답을 래퍼로 감싸지 않음
- [ ] 통합 시 스텁 파일 삭제 완료

### E2E 최소 시퀀스

```
(문서 파싱 완료 상태 준비)
분석 시작 202 → Job 조회 폴링(terminal=true) → findings 200
→ finding 상세 200 → 판정 201 → summary 갱신 확인
→ 주석 생성 201 → 목록 200 → 수정 200 → 삭제 204
→ 검토 완료 200 → 판정 재시도 409
```

---

## I. Claude 작업 지시 템플릿

```
첨부: API_명세서_v2.md, DEV3_ANALYSIS_REVIEW.md, AGENTS.md

[작업] 명세서 16번 분석 시작 API를 구현한다. AI 파이프라인은 아직 붙이지 않고
       Job 생성과 상태 전이까지만 구현한다.

착수 전:
1. 명세서 "### 16. 분석 시작"과 부록 A-3(상태 흐름)을 읽는다
2. DEV3 문서 D-1, D-2, D-10을 읽는다
3. 기존 ReviewJob Entity와 DocumentQueryPort 스텁을 읽는다

제약:
- documents 테이블을 직접 조회하지 않는다. DocumentQueryPort 만 사용한다
- 202 Accepted 를 즉시 반환하고 파이프라인 완료를 기다리지 않는다
- 응답의 startedAt, finishedAt, rulesetVersion 은 null 이다
- ErrorCode 에 새 코드를 추가하지 않는다
- 응답을 래퍼로 감싸지 않는다

완료 후 DEV3 문서 H 체크리스트로 자체 검증하고 결과를 보고한다.
```

---

## 부록. 시작 전 확인

- [ ] 개발자1의 Day 0 뼈대가 develop에 머지됐는가
- [ ] 개발자2의 Port 인터페이스 2개가 커밋됐는가
- [ ] 판정+주석 통합 여부(미결 #5)를 확정했는가
- [ ] 완료 선행조건 오류 코드(미결 #10)를 확정했는가
- [ ] 원문 뷰어 렌더링 방식(미결 #4)을 2번과 확정했는가
- [ ] AI 서버 엔드포인트·인증 방식을 확보했는가
- [ ] `ReviewJobQueryPort` 를 커밋하고 2번에게 알렸는가
