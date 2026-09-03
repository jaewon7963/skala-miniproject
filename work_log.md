# 작업 기록 — Analysis / Review 도메인 (개발자3)

담당 범위는 `DEV3_ANALYSIS_REVIEW.md` 를 따른다. API 16~27, `review_*` 7개 테이블.
단계마다 커밋 1개를 남기고 이 파일에 항목을 추가한다.

## 착수 시점의 전제 차이 (중요)

`DEV3_ANALYSIS_REVIEW.md` 는 개발자1의 Day 0 뼈대와 개발자2의 Port 2개가 이미 머지된 상태를
가정하지만, 착수 시점의 저장소는 그렇지 않았다.

- `BE/business` 는 Spring Initializr 기본 골격. 패키지 `com.example.business`,
  의존성은 `spring-boot-starter-webmvc` 하나 — JPA · Postgres 드라이버 · Flyway · Validation 없음
- `global/` 패키지(ErrorCode · 예외 처리 · 공통 응답 · JWT) 미존재
- `document/port/` 의 Port 인터페이스 2개 미존재
- 문서가 참조하는 `API_명세서_v2.md` 와 ERD ver2 DDL 이 저장소에 없음.
  있는 것은 `docs/api-spec.md`(Light v0.1)뿐이고 DEV3 문서와 충돌한다
  (`FINDING_TYPE`↔`SEVERITY`, `PATCH .../verdict`↔`POST .../decisions`,
   annotation `source`/`color` 유무, `steps`/`parseProgress` 등)

→ **DEV3 문서를 유일한 계약으로 삼고 진행한다.** `review_*` DDL 은 DEV3 B-2 / D절 기준으로 작성했다.
`docs/api-spec.md` 와 어긋나는 부분은 DEV3 문서를 따랐다. v2 명세가 올라오면 대조가 필요하다.

## 확정한 미결 사항

| # | 항목 | 결정 |
|---|---|---|
| 5 | 판정 + 주석 API 통합 | 23번 요청 본문에 **optional `annotationBody`** 채택. 값이 있으면 같은 트랜잭션에서 주석 생성. 25번 API 도 그대로 유지 |
| 9 | `PARTIAL` 상태 | 컬럼 추가하지 않음 (문서 G절 기본값, FE 도 `JOB_STATUS.PARTIAL` 제거) |
| 10 | 완료 선행조건 오류 코드 | `status != DONE` 인 Job 에 complete 요청 시 **409 `DOCUMENT_NOT_READY` 재사용**. 새 ErrorCode 를 추가하지 않는다 |

## 개발자1 · 2 산출물 통합 절차

지금은 혼자 실행 · 검증할 수 있게 임시 파일을 두었다. 통합은 3단계로 끝난다.

1. `com/logiccheck/review/support/` 삭제
2. 아래 import 를 개발자1 · 2 의 실제 경로로 치환
3. 실행 프로파일에서 `stub` 제거

**canonical 위치에 이미 만들어 둔 파일** — 개발자1 · 2 가 동일 파일을 올리면 삭제만 하면 된다.
내용은 DEV3 문서 A절에 글자 그대로 인용된 것을 그대로 옮겼다.

| 파일 | 근거 |
|---|---|
| `com/logiccheck/global/exception/ErrorCode.java` | A-4 enum 전량 |
| `com/logiccheck/document/port/DocumentQueryPort.java` | A-5 Port ① 시그니처 |
| `com/logiccheck/document/port/DocumentStructurePort.java` | A-5 Port ② 시그니처 |

**`review/support/` 의 임시 파일과 교체 대상**

| 임시 파일 | 교체 대상 (개발자1) |
|---|---|
| `review/support/BusinessException` | `global/exception/BusinessException` |
| `review/support/ErrorResponse` | `global/common/ErrorResponse` |
| `review/support/GlobalExceptionHandler` | `global/exception/GlobalExceptionHandler` |
| `review/support/BaseTimeEntity` | `global/common/BaseTimeEntity` |
| `review/support/UserPrincipal` | `global/security/UserPrincipal` |
| `review/support/CurrentUser` | `global/security/@CurrentUser` |
| `review/support/CurrentUserArgumentResolver` | 개발자1의 `JwtAuthenticationFilter` 로 대체 후 삭제 |
| `review/support/ReviewSupportWebConfig` | 삭제 |
| `review/support/ReviewPersistenceConfig` | 개발자1이 뼈대를 `com.logiccheck` 로 옮기면 삭제 |
| `review/support/StubDocumentQueryPort` | 개발자2 구현으로 대체 후 삭제 (A-7 / E-2) |
| `review/support/StubDocumentStructurePort` | 개발자2 구현으로 대체 후 삭제 |

`UserPrincipal` 은 `userId()` 접근자만 쓴다. 개발자1 구현의 접근자 이름이 다르면 그 부분만 맞춘다.

## 실행 방법

```bash
docker compose up -d postgres          # 저장소 루트
cd BE/business
./gradlew bootRun --args='--spring.profiles.active=stub'
```

`stub` 프로파일에서는 JWT 대신 **`X-User-Id` 헤더**로 인증 주체를 대체한다.

```bash
curl -H 'X-User-Id: 1' http://localhost:8081/api/review-jobs/1
```

> 로컬에 이미 Postgres 가 5432 를 점유하고 있으면 컨테이너를 다른 포트로 올린다.
> `POSTGRES_PORT=5433 docker compose up -d postgres` 로 올리고
> `DB_PORT=5433 ./gradlew bootRun ...` 으로 실행한다.
> (`DB_HOST` · `DB_PORT` · `DB_NAME` · `DB_USERNAME` · `DB_PASSWORD` 를 환경변수로 받는다.)

---

## S0 — 기반 (기능 아님, 선행 조건)

- 2026-09-03
- 구현 범위: 영속성 · 공통 계약 · 임시 지원 계층. 엔드포인트는 아직 없다.

**추가**
- `BE/business/src/main/java/com/logiccheck/global/exception/ErrorCode.java`
- `BE/business/src/main/java/com/logiccheck/document/port/DocumentQueryPort.java`
- `BE/business/src/main/java/com/logiccheck/document/port/DocumentStructurePort.java`
- `BE/business/src/main/java/com/logiccheck/review/ReviewTimes.java`
- `BE/business/src/main/java/com/logiccheck/review/support/` 11개 파일 (위 표 참고)
- `BE/business/src/main/resources/db/migration/V20260904_1200__review_domain.sql`
- `work_log.md`, `DEV3_ANALYSIS_REVIEW.md`

**변경**
- `BE/business/build.gradle` — 의존성 5개 추가
- `BE/business/src/main/resources/application.properties` — datasource · JPA · Flyway · Jackson
- `BE/business/src/main/java/com/example/business/BusinessApplication.java` — `scanBasePackages` 에 `com.logiccheck` 추가
- `BE/business/src/main/java/com/example/business/review/ReviewController.java` — `@Profile("mock")` 추가
- `BE/business/src/test/java/com/example/business/BusinessApplicationTests.java` — `@Disabled`

**결정 · 편차**

1. **`build.gradle` 의존성 5개 추가** (A-8 "팀 합의 후" 항목). JPA 없이는 엔티티가 컴파일되지 않고,
   Flyway 없이는 H 체크리스트의 DB 제약 검증이 불가하다. 최소 5개로 제한했다.
   ```
   spring-boot-starter-data-jpa
   spring-boot-starter-validation
   spring-boot-flyway                 (Boot 4 는 autoconfig 가 별도 모듈. flyway-core 만으로는 동작하지 않는다)
   flyway-database-postgresql         (runtimeOnly)
   postgresql                         (runtimeOnly)
   ```
   테스트 의존성은 추가하지 않았다. 기존 `spring-boot-starter-webmvc-test` 가
   `spring-boot-starter-test`(JUnit · AssertJ · Mockito) 를 포함한다.

2. **`ux_review_jobs_active` 에 `PENDING` 포함** — ERD 원문은 `WHERE status = 'RUNNING'` 이지만
   신규 Job 이 `PENDING` 으로 생성되므로(D-2) `PENDING` 을 포함해야 "동시 요청 2건에 Job 1개" 를
   보장할 수 있다. 검증 결과 아래 참고.

3. **타 도메인 FK 없음** — 개발자1의 baseline(`users` · `documents`)이 없어 FK 를 걸면
   Flyway 가 실패한다. `document_id` · `author_id` · `actor_id` · `element_id` · `section_id` 를
   plain `BIGINT` 으로 두었다. baseline 머지 후 별도 마이그레이션으로 FK 를 추가해야 한다.
   대상 목록은 마이그레이션 파일 상단 주석에 있다. **← 후속 과제**

4. **`findings.calculation` 을 JSONB 대신 컬럼 4개로** (`calc_expression` · `calc_expected` ·
   `calc_actual` · `calc_diff`). Boot 4 는 Jackson 3(`tools.jackson`)을 쓰고 Hibernate 의
   JSON 매핑은 Jackson 2 를 찾으므로, `ddl-auto=validate` 환경에서 위험을 없애려고 평컬럼으로 갈랐다.
   응답 형태 `{ expression, expected, actual, diff }` 는 그대로 유지된다.

5. **`findings.embedding` 은 컬럼만 두고 엔티티에 매핑하지 않는다.** 응답에 새어 나갈 수 없게 하는
   가장 확실한 방법이다(H 체크리스트 "embedding 이 응답에 없음"). RAG 단계에서 필요하면 그때 매핑한다.

6. **`findings.decided_at` 은 컬럼만 두고 응답에 넣지 않았다.** DEV3 문서가 이 필드를 명시하지 않고
   A-8 이 "명세에 없는 필드를 응답에 추가" 를 금지한다. v2 명세 확인 후 결정한다. **← 팀 확인 필요**

7. **기존 목업 `com.example.business.review.ReviewController` 를 `@Profile("mock")` 으로 격리.**
   `GET /api/findings/{id}` 가 명세 22번과 겹쳐 그대로 두면 Ambiguous mapping 으로 기동에 실패한다.
   삭제 대신 프로파일 격리로 되돌리기 쉽게 했다.

8. **`BusinessApplicationTests` 를 `@Disabled` 처리.** `@SpringBootTest` 가 Postgres 와
   `stub` 프로파일을 요구한다. 컨텍스트 로딩은 `bootRun` 으로 검증한다.
   Testcontainers 를 도입하면 복원한다. **← 후속 과제**

9. **`spring.jackson.datatype.datetime.*`** — Boot 4 / Jackson 3 에서
   `WRITE_DATES_AS_TIMESTAMPS` 가 `SerializationFeature` 에서 `DateTimeFeature` 로 옮겨졌다.
   Boot 3 관례인 `spring.jackson.serialization.write-dates-as-timestamps` 는 기동 실패를 일으킨다.

**검증**

- `./gradlew compileJava` 통과
- `./gradlew test` 통과 (BUILD SUCCESSFUL)
- `bootRun` 기동 성공, Flyway 가 `v20260904.1200` 1건 적용:
  `Successfully applied 1 migration to schema "public"`
- 테이블 7개 + `flyway_schema_history` 생성 확인
- `ddl-auto=validate` 통과 (엔티티 없음 → S2 부터 실질 검증)
- 부분 유니크 인덱스 동작 확인 (직접 INSERT):
  | 순서 | 값 | 결과 |
  |---|---|---|
  | 1 | `(document_id=1, PENDING)` | `INSERT 0 1` |
  | 2 | `(document_id=1, RUNNING)` | `ERROR: duplicate key value violates unique constraint "ux_review_jobs_active"` |
  | 3 | `(document_id=1, DONE)` | `INSERT 0 1` (부분 인덱스 대상 밖) |

**후속 과제**
- baseline 머지 후 타 도메인 FK 추가 마이그레이션
- `BusinessApplicationTests` 복원 (Testcontainers)
- `findings.decided_at` 응답 포함 여부 팀 확인
- 20번 export(MVP2) 는 범위 밖 — PDF 라이브러리 의존성과 미결 #11(인증 방식) 확정 후

---

## S1 — 계약 우선: `ReviewJobQueryPort`

- 2026-09-03
- 구현 범위: Port 인터페이스 파일만. 구현은 S2 에서 붙인다 (DEV3 C-1).

**추가**
- `BE/business/src/main/java/com/logiccheck/review/port/ReviewJobQueryPort.java`

내용은 A-5 Port ③ 그대로다. 복수 조회 시그니처를 단건으로 바꾸면 개발자2 쪽에서 N+1 이 발생한다(E-1).

**검증**
- `./gradlew compileJava` 통과

**후속 과제**
- **개발자2에게 통보 필요.** 문서 목록의 `displayStatus` · `counts` 계산이 이 Port 를 기다린다.
  구현 없이 인터페이스만 먼저 올렸으니 소비자 쪽 컴파일은 지금부터 가능하다.

---

## S2 — Job 뼈대: API 16 · 17 · 18 + Port ③ 구현

- 2026-09-03
- 구현 범위: 분석 시작(16) · 분석 작업 조회(17) · 최근 분석 작업 조회(18) · `ReviewJobQueryPort` 구현.
  AI 파이프라인은 아직 붙이지 않았다. `summary` 는 S3 에서 채운다.

**추가**
- `review/job/JobStatus.java` · `ReviewStatus.java` — `status` 와 `review_status` 를 별개 타입으로 분리 (D-1)
- `review/job/ReviewJob.java` · `ReviewJobRepository.java`
- `review/job/ReviewJobService.java` · `ReviewJobController.java`
- `review/job/dto/CreateReviewJobRequest.java` · `ReviewJobResponse.java` · `JobSummaryView.java`
- `review/port/ReviewJobQueryAdapter.java` — Port ③ 구현
- `src/test/java/com/logiccheck/review/job/ReviewJobServiceTest.java` (12건)
- `src/test/java/com/logiccheck/review/job/ReviewJobControllerTest.java` (9건)

**변경**
- `review/support/GlobalExceptionHandler.java` — Spring MVC 자체 예외 처리 보강 (아래 4번)

**결정 · 편차**

1. **선행 조건 3개를 모두 `DocumentQueryPort` 로만 확인한다.** `documents` 테이블을 직접 조회하지 않는다(D-2).
   `parse_status != DONE` → 409 `DOCUMENT_NOT_READY`, 진행 중 Job → 409 `JOB_ALREADY_RUNNING`,
   비소유자 → 403 `FORBIDDEN`.

2. **Port 가 비소유자와 soft delete 를 구분하지 못한다.** `findMetaForOwner` 는 두 경우 모두
   `Optional.empty()` 다. D-2 의 소유자 조건에 맞춰 **403 `FORBIDDEN` 으로 통일**했다.
   삭제된 문서에 404 를 주려면 Port 시그니처가 바뀌어야 하므로 개발자2와 논의가 필요하다. **← 팀 확인 필요**

3. **`saveAndFlush` + `DataIntegrityViolationException` → 409 변환.** 애플리케이션 검사만으로는
   동시 요청을 막을 수 없어 `ux_review_jobs_active` 부분 유니크 인덱스가 최종 방어선이다(D-2).
   즉시 flush 해야 제약 위반이 서비스 안에서 잡힌다.

4. **`GlobalExceptionHandler` 에 Spring MVC 예외 처리를 추가했다.** `@ExceptionHandler(Exception.class)` 가
   `NoResourceFoundException` 까지 삼켜 **없는 경로가 404 대신 500 으로 나갔다.**
   Spring 7 의 `NoResourceFoundException` 은 `ErrorResponseException` 을 상속하지 않고
   `org.springframework.web.ErrorResponse` 인터페이스만 구현하므로 타입 기반 `@ExceptionHandler` 로는
   잡히지 않는다. `instanceof` 로 걸러 원래 상태 코드를 유지하게 했다.
   경로 변수 타입 불일치 · 잘못된 JSON 본문도 400 으로 매핑했다.

5. **`errorCode` 를 17번 응답에 포함했다.** D-10 이 실패 시 `error_code` 기록을 요구하고 FE 가
   실패 사유를 표시해야 한다. DEV3 문서가 17번 응답 필드를 전량 열거하지 않아 판단으로 넣었다.
   v2 명세 확인이 필요하다. **← 팀 확인 필요**

6. **Port ③ 은 상관 서브쿼리 1회로 구현했다.** 단건 조회를 반복하면 개발자2의 문서 목록에서
   N+1 이 발생한다(E-1). Job 이 없는 문서는 결과 Map 에 키가 없다 — 계약대로다.

**검증**

- `./gradlew test` — 21건 통과 (Service 12 · Controller 9), 실패 0
- `ddl-auto=validate` 통과 — 엔티티 매핑이 `V20260904_1200` 스키마와 일치 (기동 성공)
- 실제 Postgres 대상 curl E2E:

  | 요청 | 결과 |
  |---|---|
  | `POST /api/review-jobs` | `202` · `Location: /api/review-jobs/4` · `status=PENDING` · `startedAt`·`finishedAt`·`rulesetVersion`·`summary` 모두 `null` |
  | 같은 문서 재요청 | `409 JOB_ALREADY_RUNNING` |
  | `GET /api/review-jobs/{id}` | `200` · `documentTitle`·`pageCount` 가 Port 에서 조합됨 · 래퍼 없음 |
  | `GET /api/documents/1/review-jobs/latest` | `200` |
  | `GET /api/documents/999/review-jobs/latest` | `404 NO_REVIEW_JOB` |
  | `GET /api/review-jobs/999999` | `404 NOT_FOUND` |
  | 인증 헤더 없음 | `401 UNAUTHORIZED` |
  | `GET /api/review-jobs/abc` | `400 INVALID_REQUEST` |
  | `GET /api/nope` | `404 NOT_FOUND` (수정 전 500) |
  | `DELETE /api/review-jobs` | `405` |

- **동시성**: 같은 문서에 동시 6건 요청 → `202` 1건, `409` 5건, DB 행 1건
- **`terminal` 파생**: `status=DONE` 으로 바꾸면 `terminal: true`
- **날짜 오프셋**: `started_at` 을 `2026-09-04 00:20:00+00` 로 저장 → 응답 `"2026-09-04T09:20:00+09:00"`
- **Port ③ 쿼리 의미**: 문서 100·101 에 Job 2건, 102 에 1건, 999 에 0건인 상태에서
  동등 SQL 실행 → 문서별 최신 1건만 반환, 999 는 결과에 없음

**후속 과제**
- Port ③ 은 개발자2가 소비할 때 통합 검증한다. 지금은 JPQL 부트스트랩 검증 + 동등 SQL 확인까지다
- 삭제된 문서 접근 시 404 vs 403 (위 2번)
- 17번 응답의 `errorCode` 포함 여부 (위 5번)

---

## S3 — Finding 읽기 경로: API 21 · 22 + summary

- 2026-09-03
- 구현 범위: 검토사항 목록(21) · 상세(22), 명세 17 의 `summary` 집계 완성.
  AI 파이프라인 대신 시드 데이터로 검증했다.

**추가**
- `review/finding/Severity.java` · `FindingStatus.java` · `FindingMethod.java`
- `review/finding/Finding.java` · `FindingEvidence.java` · `FindingElement.java`
- `review/finding/FindingRepository.java` · `SeverityStatusCount.java`
- `review/finding/FindingService.java` · `FindingController.java` · `FindingSummaryProvider.java`
- `review/finding/dto/FindingResponse.java` (`CalculationView` · `EvidenceView` · `BBoxView` 중첩)
- `review/job/JobFindingSummary.java` — job 이 finding 내부를 직접 알지 않게 하는 경계
- `src/main/resources/db/seed/review_seed.sql` · `review/support/ReviewSeedRunner.java`
- 테스트 15건: `FindingServiceTest`(7) · `FindingControllerTest`(6) · `FindingSummaryProviderTest`(2)

**변경**
- `review/job/ReviewJobService.java` — `JobWithDocument` 에 `summary` 추가, `status = DONE` 일 때만 채움
- `review/job/ReviewJobController.java` — `summary` 전달
- `ReviewJobServiceTest` — summary 검증 2건 추가 (총 14건)

**결정 · 편차**

1. **`embedding` 을 엔티티에 아예 매핑하지 않았다.** 컬럼은 DDL 에 있지만 `Finding` 에 필드가 없다.
   응답에 새어 나갈 경로가 원천적으로 없다. `ddl-auto=validate` 는 DB 쪽 여분 컬럼을 문제 삼지 않는다.

2. **목록 정렬을 서비스에서 수행한다.** `severity` 의 의미 순서(ERROR > WARNING > INFO)가
   문자열 정렬(ERROR · INFO · WARNING)과 달라 SQL `ORDER BY severity` 로는 원하는 순서가 안 나온다.
   `findings(job_id, severity, confidence DESC)` 인덱스는 `job_id` 조회를 받고,
   최종 정렬은 `FindingService.LIST_ORDER` 가 한다. 21번은 한 Job 전체를 반환하므로(D-4)
   페이징이 없고 집합이 작아 메모리 정렬로 충분하다.
   `confidence` 가 null 이면 같은 severity 안에서 뒤로 보낸다.

3. **`@EntityGraph(attributePaths = "evidence")`** 로 근거를 함께 가져온다. 없으면 목록에서 N+1 이 난다.

4. **`FAILED` Job 은 findings 를 그대로 반환한다.** D-4 의 "분석 미완료" 는 `PENDING` · `RUNNING` 이고
   `FAILED` 는 종료 상태다. 부분 실패로 일부 항목이 저장됐을 수 있어 빈 배열로 만들지 않았다.
   대신 `summary` 는 D-3 대로 `DONE` 일 때만 채운다 — `FAILED` 면 `null` 이다.

5. **`calculation` 은 값이 하나도 없으면 `null`** 이다. `rule_id` 가 있어도 calc_* 네 컬럼이 모두
   비었으면 빈 객체 대신 `null` 을 내린다.

6. **`bbox` 는 네 값이 모두 있을 때만 내려보낸다.** 하나라도 없으면 `bbox: null` 이고,
   FE 는 `quote` 재탐색으로 복원한다 (D-5).

7. **`evidence[]` 에 `blockId` · `prefix` · `suffix` 를 넣지 않았다.** ERD 에 Block 테이블이 없어
   FE 의 `anchorId` 를 `evidence.id` 로 대체한다(D-5). `prefix`/`suffix` 는 ERD 에 컬럼이 없다.
   `charStart` · `charEnd` 는 컬럼이 있어 포함했다.

8. **`FindingResponse.jobId` 를 포함했다.** 22번(상세)이 findingId 만 받으므로 소속 Job 을
   알 방법이 없으면 FE 가 곤란하다. DEV3 문서가 21·22 응답 필드를 전량 열거하지 않아 판단으로 넣었다.
   **← 팀 확인 필요** (`errorCode` · `decidedAt` 과 함께)

9. **시드는 `db/seed/` 에 두고 `seed` 프로파일에서만 실행한다.** `db/migration/` 에 넣으면
   Flyway 가 운영에도 적용한다. `document_id = 900001` 로 표시해 실행마다 지우고 다시 만든다.

**검증**

- `./gradlew test` — 38건 통과 (Job 23 · Finding 15), 실패 0
- `ddl-auto=validate` 통과 — `Finding` · `FindingEvidence` · `FindingElement` 매핑이 스키마와 일치
- 시드 적용 후 실제 Postgres 대상 curl:

  ```
  21) GET /api/review-jobs/16/findings  → 200, 5건
      ERROR   conf=0.96  DETERMINISTIC  calc=있음  ev=1
      ERROR   conf=0.91  DETERMINISTIC  calc=있음  ev=1
      WARNING conf=0.84  RAG            calc=null  ev=1
      WARNING conf=0.78  RAG            calc=null  ev=1
      INFO    conf=0.65  RAG            calc=null  ev=1
      embedding 키 존재: False
      모든 항목에 evidence 1건 이상: True
      모든 evidence 에 id·quote: True
      bbox 0~1 범위: True
  22) GET /api/findings/1               → 200, calculation·evidence·bbox 정상
  ```

- **summary 불변식** (판정 2건 주입 후):
  `{"total": 5, "bySeverity": {"ERROR": 2, "WARNING": 2, "INFO": 1}, "decided": 2, "accepted": 1, "rejected": 1, "open": 3}`
  `decided == accepted + rejected`, `total == sum(bySeverity)`, `total == decided + open` 모두 성립
- **summary 조건**: `RUNNING` → `null`, `FAILED` → `null`, `DONE` → 채워짐
- **21번 빈 배열**: `RUNNING` Job → `[]` + `200` (404 아님)
- **FAILED Job**: findings 5건 반환, `terminal: true`, `errorCode: "AI_TIMEOUT"`, `summary: null`

**후속 과제**
- 응답 추가 필드 3개(`errorCode` · `jobId` · `decidedAt` 제외 여부) v2 명세 대조
- `FindingElement` 는 엔티티만 만들었고 파이프라인(S5)에서 채운다

---

## S4a — 주석 CRUD(24~27) + 검토 완료(19)

- 2026-09-03
- **계획과 순서를 바꿨다.** 원래 판정(23)을 먼저 하려 했지만, 미결 #5 채택으로 23번이
  `annotationBody` 로 주석을 생성하므로 주석 쪽이 선행이다. 판정은 S4b 로 옮겼다.

**추가**
- `review/annotation/Annotation.java` · `AnnotationRepository.java`
- `review/annotation/AnnotationService.java` · `AnnotationController.java`
- `review/annotation/dto/CreateAnnotationRequest.java` · `UpdateAnnotationRequest.java`
  · `AnnotationResponse.java` · `AnchorPayload.java`
- 테스트 20건: `AnnotationServiceTest`(11) · `AnnotationControllerTest`(9)

**변경**
- `review/job/ReviewJob.java` — `completeReview()` · `isReviewCompleted()` 추가
- `review/job/ReviewJobService.java` — `completeReview()` 추가
- `review/job/ReviewJobController.java` — 19번 엔드포인트 추가
- `ReviewJobServiceTest` — 완료 관련 4건 추가 (총 18건)

**결정 · 편차**

1. **`source` · `color` 컬럼과 필드를 두지 않았다** (D-7). `finding_id` 유무로 완전히 구분된다.
   요청에 `source`/`color`/`authorId` 를 넣어 보내도 무시된다 — 확인함.

2. **`author_id` 는 JWT 사용자로 고정.** 요청 DTO 에 필드가 없다.
   `authorId: "999"` 를 보내도 DB 에는 인증 주체(1)가 저장된다 — 확인함.

3. **`authorId` 를 응답에도 넣지 않았다.** MVP1 은 단독 사용자 검토라 표시할 곳이 없다.
   협업 기능이 생기면 추가한다.

4. **Finding 주석에 `anchor` 를 생략하면 해당 Finding 의 첫 evidence 를 복사한다** (api-spec 6-1).
   좌표를 직접 보내면 그 값을 쓴다.

5. **`anchor` 는 좌표도 인용문도 없으면 `null` 로 내린다.** 빈 객체를 만들지 않는다.

6. **중첩 `@Valid` 누락으로 500 이 나던 버그를 잡았다.** `AnchorPayload.bbox` 에 `@Valid` 가 없어
   `bbox.x = 1.5` 같은 값이 검증을 통과하고 DB CHECK 제약(`ck_annotations_bbox`)에서 터져
   `DataIntegrityViolationException` → 500 이 됐다. `@Valid` + `@NotNull` 을 붙여 400 으로 바꿨다.
   bbox 는 네 값이 전부 있어야 하고 각 값은 0~1 이다. `page` 는 `@Positive`.

7. **19번은 `review_status` 와 `completed_at` 만 바꾼다** (D-8). `status` 를 건드리지 않는 것을
   서비스 테스트와 DB 조회로 이중 확인했다.

8. **미결 #10 적용**: `status != DONE` 인 Job 의 완료 요청 → 409 `DOCUMENT_NOT_READY`.
   `PENDING` · `RUNNING` · `FAILED` 세 경우 모두 확인했다.

9. **완료된 Job 에도 주석 생성 · 수정 · 삭제를 허용한다.** D-6 은 **판정**만 잠근다고 명시하고
   주석에 대해서는 아무 말이 없어 문서를 문자 그대로 따랐다. 검토 완료 후 주석까지 잠글지는
   기획 판단이 필요하다. **← 팀 확인 필요**

10. **`bbox` 응답 값의 소수점 자리수가 입력에 따라 다르다.** 직접 보낸 값은 `0.12`,
    evidence 에서 복사한 값은 `0.120000` 으로 나온다(BigDecimal scale). 수치는 동일하고
    FE 의 숫자 파싱에 영향이 없어 정규화하지 않았다.

**검증**

- `./gradlew test` — 62건 통과 (Job 27 · Finding 15 · Annotation 20), 실패 0
- `ddl-auto=validate` 통과 — `Annotation` 매핑이 스키마와 일치
- 실제 Postgres 대상 curl E2E:

  | 요청 | 결과 |
  |---|---|
  | `POST .../annotations` (자유 주석) | `201` · `findingId: null` · `anchor` 그대로 |
  | `POST .../annotations` (findingId, anchor 생략) | `201` · 첫 evidence 좌표 복사 (`page 11`, `bbox 0.12/0.31/0.66/0.03`) |
  | `authorId`·`source`·`color` 주입 시도 | 무시됨. DB `author_id = 1`(JWT 주체) |
  | `body: "  "` | `400` · `details.body` |
  | 다른 Job 의 `findingId` | `404 NOT_FOUND` |
  | `bbox.x = 1.5` | `400` · `details["anchor.bbox.x"]` (수정 전 500) |
  | `bbox` 일부 누락 | `400` · `w`·`h` 둘 다 |
  | `anchor.page = 0` | `400` |
  | `GET .../annotations` | `200` · 2건 · `source`·`color`·`authorId` 키 없음 |
  | `PATCH /api/annotations/{id}` | `200` · `body` 만 변경, `updatedAt` 갱신 |
  | `PATCH` 빈 본문 | `400` |
  | `DELETE /api/annotations/{id}` | `204` · 본문 없음 |
  | 재삭제 | `404` |
  | 삭제 후 `GET .../annotations` | 1건만 (DB 에는 `deleted_at` 채워진 행이 남아 있음) |
  | `POST .../complete` | `200` · `reviewStatus: COMPLETED` · `completedAt` 기록 |
  | 완료 후 DB | `status = DONE`(불변) · `review_status = COMPLETED` |
  | 재완료 | `409 JOB_ALREADY_COMPLETED` |
  | `PENDING` Job 완료 요청 | `409 DOCUMENT_NOT_READY` |

**후속 과제**
- 검토 완료 후 주석 잠금 여부 (위 9번)

---

## S4b — 판정: API 23 (미결 #5 통합 채택)

- 2026-09-03
- 구현 범위: 검토사항 판정(23). `annotationBody` 를 함께 받으면 판정과 주석을 한 트랜잭션에서 저장한다.

**추가**
- `review/finding/DecisionAction.java` — `ACCEPT → ACCEPTED` · `REJECT → REJECTED`
- `review/finding/FindingDecision.java` · `FindingDecisionRepository.java`
- `review/finding/dto/CreateDecisionRequest.java`
- 테스트 13건 추가 (`FindingServiceTest` 9 · `FindingControllerTest` 4)

**변경**
- `review/finding/Finding.java` — `decide(DecisionAction)` 추가
- `review/finding/FindingService.java` — `decide()` 추가, `requireOwnedFinding()` 로 소유권 검사 통합
- `review/finding/FindingController.java` — 23번 엔드포인트 추가

**결정 · 편차**

1. **요청 필드는 `action` 이다** (`ACCEPT` · `REJECT`). 구 명세의 `verdict: "ACCEPTED"` 가 아니라
   D-6 의 전이 라벨(`OPEN ──ACCEPT──▶ ACCEPTED`)과 `finding_decisions.action` CHECK 제약에 맞췄다.
   `HOLD` 같은 미지원 값은 400 이다.

2. **응답은 갱신된 Finding** (`FindingResponse`) 이다. 201 이지만 `Location` 헤더를 붙이지 않았다 —
   판정 이력 조회 API 가 보류라서 가리킬 리소스가 없다.
   **주의:** `annotationBody` 로 만든 주석의 id 는 이 응답에 없다. 필요하면 FE 가 24번을 다시 부르거나
   25번을 따로 쓴다. 응답을 `{finding, annotation}` 복합 객체로 만드는 것은 A-3 위반 소지가 있어 피했다.
   **← 팀 확인 필요**

3. **재판정을 허용한다.** D-6 이 "`review_status = COMPLETED` 인 Job 의 Finding 은 판정을
   변경할 수 없다" 고 못 박은 것은, 완료되지 않은 Job 에서는 변경이 가능하다는 뜻으로 읽었다.
   `finding_decisions` 에 이력이 누적되므로 변경 경로가 추적된다.
   **되돌리기(OPEN 복귀) API 는 만들지 않았다** — FE 의 `undoVerdict` 는 비활성화된다.

4. **`actor_id` 는 JWT 사용자로 고정.** 요청에 `actorId` 를 넣어 보내도 무시된다 — 확인함.

5. **`annotationBody` 처리는 `AnnotationService.createForFinding` 에 위임하고
   `Propagation.MANDATORY` 를 걸었다.** 트랜잭션 없이는 호출 자체가 실패하므로,
   판정만 커밋되고 주석이 따로 새는 상황이 구조적으로 불가능하다.
   좌표는 해당 Finding 의 첫 evidence 를 복사한다.

6. **`decided_at` 은 판정 시 기록하지만 응답에 넣지 않았다** (S0 6번과 동일한 이유).

**검증**

- `./gradlew test` — 75건 통과 (Job 27 · Finding 28 · Annotation 20), 실패 0
- `ddl-auto=validate` 통과 — `FindingDecision` 매핑이 스키마와 일치
- 실제 Postgres 대상 curl:

  | 요청 | 결과 |
  |---|---|
  | `POST /api/findings/{id}/decisions` `{"action":"ACCEPT"}` | `201` · `status: ACCEPTED` |
  | `{"action":"REJECT","note":...,"annotationBody":...}` | `201` · `status: REJECTED` |
  | `finding_decisions` | 2행. `actor_id=1`, `before_status=OPEN`, `after_status=ACCEPTED/REJECTED`, `note` 저장 |
  | `annotations` (annotationBody 로 생성) | 1행. `finding_id=17`, `author_id=1`, `page_no=14`, `bbox_x=0.12` — 첫 evidence 복사 확인 |
  | 재판정 | `201`, 해당 Finding 의 이력 2건 |
  | `{"action":"HOLD"}` | `400 INVALID_REQUEST` |
  | `{}` | `400` · `details.action` |
  | `actorId: "999"` 주입 | 무시. `actor_id` = 인증 주체(5) |
  | 완료된 Job 의 Finding 판정 | `409 JOB_ALREADY_COMPLETED` |

- **17번 summary 연동**: 판정 3건 후 `{"total":5,"decided":3,"open":2,...}` — `decided + open == total` 성립

**H절 E2E 최소 시퀀스 전체 통과**

```
분석 시작                       202
Job 조회 (terminal=true)        200
findings 목록                   200
finding 상세                    200
판정                            201
summary 갱신 (decided=3 open=2) 확인
주석 생성                       201
주석 목록                       200
주석 수정                       200
주석 삭제                       204
검토 완료                       200
판정 재시도                     409
완료 후 DB: status=DONE(불변) · review_status=COMPLETED
```

**MVP1 11개 엔드포인트 구현 완료.** 남은 것은 S5(AI 파이프라인)와 범위 밖인 20번(export, MVP2).

---

## S5a — 검증 규칙 + 결정적 검산 파이프라인

- 2026-09-03
- 구현 범위: `ValidationRule` · `ruleset_version` 스냅샷 · `@Async` 파이프라인 골격 ·
  결정적 검산 1종 · 인용문 검증. AI 연동은 S5b.

**추가**
- `review/rule/ValidationRule.java` · `ValidationRuleRepository.java` · `RulesetVersionResolver.java`
- `review/pipeline/FindingDraft.java` — 저장 전 검토사항. 결정적 검산과 AI 가 같은 형태로 결과를 낸다
- `review/pipeline/DeterministicChecker.java` — 검산기 인터페이스 (규칙 코드로 등록)
- `review/pipeline/NumericConsistencyChecker.java` — 검산기 1종
- `review/pipeline/QuoteVerifier.java` — 인용문 원문 대조 (D-5)
- `review/pipeline/ReviewPipeline.java` — `@Async` 오케스트레이션
- `review/pipeline/ReviewPipelineStore.java` — 트랜잭션 경계
- `review/finding/FindingElementRepository.java`
- `review/support/ReviewAsyncConfig.java` (TEMP — 개발자1의 `AsyncConfig` 로 교체)
- 테스트 24건: `NumericConsistencyCheckerTest`(8) · `QuoteVerifierTest`(8) · `ReviewPipelineTest`(6)
  + `ReviewJobServiceTest` 2건

**변경**
- `review/job/ReviewJob.java` — `markRunning(rulesetVersion)` · `markDone()` · `markFailed(errorCode)`
- `review/job/ReviewJobService.java` — 커밋 후 파이프라인 트리거
- `review/finding/Finding.java` · `FindingEvidence.java` · `FindingElement.java` — 생성 팩토리
- `review/support/StubDocumentStructurePort.java` — 표본 데이터 옵션
- `db/seed/review_seed.sql` — `NUMERIC_CONSISTENCY` 규칙 추가

**결정 · 편차**

1. **파이프라인은 트랜잭션 커밋 후에 시작한다.** `ReviewJobService.start()` 가
   `TransactionSynchronization.afterCommit` 으로 `runAsync` 를 던진다. 트랜잭션 안에서 던지면
   파이프라인 스레드가 아직 커밋되지 않은 Job 을 조회해 그냥 건너뛴다.

2. **오케스트레이션은 트랜잭션을 열지 않는다.** 상태 전이와 저장은 `ReviewPipelineStore` 의
   짧은 트랜잭션으로 나눴다. AI 호출(S5b)이 DB 커넥션을 붙잡지 않게 하기 위함이다.

3. **`startRunning` 은 `PENDING` 인 Job 만 착수한다.** 중복 실행이 들어와도 두 번째는
   상태 필터에서 걸러진다. `PENDING → RUNNING` 은 같은 행의 UPDATE 라
   `ux_review_jobs_active` 부분 유니크 인덱스를 위반하지 않는다.

4. **ruleset 버전은 활성 규칙의 최대 버전 문자열로 결정한다.** 버전 이름이 날짜 기반
   (`ruleset-2026.09.01`)이라 문자열 순서가 시간 순서와 일치한다. 활성 규칙이 없으면
   `review.ruleset.fallback-version`(기본 `ruleset-empty`)을 쓴다 —
   Job 의 `ruleset_version` 을 NULL 로 남기지 않는다.

5. **검산기 1종만 구현했다: `NUMERIC_CONSISTENCY`.**
   같은 대상을 가리키는 수치가 문서 안에서 서로 다른 경우를 지적한다.
   주제어는 `rawText` 에서 수량 토큰만 걷어내 만든다. **숫자를 통째로 지우면 연도까지 사라져
   서로 다른 해의 수치가 한 묶음이 되므로**, 시점 표기(년·월·일·분기·주·차)가 붙은 숫자 토큰은
   주제어로 남긴다. 단위가 다르면 별개 항목으로 본다. 차이가 규칙의 `tolerance` 이하면 지적하지 않는다.

6. **다른 규칙(`REVENUE_SUM` · `LABOR_COST`)은 검산기를 만들지 못했다. ← 개발자2 의존, 팀 확인 필요**
   `DocumentStructurePort.ElementView(id, pageNo, kind, rawText, numericValue, unit, bbox)` 에는
   **대상 식별자(label/subject)도 표 구조(행·열)도 없다.** 합계 검산(`sum(parts) = total`)은
   어느 요소가 부분이고 어느 요소가 합계인지 알아야 하는데 그 정보가 Port 에 없다.
   `NUMERIC_CONSISTENCY` 는 `rawText` 정규화라는 휴리스틱으로 우회했지만 합계 검산은 우회가 안 된다.
   → **개발자2와 협의 필요**: `ExtractedElement` 에 대상 식별자(예: `label` 또는 `subject_key`)와
     표 좌표(`table_id`·`row`·`col`)를 추가할지. 추가되면 `Port ②` 시그니처 변경이므로 팀 합의 사항이다.
     그때 정규화 휴리스틱도 그 값으로 교체한다.

7. **인용문 검증은 두 갈래다** (D-5 · D-10 §8.10).
   원문 텍스트(`pages.text_layer`)가 있으면 공백을 무시해 대조하고 불일치하면 폐기한다.
   원문 텍스트가 없는 페이지(이미지형 표 등)는
   - 결정적 검산 결과는 **남긴다** — 인용문이 추출 요소의 `rawText` 이므로 원문에서 온 값이다
   - AI 결과는 **폐기한다** — 검증할 수 없는 AI 응답을 신뢰 데이터처럼 저장하지 않는다
   근거가 없거나 인용문이 빈 초안은 무조건 폐기한다.

8. **실패 시 `error_code = "PIPELINE_ERROR"`.** ErrorCode enum 에 추가하지 않았다 —
   이 값은 HTTP 응답 코드가 아니라 `review_jobs.error_code` 컬럼에 남기는 진단 문자열이다.

9. **스텁 구조 Port 에 표본 데이터를 넣었다** (`review.stub.sample-structure=true`).
   기본은 여전히 빈 리스트다(E-2). 표본의 `rawText` 는 같은 페이지 `textLayer` 안에
   그대로 들어 있어 인용문 검증까지 통과한다. 개발자2 구현이 오면 이 스텁은 삭제한다.

**검증**

- `./gradlew test` — 99건 통과, 실패 0
- `ddl-auto=validate` 통과 — `ValidationRule` 매핑이 스키마와 일치
- 실제 Postgres 대상 E2E (`--review.stub.sample-structure=true`):

  ```
  POST /api/review-jobs → 202
    {'id': '23', 'status': 'PENDING', 'rulesetVersion': None,
     'startedAt': None, 'finishedAt': None, 'summary': None}     ← D-2 대로 전부 null

  폴링 1회 만에 status=DONE terminal=True

  파이프라인 로그:
    파이프라인 완료. jobId=23 ruleset=ruleset-2026.09.01 요소=3 초안=1 저장=1

  GET /api/review-jobs/23
    status=DONE · terminal=true · rulesetVersion="ruleset-2026.09.01"
    startedAt/finishedAt 기록 · summary {total:1, ERROR:1, open:1}

  GET /api/review-jobs/23/findings
    1건 · severity=ERROR · method=DETERMINISTIC · confidence=0.99
    calculation {expression, expected:"18억 원", actual:"24억 원", diff:"6억 원"}
    evidence 2건 — p.9 / p.11, 각각 id·quote·bbox(0~1) 있음
    quote 는 원문 그대로: "2027년 예상 매출 18억 원" / "2027년 예상 매출 24억 원"
  ```

- **오탐 없음 확인**: 표본에 `2026년 예상 매출 9.6억 원` 을 섞었지만 지적 대상에서 빠졌다
  (다른 해는 다른 항목)
- **`finding_elements` 연결**: `finding_id=26 → element_id 901, 902`
- **`ruleset_version` 스냅샷**: `review_jobs.ruleset_version = ruleset-2026.09.01`
- **실패 경로**: 구조 조회가 예외를 던지면 `markFailed(jobId, "PIPELINE_ERROR")`, `markDone` 미호출 (단위 테스트)
- **중복 실행**: `PENDING` 이 아닌 Job 은 저장·상태 변경 없이 건너뜀 (단위 테스트)

**후속 과제**
- `REVENUE_SUM` · `LABOR_COST` 검산기 — `ExtractedElement` 스키마 확정 후 (위 6번)
- S5b: AI 서버 연동 클라이언트 + JSON Schema 검증 + RAG 판단

---

## S5b — AI 서버 연동

- 2026-09-03
- 구현 범위: AI 클라이언트 · 응답 스키마 검증 · RAG 판단 · 인용문 대조 폐기.

**추가**
- `ai/ReviewAiClient.java` — 인터페이스
- `ai/ReviewAiProperties.java` — `@ConfigurationProperties(prefix = "review.ai")`
- `ai/ReviewAiRequest.java` · `ReviewAiResponse.java` — 명세 8-2 · 8-3 기준
- `ai/HttpReviewAiClient.java` — `RestClient`, timeout 설정
- `ai/StubReviewAiClient.java` — `@Primary @Profile("stub")`
- `ai/ReviewAiResponseValidator.java` — 스키마 검증
- `ai/ReviewAiException.java`
- `review/pipeline/AiReviewStage.java` — D-10 4·5단계
- `docs/ai/schema-finding.json` — AI 팀과 공유할 응답 스키마
- 테스트 20건: `ReviewAiResponseValidatorTest`(11) · `AiReviewStageTest`(7) · `ReviewPipelineTest`(2)

**변경**
- `review/pipeline/ReviewPipeline.java` — AI 단계 연결, `ownerId` 전달, AI 실패 시 그 `error_code` 사용
- `review/job/ReviewJobService.java` — 파이프라인에 `ownerId` 전달
- `review/support/ReviewPersistenceConfig.java` — `@EnableConfigurationProperties`
- `src/main/resources/application.properties` — AI 설정 전량

**결정 · 편차**

1. **`review.ai.enabled` 기본값이 `false` 다.** AI 서버 없이도 나머지 파이프라인이 돌아야 한다.
   `false` 면 4·5단계를 건너뛰고 결정적 검산 결과만 저장한다.
   `true` 인데 호출이나 스키마 검증이 실패하면 **Job 을 `FAILED`** 로 남긴다 (D-10 §8.10 문자 그대로).
   부분 성공을 표현하려면 `PARTIAL` 상태가 필요한데 미결 #9 에서 추가하지 않기로 했다.

2. **`error_code` 를 실패 원인별로 구분한다**: `AI_CALL_FAILED`(타임아웃·HTTP 오류) ·
   `AI_RESPONSE_INVALID`(빈 본문·`findings` 누락) · `AI_CLIENT_MISSING`(설정 불일치) ·
   `PIPELINE_ERROR`(그 외). `ErrorCode` enum 에는 추가하지 않았다 — HTTP 응답 코드가 아니라
   `review_jobs.error_code` 컬럼용 진단 문자열이다.

3. **AI 응답의 `type` 을 `severity` 로 바꿨고 값은 `ERROR` · `WARNING` · `INFO` 다.**
   구 명세 8-3 은 `type: ERROR|NEEDS_CHECK|NO_EVIDENCE` 였지만 DEV3 F절이 SEVERITY 3종으로 바꿨다.
   **← AI 팀과 확정 필요.** `docs/ai/schema-finding.json` 에 이 계약을 적어 뒀다.

4. **JSON Schema 검증을 라이브러리 없이 코드로 강제했다.** A-8 이 의존성 추가를 제한하므로
   검증 라이브러리를 넣지 않고 `ReviewAiResponseValidator` 가 같은 규칙을 검사한다.
   스키마 문서(`docs/ai/schema-finding.json`)와 코드가 같은 규칙을 표현한다.
   - 응답 전체가 규격을 벗어나면(`findings` 누락) **전체 거부** → Job `FAILED`
   - 개별 항목이 규격을 벗어나면 **그 항목만 버리고** 나머지는 저장
   - 검사 항목: `severity` enum · `title` 비어 있지 않음 · `confidence` 0~1 ·
     `evidence` 1건 이상 · 각 `evidence` 의 `page ≥ 1` 과 `quote` 비어 있지 않음

5. **bbox 는 AI 응답에서 받지 않는다.** `evidence[].elementId` 로 파싱 결과의 요소를 되짚어
   그 좌표를 쓴다. **좌표의 출처를 파싱 결과로 한정해 AI 가 좌표를 지어내지 못하게 한다** (D-5).
   모르는 `elementId` 면 좌표 없이 `quote` 만 남는다 — FE 가 텍스트 재탐색으로 복원한다.

6. **AI 항목은 `rule_id` 를 채우지 않아 항상 `method = RAG` 로 파생되고 `calculation` 은 `null` 이다** (D-4).
   AI 가 `method` 나 `calculation` 을 보내도 무시한다.

7. **API Key 보안** (D-10 §8.10)
   - 설정은 `review.ai.api-key=${REVIEW_AI_API_KEY:}` — 환경변수만 받는다. 파일에 값이 없다.
   - `ReviewAiProperties.toString()` 을 직접 재정의해 키를 `***` 로 가린다.
     record 기본 `toString` 은 모든 컴포넌트를 그대로 찍기 때문에 그대로 두면 설정을 로그에 찍는
     순간 키가 새어 나간다. 단위 테스트로 고정했다.
   - `RestClientException` 을 그대로 감싸지 않고 예외 클래스명만 담은 메시지를 새로 만든다 —
     요청 본문이나 헤더가 로그에 섞이지 않게.
   - 모델명 · Temperature · promptVersion · timeout 전부 설정으로 분리.

8. **`HttpReviewAiClient` 에 `@Profile("!stub")` 을 붙였다.** `stub` 프로파일에서도 이 빈이
   같이 만들어져 `base-url` 부재로 기동이 깨졌다. `base-url` 없이 `enabled=true` 면
   기동 시점에 실패하는 것(fail-fast)은 의도한 동작이므로 유지하고, 스텁 프로파일에서만 제외했다.

9. **`ownerId` 를 파이프라인까지 넘긴다.** `AiReviewStage` 가 `documentTitle` 을 보내려면
   `DocumentQueryPort` 를 읽어야 하는데, 이 Port 는 **소유자 기준으로만** 조회를 허용한다(A-5 Port ①).
   백그라운드 스레드에 사용자 컨텍스트가 없어 처음엔 `userId = null` 로 호출했는데,
   개발자2 실구현에서는 항상 `Optional.empty()` 가 되어 제목이 조용히 `null` 이 된다.
   Job 을 시작한 사용자를 `runAsync(jobId, ownerId)` 로 넘겨 계약을 지키도록 고쳤다.

10. **`docs/ai/prompt-review.md` · `prompt-chat.md` 는 만들지 않았다** (명세 8-4 산출물).
    프롬프트 작성은 AI 담당 영역이다. 응답 스키마만 계약으로 고정했다.

**검증**

- `./gradlew test` — 119건 통과, 실패 0
- 하드코딩된 키·시크릿 스캔: 없음. 설정 로그는 마스킹된 `toString` 을 쓴다
- 실제 Postgres + 스텁 AI 클라이언트 E2E (`REVIEW_AI_ENABLED=true`, `--review.stub.sample-structure=true`):

  ```
  파이프라인 로그:
    인용문이 원문과 불일치해 검토사항을 폐기한다. page=9 title=폐기되어야 하는 항목
    파이프라인 완료. jobId=25 ruleset=ruleset-2026.09.01 요소=3 결정적=1/1 AI=1/2 저장=2

  GET /api/review-jobs/25/findings → 2건
    ERROR   DETERMINISTIC  calc=있음  ev=2  같은 항목의 수치가 서로 다릅니다
        p.9  bbox=있음  quote='2027년 예상 매출 18억 원'
        p.11 bbox=있음  quote='2027년 예상 매출 24억 원'
    WARNING RAG            calc=null  ev=1  근거 문서와 목표치의 연결이 확인되지 않습니다
        p.9  bbox=있음  quote='2027년 예상 매출 18억 원'

  폐기되어야 하는 항목이 남아 있는가: False
  RAG 항목의 calculation 이 모두 null: True
  모든 항목에 evidence 1건 이상: True

  summary {total:2, ERROR:1, WARNING:1, INFO:0, open:2}
  rulesetVersion = ruleset-2026.09.01 · errorCode = None
  ```

  스텁 AI 클라이언트는 **일부러 원문에 없는 인용문을 하나 섞어** 보낸다.
  그 항목이 폐기되고 나머지만 저장되는 것을 위 로그와 결과가 함께 보여준다.

- AI 실패 경로 (단위 테스트): `AI_CALL_FAILED` 를 던지면 `markFailed(42L, "AI_CALL_FAILED")`,
  `markDone` 과 `saveFindings` 는 호출되지 않는다
- 클라이언트 빈 부재: `AI_CLIENT_MISSING`
- 비활성 시: AI 호출 없음, 빈 목록

**후속 과제**
- AI 응답의 `severity` 이름·값 AI 팀 확정 (위 3번)
- AI 서버 엔드포인트·인증 방식 확보 후 `REVIEW_AI_BASE_URL`·`REVIEW_AI_API_KEY` 설정
- `docs/ai/prompt-review.md` — AI 담당

---

## 완료 기준 (DEV3 H절) 자체 검증

실행 중인 애플리케이션에 실제 요청을 보내 34개 항목을 확인했다. 스크립트는 아래 조건으로 돌렸다.

```bash
docker compose up -d postgres            # 로컬 5432 가 점유돼 있으면 POSTGRES_PORT=5433
cd BE/business
REVIEW_AI_ENABLED=true DB_PORT=5433 ./gradlew bootRun \
  --args='--spring.profiles.active=stub,seed --review.stub.sample-structure=true'
```

| 항목 | 결과 |
|---|---|
| 11개 엔드포인트(MVP1) Method·Path·Status Code 가 명세와 일치 | PASS |
| 16번이 202 + `Location` 헤더 반환, 파이프라인을 기다리지 않음 | PASS |
| 응답 시점의 `startedAt`·`finishedAt`·`rulesetVersion` 이 null | PASS |
| 16번 선행 조건: 파싱 미완료 409 `DOCUMENT_NOT_READY` | PASS (S2 검증) |
| 16번 선행 조건: 중복 실행 409 `JOB_ALREADY_RUNNING` | PASS |
| 동시 요청에서 Job 이 1개만 생성됨 (DB 제약 포함) | PASS — 동시 6건 → 202 1건 · 409 5건 · DB 1행 |
| `terminal` 이 `status ∈ {DONE, FAILED}` 에만 `true` | PASS |
| `summary` 가 `status = DONE` 일 때만 채워지고 그 외 `null` | PASS |
| `documentTitle`·`pageCount` 를 `DocumentQueryPort` 로 조합 | PASS — `documents` 직접 조회 없음 |
| 21번이 Query 파라미터를 받지 않고 전체 반환 | PASS — `?severity=&sort=` 를 붙여도 같은 결과 |
| 분석 미완료 Job 의 findings 가 빈 배열 | PASS |
| `method` 가 `rule_id` 유무로 파생됨 | PASS — DETERMINISTIC·RAG 둘 다 확인 |
| `calculation` 이 DETERMINISTIC 일 때만 채워짐 | PASS |
| 모든 Finding 에 `evidence` 가 1건 이상, 각 evidence 에 `id`·`quote` 존재 | PASS |
| `bbox` 가 0~1 상대 좌표 | PASS |
| `embedding` 이 응답에 없음 | PASS — 엔티티에 매핑 자체가 없다 |
| 판정 시 `finding_decisions` 에 이력이 쌓임 | PASS |
| 완료된 Job 의 Finding 판정 시 409 `JOB_ALREADY_COMPLETED` | PASS |
| 19번이 `status`·`parse_status` 를 변경하지 않음 | PASS — 완료 전후 `status = DONE` 불변 |
| 주석에 `source`·`color` 필드가 없음 | PASS |
| `author_id` 를 요청에서 받지 않음 | PASS — `authorId: "999"` 주입 시도 무시 |
| 주석 soft delete 후 목록에서 제외됨 | PASS — DB 에는 `deleted_at` 채운 행이 남음 |
| AI API Key 가 코드·로그에 없음 | PASS — 환경변수만, `toString` 마스킹, 하드코딩 스캔 무결과 |
| 타인 리소스 접근 시 403 | PASS (단위 테스트 — 스텁 Port 가 항상 소유자를 반환해 E2E 로는 재현 불가) |
| `id` 필드가 문자열, 날짜에 `+09:00` | PASS |
| 응답을 래퍼로 감싸지 않음 | PASS |
| 통합 시 스텁 파일 삭제 완료 | 미해당 — 개발자1·2 산출물 머지 시 수행 (위 통합 절차 참고) |

**34/34 PASS.**

---

## 현재 상태 요약

| 구분 | 상태 |
|---|---|
| MVP1 담당 API 11건 (16~19, 21~27) | 구현 · 검증 완료 |
| 20번 검토 결과 PDF 내보내기 (MVP2) | 범위 밖 — PDF 라이브러리 의존성과 미결 #11 확정 후 |
| `ReviewJobQueryPort` (개발자2 제공분) | 인터페이스 + 구현 완료. 개발자2 통합 대기 |
| AI 파이프라인 | 결정적 검산 1종 + AI 연동 완료. AI 서버 엔드포인트 확보 대기 |
| 테스트 | 119건 통과 |
| 커밋 | `backend/pkt_init` 에 7건, `origin` push 완료 |

### 팀에 확인이 필요한 것

1. **응답 필드 3개** — DEV3 문서가 응답 필드를 전량 열거하지 않아 판단으로 넣거나 뺐다. v2 명세 대조 필요.
   - 17번 `errorCode` — 넣음 (실패 사유 표시용)
   - 21·22번 `jobId` — 넣음 (22번이 findingId 만 받아 소속 Job 을 알 수 없음)
   - 21·22번 `decidedAt` — 뺌 (컬럼은 있음)
2. **삭제된 문서 접근 시 404 vs 403** — `DocumentQueryPort.findMetaForOwner` 가 두 경우를
   구분하지 못해 403 으로 통일했다. 구분이 필요하면 Port 시그니처 변경 → 개발자2 협의
3. **검토 완료 후 주석 잠금 여부** — D-6 은 판정만 잠근다. 문서 문자 그대로 주석은 허용했다
4. **`ExtractedElement` 에 대상 식별자·표 좌표 추가 여부** — `REVENUE_SUM`·`LABOR_COST` 같은
   합계 검산은 현재 Port ② 시그니처로 표현할 수 없다 → 개발자2 협의 (S5a 6번)
5. **AI 응답의 `severity` 이름·값** — 구 명세의 `type`(ERROR/NEEDS_CHECK/NO_EVIDENCE) 에서
   DEV3 F절의 `severity`(ERROR/WARNING/INFO) 로 바꿨다 → AI 팀 확정
6. **23번 응답에 생성된 주석 id 포함 여부** — 현재는 갱신된 Finding 만 반환한다
7. **미결 #4 원문 뷰어 렌더링 방식** — `finding_evidence.bbox` 좌표계 기준에 영향 → 개발자2 협의
8. **FE 변경 (DEV3 F절 6개 파일)** — 프론트 담당자 몫. 현재 FE 는 `VITE_USE_MOCK=true` 이고
   `endpoints.js` 가 구 명세(`PATCH .../verdict`, annotations 없음)를 향한다
