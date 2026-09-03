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
