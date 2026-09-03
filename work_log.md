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
