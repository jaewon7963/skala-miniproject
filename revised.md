# 코드 점검과 수정 기록

현재 코드를 여섯 가지 기준으로 점검하고, 거기서 나온 결함을 고친 기록이다.

- 브랜치: `backend/pkt_revised` (`backend/pkt_AiMockUp` 에서 분기)
- 점검 근거: **실제 DB 데이터와 실행 중인 서버** — 코드만 읽고 판단하지 않았다

---

## 점검 결과 요약

| # | 기준 | 판정 |
|---|---|---|
| 1 | 데이터 정규화 | 3NF 위반 없음. **비정규화 2곳은 정당화가 약해 고침** |
| 2 | Mock API 엔드포인트 완성도 | 화면이 부르는 22개 전부 정상 |
| 3 | FE/BE 프로젝트 구조 | 포트 설계는 좋으나 경계가 6곳 샘 — 보고만 |
| 4 | DB 연동 | 정상 |
| 5 | 데이터 바인딩·화면 시연 | 정상 |
| 6 | 수정분 동작 | 정상 (테스트 46개 통과 + 실문서 3건 재검증) |

**고친 것은 목차·요약 품질 4건과 정규화 2건이다.** 아래에 왜 고쳤는지를 근거와 함께 남긴다.

---

# 1. 목차가 읽을 수 있는 형태로 나오지 않았다

## 1-1. 목차가 373개로 폭증했다

### 왜 고쳤나

실제 DB 를 보니 28쪽짜리 발표자료의 목차가 **373개**였다.

```
document_id | sections | distinct_titles
          2 |      373 |             255
```

목차 응답이 **31KB** 로 원문(43KB)에 육박했다. 사이드바를 열면 373줄이 쏟아진다.
게다가 한 문장이 두 항목으로 쪼개져 있었다.

```
0 | lv1 | p1 | 사업계획서의 숫자는
1 | lv1 | p1 | 정말 맞습니까?
```

원인은 `DocumentParseStatusUpdater.java:63` 의 분기였다.

```java
List<ParsedDocument.ParsedSection> outline = parsed.sections().isEmpty()
        ? deriveSections(blocksByPage)   // MAX_DERIVED_SECTIONS = 40 상한 있음
        : parsed.sections();             // ← 상한 없음
```

PDF 북마크가 있으면 그대로 쓰는데, `PdfStructureExtractor.walk` 가 중첩 북마크를
**무제한 재귀**로 펼친다. 슬라이드를 PDF 로 내보내면 텍스트 상자마다 북마크가 생긴다.
상한은 추정 경로에만 걸려 있었다.

레벨 분포도 문제였다. 화면 목차는 `is-level-2` 까지만 들여쓰기 스타일을 갖고 있는데
(`SectionOutline.vue:89`) 실제로는 레벨 5까지 나왔다.

```
lv1 26 · lv2 98 · lv3 137 · lv4 85 · lv5 27
```

### 무엇을 고쳤나

- `PdfStructureExtractor` 에 `MAX_OUTLINE_LEVEL = 2` — 화면이 구분할 수 있는 깊이까지만 받는다
- 상한(`MAX_SECTIONS = 40`)을 **두 경로에 공통** 적용
- 빈 제목은 버린다

### 결과

**373개 → 40개**, 목차 응답 **31KB → 3.8KB**. 레벨도 2까지로 정리됐다.

## 1-2. 쪽마다 찍히는 머리글이 목차가 됐다

### 왜 고쳤나

12쪽 보고서의 목차 12개 중 **11개가 같은 제목**이었다.

```
0 | p1  | SKALA
1 | p2  | sLLM_FineTuning 서브 노트 1 SKALA
2 | p3  | sLLM_FineTuning 서브 노트 1 SKALA
...  p12 까지 동일
```

`deriveSections` 가 페이지마다 첫 제목 블록을 하나씩 올리는데, 모든 페이지 상단에
반복되는 머리글이 매번 첫 제목으로 잡혔다. 목차로서 아무 정보도 주지 못한다.

### 무엇을 고쳤나

직전 항목과 제목이 같으면 새로 만들지 않는다. 다만 **사이에 다른 항목이 끼었다가
다시 나온 제목은 남긴다** — 그건 머리글이 아니라 실제로 다시 등장한 절이다.

### 결과

**12개 → 2개.**

## 1-3. 목차 계층이 죽어 있었다

### 왜 고쳤나

`sections.parent_id` 가 **386개 행 전부 NULL** 이었다. `DocumentParseStatusUpdater.java:69`
가 부모 자리에 리터럴 `null` 을 넘기고 있었다.

그런데 `DocumentQueryService.buildTree()`(`:200-220`)는 이 컬럼을 걸어 트리를 조립한다.
**트리를 만드는 코드는 있는데 입력이 없는 상태**였다.

### 무엇을 고쳤나

앞쪽에서 가장 가까운, 더 얕은 레벨의 항목을 부모로 잇는다.

### 결과

문서2의 40개 중 **32개가 하위 항목으로 연결**됐다. 화면에서 들여쓰기가 살아난다.

## 1-4. 파생 목차가 출처 구분 없이 저장됐다

### 왜 고쳤나

`Section.Source.EXTRACTED` 를 쓰는 코드가 **0곳**이었다. DB 의 386개 섹션이 전부
`ORIGINAL` 이라, 북마크에서 온 것과 우리가 추정한 것을 구분할 수 없었다.
`idx_sections_document_source_order (document_id, source, order_no)` 도 상수 컬럼
인덱스가 되어 있었다.

### 무엇을 고쳤나

북마크 경로는 `ORIGINAL`, 본문 추정 경로는 `EXTRACTED`.

### 결과

문서2 `ORIGINAL`(북마크 있음), 문서1·3 `EXTRACTED`(추정)로 정확히 갈렸다.

## 1-5. 요약이 문장이 아닌 것을 집었다

### 왜 고쳤나

실제 DB 의 요약 3건 중 2건이 쓸모없었다.

| 문서 | 고치기 전 |
|---|---|
| 1 | `AI 매장 안내 로봇을 개발해 무인 매장에 공급하는 사업이다.` (정상) |
| 2 | `Problem Insight Solution Design Demo Next` ← 목차 네비게이션 라벨 |
| 3 | `제  출  일 2026. 08. 13.` ← 표지의 제출일 칸 |

`summarize()` 가 "10자 넘는 첫 `p` 블록"을 그대로 쓰고 있었다.

### 무엇을 고쳤나

문장으로 읽히는 것만 고른다 — 20자 이상이고, 한국어 종결어미(다/요/음/임)로 끝나거나
마침표로 끝나되 **그 앞이 글자**여야 한다. 마지막 조건이 날짜를 걸러낸다
(`... 13.` 은 마침표 앞이 숫자다). 마땅한 문장이 없으면 비워 둔다 — 화면은 요약이
없어도 그린다.

### 결과

| 문서 | 고친 뒤 |
|---|---|
| 2 | `숫자가 서로 맞지 않는다 같은 지표가 문서 전체에서 유지되지 않는다 …` |
| 3 | `과적합 트레이닝은 매개변수와 연관된다. 에폭값을 너무 과하게 주지 말고 …` |

## 왜 별도 클래스로 뺐나

목차 규칙은 문서마다 제일 자주 어긋나는 부분인데, 저장 코드에 붙어 있어서 검증하려면
PostgreSQL 컨테이너가 필요했다. **`DocumentOutline` 으로 분리해 DB 없이 문서 모양만
바꿔가며 검증**할 수 있게 했다. 위 다섯 가지 사례가 전부 단위 테스트로 고정돼 있다
(`DocumentOutlineTest`, 7건).

---

# 2. 정규화가 뒤로 가 있던 두 곳

## 2-1. 검산 근거를 숫자로 되돌렸다

### 왜 고쳤나

`baseline` 은 원자 컬럼 세 개였다 (`V20260904_0900__baseline.sql:119-121`).

```sql
calc_expr TEXT, expected_value NUMERIC, actual_value NUMERIC
```

이걸 JSONB 한 컬럼으로 합치면서 **값을 `"36.8억"` 같은 표시용 문자열로 바꿨다.**
두 가지가 잘못됐다.

1. 숫자가 아니라 **SQL 로 비교하거나 집계할 수 없다.** "차이가 1억 넘는 오류만" 같은
   질의가 불가능하다
2. 한 컬럼에 식·기댓값·실제값·차이 **네 속성**이 들어갔다

### 무엇을 고쳤나

`V20260904_1200__atomic_calculation.sql` 로 `calc_expression TEXT` ·
`calc_expected NUMERIC` · `calc_actual NUMERIC` 를 만들고 JSONB 를 뺐다.
차이는 두 값의 뺄셈이라 **저장하지 않고 조회할 때 만든다.**
사람이 읽는 표기도 응답에서 만들므로 저장 값은 순수한 숫자로 남는다.

### 결과

```
 id |       식        |   기댓값   |   실제값
  1 | 36.8억 + 21.3억 | 5810000000 | 5000000000
```

화면이 받는 모양은 그대로다.

```json
{"expression": "36.8억 + 21.3억", "expected": "58.1억", "actual": "50억", "diff": "8.1억"}
```

## 2-2. 진행 단계에서 불변 리터럴을 뺐다

### 왜 고쳤나

`JobStep(key, label, detail, state)` 중 `key`·`label`·`detail` 은
`ReviewJobSteps.java` 에 한 번 정의된 **고정 문자열**인데, 작업 행마다 다섯 벌씩
통째로 저장되고 있었다. 실제로 변하는 건 `state` 뿐이다.

### 무엇을 고쳤나

`state` 배열만 저장하고 이름과 문구는 응답을 만들 때 붙인다. 기존 행은 마이그레이션에서
변환한다.

```sql
UPDATE review_jobs SET steps = (
    SELECT jsonb_agg(elem ->> 'state' ORDER BY ord)
    FROM jsonb_array_elements(steps) WITH ORDINALITY AS t(elem, ord)) …
```

### 결과

```
1 → ["DONE", "DONE", "DONE", "DONE", "DONE"]
```

화면이 받는 모양은 그대로다. 상태가 모자라거나 비어도 대기 상태로 채운다 — 화면이
`state.toLowerCase()` 를 부르기 때문에 null 이 섞이면 그 자리에서 죽는다
(`JobProgressView.vue:72`).

---

# 3. 고치지 않은 것과 그 이유

## 3-1. 그대로 두는 비정규화

| 대상 | 이유 |
|---|---|
| `pages.blocks` (JSONB) | 블록을 테이블로 쪼개면 28쪽 문서에 620행이 생긴다. 원문 뷰어는 **항상 페이지 통째로** 읽어서 쪼갤 이득이 없고 조회만 느려진다 |
| `documents.page_count` | `COUNT(pages)` 로 계산 가능하지만 목록 화면이 문서마다 세게 된다. 파싱 완료 시 1회 확정되고 이후 안 바뀐다. **실데이터 3/3 일치 확인** |
| `findings.page_no` | 첫 근거의 `page_no` 와 항상 같다. 정렬·필터가 근거 조인 없이 읽어야 한다. **실데이터 15/15 일치 확인** |
| `review_jobs.parse_duration_ms` · `analyze_duration_ms` | 설정값을 행마다 복사한 것이지만, 설정이 바뀌어도 **과거 작업의 진행률을 그대로 재현**하려면 필요하다. 스냅샷으로 본다 |

## 3-2. FK 를 걸 수 없는 참조

`finding_evidence.anchor_id` 와 `annotations.anchor_id` 는 `pages.blocks` JSONB
**배열 안의** `id` 를 가리킨다. 대상이 행이 아니라 JSON 문서 안의 값이라 FK 가
구조적으로 불가능하다. 같은 이유로 **다른 문서의 블록을 가리켜도 DB 가 막지 못한다.**

대신 통합 테스트가 모든 검토 항목의 근거가 실제 원문 블록을 가리키는지 검증한다
(`ReviewJobFlowIntegrationTest.assertEveryFindingAnchorsIntoTheDocument`).
재분석 후 실데이터 **10/10 유효**를 SQL 로 다시 확인했다.

## 3-3. 빈 테이블 4개

`extracted_elements` · `validation_rules` · `annotations` · `finding_decisions` 는
코드가 읽지도 쓰지도 않는다. 팀 ERD 유지 목적이고, `DEV3.md` A-6 이 머지된
마이그레이션 수정을 금지하므로 남긴다.

## 3-4. 구조 경계가 새는 6곳

`document ⇄ review` 는 **양방향 모두 포트를 거친다.** 그 외에 여섯 군데가 샌다.
동작에 지장이 없고 파일을 많이 건드려야 해서 이번 범위에서 뺐다.

**내가 만든 것**

| 새는 곳 | 성격 |
|---|---|
| `ReviewJobService` → `AuthService` 직접 호출 (`:199`, 검토자 이메일 한 줄) | 포트를 안 거친 유일한 도메인 간 서비스 의존 |
| `review`·`ai` 가 `document.entity.PageBlock` 직접 import, 포트 계약(`PageContentView`)도 이 타입을 노출 | 값 레코드라 실해는 없지만 엔티티가 경계를 넘는 것처럼 보인다. `document.port` 로 옮기면 해결 |
| `ReviewJobController` 가 `@RequestMapping("/api")` 로 잡고 `/api/documents/{id}/review-jobs/latest` 까지 소유 | prefix 가 패키지와 안 맞는 유일한 컨트롤러 |
| `auth.dto.SignupRequest` vs `user.dto.SignUpRequest` — **대문자 U 하나 차이** | 둘 다 회원가입 페이로드. 사고 나기 쉽다 |
| `review`·`ai` 만 레이어 구분 없는 평면 패키지 (21개·7개 파일) | 다른 도메인은 controller/service/repository/entity/dto 로 나뉜다 |

**팀원 영역** — `TagController` 가 리포지토리 직접 호출 · `auth ⇄ user` 순환 의존 ·
`document.service` 가 `TagRepository` 직접 사용 · 메인 클래스가 `com.example.business` 에
홀로 있어 모든 `@SpringBootTest` 가 `classes = BusinessApplication.class` 를 명시해야 함.

## 3-5. 프런트 9곳이 스토어를 우회

`UploadView.vue:32,39,61,84` · `ReviewView.vue:86` · `ReviewDoneView.vue:14` ·
`ReportView.vue:34` · `AskPanel.vue:34,44` 가 스토어를 건너뛰고 `api/*` 를 직접 부른다.
특히 `AskPanel.vue:44` 는 이미 있는 `useReviewStore.addFinding`(`stores/review.js:225`)과
같은 호출을 중복한다. **프런트 수정 금지**라 손대지 않는다.

## 3-6. `documents.version` 이 항상 1

`bumpVersion()` 호출부가 0곳인데 응답에는 실려 나간다. 재분석할 때 올리는 게 맞지만
화면이 이 값을 쓰지 않아 별건으로 남긴다.

---

# 4. 확인 방법

## 목차·요약

```sql
select document_id, count(*) 목차, count(distinct title) 서로다른제목,
       count(*) filter (where parent_id is not null) 하위항목,
       max(level) 최대레벨, string_agg(distinct source, ',') 출처
from sections group by 1 order by 1;

select id, summary from documents order by id;
```

재분석 후 실제 결과:

| 문서 | 목차 | 서로다른제목 | 하위항목 | 최대레벨 | 출처 |
|---|---|---|---|---|---|
| 1 (1쪽) | 1 | 1 | 0 | 1 | EXTRACTED |
| 2 (28쪽) | **40** (전 373) | 40 | 32 | 2 | ORIGINAL |
| 3 (12쪽) | **2** (전 12) | 2 | 0 | 1 | EXTRACTED |

## 검산 근거·진행 단계

```sql
select id, calc_expression, calc_expected, calc_actual from findings where calc_expression is not null;
select id, steps from review_jobs order by id;
```

## 근거 앵커 무결성

```sql
select count(*) filter (where ok) || ' / ' || count(*) from (
  select exists (select 1 from pages p, jsonb_array_elements(p.blocks) b
                 where p.document_id = j.document_id and b->>'id' = e.anchor_id) as ok
  from finding_evidence e
  join findings f on f.id = e.finding_id
  join review_jobs j on j.id = f.job_id) t;
```

→ **10 / 10**

## 테스트와 화면

```bash
./gradlew test        # 46개 통과 (기존 36 + 신규 10)
```

화면이 부르는 **22개 엔드포인트 전수 확인**, 브라우저로 검토 화면을 열어 목차
사이드바가 계층 들여쓰기와 함께 읽히는지 눈으로 확인했다.
