# BizXray Frontend Features & Technologies

## 1. 문서 목적

이 문서는 BizXray 프런트엔드에 실제 구현된 기능과 각 기능을 구성하는 기술을 설명한다. 시각 디자인 명세가 아니라 구현 구조, 상태 흐름, 브라우저 API, Vue 사용 방식과 기술적 특징을 중심으로 정리한다.

현재 프런트엔드는 Vue 3 기반 SPA이며, 백엔드가 완성되지 않은 기능도 프런트엔드 Mock API를 통해 전체 사용자 흐름을 확인할 수 있도록 구성되어 있다.

## 2. 핵심 기술 스택

| 분류 | 기술 | 프로젝트에서의 역할 |
|---|---|---|
| UI Framework | Vue 3.5 | 컴포넌트와 반응형 화면 구성 |
| 작성 방식 | Composition API, `<script setup>` | 상태와 화면 로직 구성 |
| Build Tool | Vite 8 | 개발 서버, HMR, 프로덕션 번들 |
| State Management | Pinia 3 | 인증, 문서, 검토, UI 전역 상태 관리 |
| Routing | Vue Router 5 | SPA 라우팅, 인증 가드, 화면 지연 로딩 |
| HTTP | Fetch API | JSON 기반 API 요청 |
| Upload | XMLHttpRequest | 파일 업로드 진행률 측정 |
| Persistence | Local Storage | 인증 상태, 테마, 레이아웃, 주석 저장 |
| Text Selection | Selection API, Range API | 드래그한 텍스트와 위치 추출 |
| DOM Navigation | `nextTick`, `scrollIntoView` | 원문 앵커로 이동 |
| Pointer Input | Pointer Events | 사이드바 크기 조절 |
| Trackpad Input | Wheel/Gesture Events | 문서 확대·축소 |
| Styling | Scoped CSS, CSS Variables | 컴포넌트 스타일과 테마 관리 |
| Validation | Custom Validator | 이메일과 비밀번호 검증 |
| Code Quality | ESLint, Oxlint, Prettier | 정적 검사와 코드 포맷 |

## 3. 애플리케이션 초기화

진입점은 `src/main.js`다.

```js
const app = createApp(App)

app.use(createPinia())
app.use(router)
app.mount('#app')
```

앱 시작 시 수행되는 작업은 다음과 같다.

1. 글로벌 CSS 로드
2. 저장된 테마 초기화
3. Vue 애플리케이션 생성
4. Pinia 등록
5. Vue Router 등록
6. `#app`에 애플리케이션 마운트

## 4. Vue 컴포넌트 기술

### 4.1 Composition API

프로젝트의 주요 컴포넌트는 Options API 대신 Composition API로 작성되어 있다.

사용 중인 주요 Vue API는 다음과 같다.

- `ref`: 변경 가능한 상태
- `computed`: 상태에서 계산되는 파생 값
- `watch`: 특정 상태 변경 감지
- `nextTick`: DOM 갱신 이후 작업 수행
- `onMounted`: 화면 진입 후 데이터 로드
- `onUnmounted`, `onBeforeUnmount`: 이벤트와 상태 정리
- `defineProps`: 부모가 전달한 데이터 선언
- `defineEmits`: 자식 컴포넌트 이벤트 선언

### 4.2 Single File Component

각 Vue 컴포넌트는 로직, 템플릿, 스타일을 한 파일에 구성한다.

```vue
<script setup>
</script>

<template>
</template>

<style scoped>
</style>
```

`scoped` CSS를 사용하므로 컴포넌트 스타일이 다른 화면에 직접 전파되지 않는다.

### 4.3 공통 컴포넌트

다음 UI를 재사용 가능한 컴포넌트로 분리했다.

- 버튼
- 입력창
- 셀렉트
- 모달
- 페이지네이션
- 진행률
- 상태 배지
- 검토 유형 배지
- 토스트
- 빈 상태
- 테마 토글
- 헤더
- 브랜드 마크

화면 컴포넌트는 공통 컴포넌트의 Props와 Emits를 사용해 크기, 상태, 이벤트만 전달한다.

## 5. 라우팅과 화면 전환

### 5.1 Vue Router

Vue Router의 History 모드를 사용한다.

```js
createWebHistory(import.meta.env.BASE_URL)
```

구현된 주요 화면은 다음과 같다.

- 랜딩
- 로그인
- 회원가입
- 문서 라이브러리
- 문서 업로드
- 분석 진행 상황
- 검토 워크스페이스
- 검토 완료
- 검토 보고서
- 설정
- 404

### 5.2 Lazy Loading

라우트별 화면은 동적 import로 불러온다.

```js
component: () => import('@/views/review/ReviewView.vue')
```

초기 접속 시 모든 화면 코드를 한 번에 다운로드하지 않고 필요한 화면 번들만 로드한다.

### 5.3 인증 가드

`router.beforeEach`에서 로그인 여부를 확인한다.

- 비로그인 사용자가 보호 화면 접근: 로그인으로 이동
- 로그인 사용자가 로그인·회원가입 접근: 라이브러리로 이동
- 원래 접근하려던 URL: `redirect` 쿼리에 보존

### 5.4 다중 레이아웃

라우트 `meta.layout`을 통해 화면 특성에 맞는 레이아웃을 선택한다.

- Landing Layout
- Auth Layout
- App Layout
- Workspace Layout

검토 화면은 넓은 문서 공간이 필요하므로 일반 앱 화면과 다른 Workspace Layout을 사용한다.

## 6. Pinia 상태 관리

### 6.1 인증 스토어

관리 상태:

- 사용자 이메일
- 인증 토큰
- 로그인 상태
- 인증 요청 로딩 상태

지원 동작:

- 로그인
- 회원가입
- 로그아웃
- 비밀번호 변경
- 회원 탈퇴

### 6.2 문서 스토어

관리 상태:

- 문서 목록
- 태그 목록
- 문서 수
- 상태별 문서 수
- 검색 조건
- 정렬 조건
- 기간 조건
- 페이지네이션
- 로딩과 오류

### 6.3 검토 스토어

관리 상태:

- 검토 작업
- 목차
- 문서 페이지
- 검토 항목
- 사용자 주석
- 현재 페이지
- 문서 확대율
- 선택된 검토 항목
- 검토 유형 필터
- 정렬 조건
- 우측 패널 탭
- 하이라이트 표시 여부
- 좌우 사이드바 너비
- 이동 강조 앵커

검토 화면에서 여러 컴포넌트가 동일한 상태를 사용하므로 Pinia가 핵심적인 역할을 한다.

### 6.4 UI 스토어

토스트 메시지를 중앙 관리한다.

- 성공
- 오류
- 정보

API 성공·실패 결과를 화면별로 직접 그리지 않고 공통 토스트로 전달한다.

## 7. API 계층 구조

프런트엔드는 다음 계층으로 API를 분리한다.

```text
Vue View / Component
        ↓
     Pinia Store
        ↓
    API Module
        ↓
Mock Handler 또는 HTTP Client
        ↓
     Backend API
```

화면이 URL을 직접 사용하지 않으므로 Mock API와 실제 백엔드 API를 쉽게 전환할 수 있다.

### 7.1 환경변수 기반 전환

```env
VITE_USE_MOCK=true
VITE_API_BASE_URL=/api
```

```js
USE_MOCK
  ? mock.reviews.getFindings(jobId)
  : http.get(EP.reviews.findings(jobId))
```

### 7.2 HTTP Client

공통 HTTP Client가 담당하는 기능:

- Base URL 결합
- Query String 생성
- JSON 직렬화
- Authorization Header
- 응답 JSON 파싱
- 상태 코드별 오류 처리
- 네트워크 오류 정규화

## 8. 인증 기능

### 8.1 이메일 기반 회원가입

현재 회원가입에 필요한 정보:

- 업무용 이메일
- 비밀번호
- 필수 약관 동의

이름 필드는 사용자 모델과 관련 화면에서 제거됐다.

### 8.2 입력 검증

이메일과 비밀번호는 프런트에서 먼저 검사한다.

- 이메일 형식 검사
- 비밀번호 8자 이상
- 영문 포함
- 숫자 포함
- 필수 약관 동의

### 8.3 인증 지속성

인증 토큰과 사용자 정보는 Local Storage에 저장된다.

```text
logicheck.token
logicheck.user
```

현재 구조는 데모와 개발에 적합하다. 운영 환경에서는 XSS 위험을 낮추기 위해 HttpOnly Cookie 기반 인증을 고려해야 한다.

## 9. 문서 라이브러리

### 9.1 문서 목록

표시 정보:

- 문서명
- 페이지 수와 파일 크기
- 태그
- 상태
- 최근 수정 시간
- 문서별 옵션 메뉴

### 9.2 검색과 필터

다음 조건을 조합할 수 있다.

- 문서명 검색
- 문서 요약 검색
- 상태 필터
- 기간 필터
- 태그 필터
- 이름순
- 최근 수정순
- 페이지네이션

검색 조건은 Pinia의 단일 `query` 객체로 관리한다.

```js
{
  q,
  status,
  period,
  sort,
  tag,
  page,
  size,
}
```

### 9.3 태그

문서는 복수 태그를 가질 수 있다.

```js
{
  tags: ['government', 'rnd', 'healthcare']
}
```

태그별 문서 수는 Mock API가 문서 데이터를 기준으로 계산한다. 사이드바에서 태그를 클릭하면 해당 태그를 포함하는 문서만 반환한다.

### 9.4 문서 액션

문서별 Ellipsis 메뉴에서 다음 기능을 제공한다.

- 이름 변경
- 검토 요약 내보내기
- 문서 삭제

파싱 실패 문서는 검토 화면으로 이동하지 않고 오류 토스트를 표시한다.

## 10. 문서 업로드

### 10.1 파일 검증

업로드 전에 다음 조건을 확인한다.

- MIME Type이 PDF인지 확인
- 파일 크기 제한 확인

### 10.2 업로드 진행률

실제 API 모드에서는 `XMLHttpRequest`를 사용한다.

```js
xhr.upload.onprogress = (event) => {
  const progress = Math.round((event.loaded / event.total) * 100)
}
```

Fetch API는 일반적으로 업로드 진행률 이벤트를 제공하지 않기 때문에 이 기능만 XHR을 사용한다.

Mock 모드에서는 일정 시간 간격으로 진행률을 증가시켜 실제 업로드와 비슷한 UI를 제공한다.

### 10.3 분석 작업 생성

업로드 후 문서 ID로 검토 작업을 생성하고 진행 화면으로 이동한다.

```text
파일 선택
  → 업로드
  → 문서 생성
  → Review Job 생성
  → 진행 화면 이동
```

## 11. 분석 진행 상황

분석 진행 화면은 Job 상태를 일정 간격으로 조회한다.

```js
while (status !== DONE && status !== FAILED) {
  await delay(...)
  job = await getJob(jobId)
}
```

표시 내용:

- 파싱 진행률
- 분석 진행률
- 단계별 처리 상태
- 부분 실패 페이지
- 발견된 검토 항목 수

작업이 완료되면 검토 화면으로 이동한다.

## 12. 검토 워크스페이스

### 12.1 3분할 레이아웃

검토 화면 구조:

```text
목차 사이드바 | 원문 문서 | 검토 패널 | 아이콘 레일
```

좌측 목차와 우측 패널의 너비는 Pointer Events를 사용해 조절한다.

사용 이벤트:

- `pointerdown`
- `pointermove`
- `pointerup`

변경된 너비는 Local Storage에 저장된다.

### 12.2 우측 아이콘 레일

우측 패널은 다음 탭을 제공한다.

- 검토 결과
- AI 질문
- 주석

탭 상태는 검토 Pinia 스토어에서 관리한다. 원문의 검토 하이라이트나 주석 인덱스를 클릭하면 연결된 탭으로 자동 전환된다.

## 13. 문서 원문 렌더링

현재 문서는 실제 PDF Canvas가 아니라 파싱된 구조화 블록을 Vue 템플릿으로 렌더링한다.

지원 블록:

- 제목
- 문단
- 표 캡션
- 표 헤더
- 표 셀
- 이미지·도형 Placeholder

각 블록은 고유한 Anchor ID를 가진다.

```text
b-11-2
b-7-2--caption
b-7-2--head-0
b-7-2--cell-2-1
```

이 Anchor ID가 검토 항목, 주석, 원문 이동 기능의 기준이 된다.

## 14. 드래그 텍스트 선택

이 프로젝트에서 가장 특징적인 기능 중 하나다.

### 14.1 사용 기술

- Browser Selection API
- Range API
- `getBoundingClientRect`
- DOM `data-*` 속성
- Vue 반응형 상태

### 14.2 동작 흐름

```text
사용자가 텍스트 드래그
  → window.getSelection()
  → Range 추출
  → 시작/종료 Anchor 확인
  → 선택 문자열과 좌표 저장
  → 선택 영역 옆에 + 버튼 표시
```

```js
const browserSelection = window.getSelection()
const range = browserSelection.getRangeAt(0)
const rect = range.getBoundingClientRect()
```

### 14.3 선택 가능 범위

- 일반 문단 일부 또는 전체
- 제목 일부 또는 전체
- 표 캡션
- 표 헤더 셀
- 표 데이터 셀

위치 정확성을 보장하기 위해 서로 다른 블록이나 여러 표 셀을 가로지르는 선택은 등록하지 않는다.

## 15. 사용자 검토 항목 생성

텍스트 선택 후 다음 유형으로 검토 항목을 만들 수 있다.

- 오류
- 확인 필요
- 근거 부족

생성되는 데이터:

```js
{
  type,
  page,
  sectionId,
  title,
  description,
  confidence: 1,
  method: 'MANUAL',
  verdict: 'PENDING',
  evidence: [
    {
      anchorId,
      page,
      label,
      selectedText,
    },
  ],
}
```

생성 후:

- 우측 검토 결과에 추가
- 선택 문자열에 유형별 하이라이트 적용
- 사용자 등록 방식 표시
- 해당 카드 자동 선택
- 검토 결과 탭 자동 전환

Mock 모드에서 수동 검토 항목은 메모리 상태에 저장된다. 브라우저 전체 새로고침 시 Mock 상태가 다시 초기화될 수 있다.

## 16. 선택 문자열 하이라이트

선택 문자열만 하이라이트하기 위해 원문 전체를 단일 텍스트로 출력하지 않고 범위별 Segment로 분해한다.

```text
선택 전 문자열
선택 문자열
선택 후 문자열
```

Vue에서는 Segment 종류에 따라 다른 요소를 렌더링한다.

```vue
<mark v-if="part.kind === 'finding'">
  {{ part.text }}
</mark>
```

유형별 색상:

- 오류: 빨간색
- 확인 필요: 주황색
- 근거 부족: 연한 파란색

판정 상태가 변경돼도 검토 유형의 의미 색상은 유지한다. 단, `오류 아님` 판정 항목은 원문 Anchor Map에서 제외되어 하이라이트가 제거된다.

## 17. 검토 항목 판정

판정 상태:

- `PENDING`: 미판정
- `ACCEPTED`: 검토 반영
- `REJECTED`: 오류 아님

### 17.1 낙관적 업데이트

사용자 클릭 후 서버 응답을 기다리지 않고 먼저 화면 상태를 변경한다.

```js
const previous = finding.verdict
finding.verdict = verdict

try {
  const updated = await api.updateVerdict(...)
  Object.assign(finding, updated)
} catch (error) {
  finding.verdict = previous
}
```

요청 실패 시 이전 상태로 복구한다.

### 17.2 판정과 주석 저장

검토 카드의 주석 입력 내용은 별도 저장 버튼 없이 판정 버튼과 함께 처리된다.

- 입력 내용이 있으면 주석 생성
- 비어 있거나 공백뿐이면 주석 미생성
- 판정 자체는 정상 실행

## 18. 주석 시스템

주석은 검토 결과와 별도의 상태로 관리한다.

### 18.1 주석 생성 방식

1. 원문을 드래그해 주석 생성
2. 검토 항목 근거 아래에 작성하고 판정과 함께 생성

### 18.2 주석 데이터

```js
{
  id,
  page,
  anchorId,
  selectedText,
  context,
  findingId,
  text,
  createdAt,
  updatedAt,
}
```

### 18.3 인덱스

주석 배열 순서를 기준으로 `1, 2, 3...` 인덱스를 계산한다.

- 선택 문자열 우측 상단에 인덱스 표시
- 검토 근거 블록 우측 상단에 인덱스 표시
- 주석 탭에도 같은 번호 표시
- 주석 삭제 후 전체 번호 자동 재계산

번호를 DB 필드로 고정하지 않고 배열 순서에서 계산하기 때문에 삭제 후 별도의 번호 업데이트 요청이 필요하지 않다.

### 18.4 주석 수정과 삭제

주석 카드의 Ellipsis 메뉴에서 다음 작업을 제공한다.

- 인라인 수정
- 삭제

수정 시 카드 내부의 Textarea를 사용한다. 삭제 시 Pinia 배열에서 항목을 제거하고 Local Storage를 갱신한다.

### 18.5 주석 저장

주석은 검토 작업 ID별 Local Storage에 저장된다.

```text
logiccheck.annotations.{jobId}
```

따라서 같은 브라우저에서는 새로고침 후 유지되지만 다른 브라우저나 사용자와 공유되지 않는다. 운영 환경에서는 백엔드 저장 API가 필요하다.

## 19. 원문과 항목 사이 점프

이 프로젝트의 또 다른 핵심 기술이다.

### 19.1 검토 항목에서 원문으로

```text
우측 검토 카드 클릭
  → evidence.anchorId 확인
  → 해당 페이지로 이동
  → DOM 갱신 대기
  → Anchor 요소 탐색
  → 화면 중앙으로 스크롤
  → 깜빡임 강조
```

### 19.2 원문에서 검토 항목으로

원문 하이라이트를 클릭하면 `findingByAnchor` Map에서 검토 항목을 찾는다.

```js
const findingByAnchor = computed(() => {
  const map = {}
  findings.value.forEach((finding) => {
    finding.evidence.forEach((evidence) => {
      map[evidence.anchorId] = finding
    })
  })
  return map
})
```

찾은 항목을 활성화하고 우측 패널을 검토 결과 탭으로 전환한다.

### 19.3 주석 점프

주석 번호 또는 주석 카드를 클릭하면:

- 주석 탭 자동 표시
- 해당 페이지 이동
- 해당 Anchor로 스크롤
- 주석 인덱스 또는 위치 깜빡임

### 19.4 사용 기술

- Pinia Computed Map
- `nextTick`
- `querySelector`
- `CSS.escape`
- `scrollIntoView`
- CSS Keyframe Animation

```js
await nextTick()
target.scrollIntoView({
  behavior: 'smooth',
  block: 'center',
})
```

## 20. 문서 확대·축소

### 20.1 버튼 확대·축소

상단 도구 모음에서 확대율을 조정할 수 있다.

- 최소 55%
- 기본 82%
- 최대 140%

### 20.2 트랙패드 핀치

브라우저별 트랙패드 이벤트를 지원한다.

- Chrome 계열: `wheel` + `ctrlKey`
- macOS Safari: `gesturestart`, `gesturechange`

일반 두 손가락 스크롤은 차단하지 않고 핀치 제스처일 때만 확대율을 변경한다.

```js
if (!event.ctrlKey && !event.metaKey) return
event.preventDefault()
zoom.value = clampZoom(...)
```

## 21. AI 질문 패널

AI 질문 화면은 현재 Mock API로 동작한다.

지원 기능:

- 질문 입력
- 추천 질문
- 대화 목록
- 응답 로딩 상태
- 관련 근거 목록
- 근거 페이지 이동
- AI 답변을 검토 항목으로 승격

실제 AI 모델이 프런트에서 직접 실행되는 것은 아니며 API 계약을 기준으로 UI 흐름만 구현되어 있다.

## 22. 검토 완료와 보고서

검토 완료 시 Review Job의 완료 API를 호출하고 완료 화면으로 이동한다.

보고서 화면에서는 다음 정보를 구성한다.

- 검토자 이메일
- 검토 일시
- 검토 결과 요약
- 반영 항목
- 체크리스트
- 문서 출력용 스타일

인쇄 시 다크 모드를 사용하더라도 출력물은 라이트 팔레트를 사용하도록 Print CSS를 적용한다.

## 23. 다크 모드

다크 모드는 Composable로 분리되어 있다.

```js
const { isDark, toggle } = useTheme()
```

동작:

- 저장된 사용자 테마 로드
- 시스템 테마 참고
- `<html data-theme="dark">` 적용
- Local Storage 저장

컴포넌트는 색상을 직접 바꾸지 않고 CSS Variable을 사용한다. 다크 모드에서는 토큰 값만 재정의한다.

## 24. 반응형 UI와 레이아웃 상태

검토 화면의 좌우 패널 너비는 사용자 조작값을 저장한다.

```text
logiccheck.review.layout
```

저장 값이 너무 작거나 큰 경우를 막기 위해 Clamp 처리를 한다.

```js
Math.min(max, Math.max(min, value))
```

더블클릭하면 기본 레이아웃으로 복구할 수 있다.

## 25. 접근성 관련 구현

현재 사용 중인 접근성 기술:

- 아이콘 버튼 `aria-label`
- 토글 `role="switch"`
- 토글 `aria-checked`
- 사이드바 Resizer `role="separator"`
- 버튼 `title`
- 실제 Button Element 사용
- 입력 Label 연결
- Disabled 상태
- Escape 키로 선택 메뉴 닫기
- 키보드 포커스 스타일
- 색상과 텍스트를 함께 사용한 상태 표시

## 26. 성능 관련 구현

### 26.1 라우트 코드 분할

화면 단위 Lazy Import로 초기 번들 크기를 줄인다.

### 26.2 Computed 기반 파생 상태

필터 목록과 Anchor Map은 `computed`로 관리해 원본 상태가 변할 때만 다시 계산한다.

### 26.3 데이터 복제

Mock API에서는 JSON 직렬화를 이용해 반환 데이터를 복제한다. 화면이 Mock 저장소 원본을 직접 수정하지 않도록 하기 위한 처리다.

### 26.4 이벤트 정리

Pointer, Keyboard Event는 컴포넌트 해제 시 제거한다. 화면을 다시 열 때 이벤트가 중복 등록되는 것을 방지한다.

## 27. 코드 품질

### 27.1 ESLint

JavaScript와 Vue 템플릿의 정적 오류를 검사한다.

### 27.2 Oxlint

빠른 JavaScript 정적 분석을 제공한다.

### 27.3 Prettier

코드 포맷을 일관되게 유지한다.

### 27.4 Production Build

기능 변경 후 다음 명령으로 Vue SFC 컴파일과 번들 생성을 확인한다.

```bash
npm run build
```

## 28. 이 프로젝트에서 특징적인 프런트엔드 기술

### 28.1 Selection API를 Vue 상태와 연결한 수동 검토 기능

일반적인 Vue CRUD 애플리케이션과 가장 차별화되는 부분이다. 사용자의 브라우저 텍스트 선택을 구조화된 검토 데이터로 변환한다.

단순히 선택 문자열만 저장하지 않고 다음 정보를 함께 연결한다.

- 페이지
- 문서 블록
- 표 행과 셀
- 선택 문자열
- 검토 유형
- 주석

Vue 반응형 상태와 DOM Range를 함께 사용한다는 점이 기술적 특징이다.

### 28.2 세분화된 Anchor 시스템

문단뿐 아니라 제목, 표 캡션, 표 헤더와 개별 셀에 고유 ID를 부여한다.

이 Anchor를 기준으로:

- 검토 결과 생성
- 주석 생성
- 하이라이트 렌더링
- 원문 이동
- 우측 항목 선택
- 깜빡임 강조

가 하나의 좌표 체계로 동작한다.

### 28.3 문자열 Segment 렌더링

선택한 문자열만 하이라이트하기 위해 원문을 구간별로 나눈 후 Vue가 서로 다른 Markup으로 렌더링한다.

이는 단순 CSS Class Toggle보다 정교한 구현이며, 실제 PDF Text Layer로 확장할 때도 활용할 수 있는 구조다.

### 28.4 양방향 원문 탐색

원문과 검토 결과가 서로 이동 가능한 구조다.

- Finding → Evidence Anchor
- Evidence Anchor → Finding
- Annotation → Anchor
- Annotation Index → Annotation Panel

Pinia를 화면 간 이벤트 버스처럼 사용하지 않고 명시적인 상태와 Action으로 연결했다.

### 28.5 검토 결과와 주석의 분리

검토 결과와 주석은 유사해 보이지만 별도 모델로 관리한다.

- 검토 결과: 유형, 신뢰도, 판정 상태 보유
- 주석: 사용자 설명, 인덱스, 수정·삭제 상태 보유

도메인 의미를 분리했기 때문에 이후 백엔드 API와 권한 모델을 각각 설계할 수 있다.

### 28.6 입력 장치 통합

마우스만 고려하지 않고 다음 입력을 사용한다.

- Pointer Events: 패널 크기 조절
- Trackpad Pinch: 확대·축소
- Wheel Events: Chrome 계열 확대
- Gesture Events: Safari 확대
- Keyboard Events: Escape 처리

문서 검토 도구에 필요한 데스크톱 상호작용을 폭넓게 구현했다.

### 28.7 Mock과 실제 API의 동일 계약

Mock Handler가 단순 고정 JSON이 아니라 다음 상태 변경을 처리한다.

- 검토 판정
- 수동 검토 항목 생성
- 문서 삭제와 이름 변경
- 분석 작업 진행
- AI 대화

프런트엔드 개발 단계에서 실제 사용자 흐름을 검증할 수 있고, 실제 백엔드 전환 시 API Module 바깥의 변경을 줄일 수 있다.

## 29. 현재 기술적 한계

### 29.1 실제 PDF 렌더링이 아님

현재 원문은 구조화된 Mock 블록이다. 실제 PDF 적용 시 PDF.js Canvas와 Text Layer, Bounding Box를 연결해야 한다.

### 29.2 Local Storage 주석

주석은 서버에 저장되지 않는다. 협업, 사용자별 권한, 여러 기기 동기화를 지원하려면 백엔드 영속화가 필요하다.

### 29.3 중첩 선택 범위

동일 문자열에 검토 하이라이트와 주석이 중첩되거나 여러 범위가 겹칠 때는 범위 병합 정책이 더 필요하다.

### 29.4 여러 블록 선택

현재 한 번의 선택은 하나의 블록 안에서만 허용한다. 여러 문단이나 여러 표 셀을 묶으려면 Multi-range Evidence 모델이 필요하다.

### 29.5 프런트엔드 자동 테스트

현재 Production Build와 Lint 검증이 중심이다. 다음 테스트 도입이 권장된다.

- Vitest
- Vue Test Utils
- Playwright
- Selection/Range 유틸리티 단위 테스트
- Pinia Store 테스트
- 검토 전체 흐름 E2E 테스트

## 30. 관련 주요 파일

| 기능 | 파일 |
|---|---|
| 앱 초기화 | `src/main.js` |
| 라우팅 | `src/router/index.js` |
| 인증 상태 | `src/stores/auth.js` |
| 문서 목록 상태 | `src/stores/documents.js` |
| 검토 상태 | `src/stores/review.js` |
| HTTP Client | `src/api/http.js` |
| API 전환 | `src/api/config.js` |
| Mock Handler | `src/api/mock/handlers.js` |
| Mock Data | `src/api/mock/db.js` |
| 업로드 화면 | `src/views/upload/UploadView.vue` |
| 분석 진행 | `src/views/upload/JobProgressView.vue` |
| 라이브러리 | `src/views/library/LibraryView.vue` |
| 검토 워크스페이스 | `src/views/review/ReviewView.vue` |
| 원문과 선택 기능 | `src/components/review/DocumentViewer.vue` |
| 검토 카드 | `src/components/review/FindingCard.vue` |
| 검토 목록 | `src/components/review/FindingList.vue` |
| 주석 목록 | `src/components/review/AnnotationList.vue` |
| 목차 | `src/components/review/SectionOutline.vue` |
| AI 질문 | `src/components/review/AskPanel.vue` |
| 테마 | `src/composables/useTheme.js` |
| 디자인 토큰 | `src/assets/styles/tokens.css` |

## 31. 요약

BizXray 프런트엔드는 Vue 3, Pinia, Vue Router를 기반으로 한 문서 검토 SPA다. 문서 업로드나 라이브러리 같은 일반적인 애플리케이션 기능 외에도 브라우저 Selection API, 세분화된 Anchor ID, 문자열 Segment 렌더링과 DOM Scroll 제어를 결합하여 사용자가 원문을 직접 검토 데이터로 변환할 수 있다는 점이 가장 큰 특징이다.

특히 다음 세 가지가 이 프로젝트를 대표하는 프런트엔드 기술이다.

1. 텍스트 드래그를 검토 항목과 주석 데이터로 변환하는 Selection/Range 기반 기능
2. 원문, 검토 결과, 주석을 연결하는 양방향 Anchor Navigation
3. 문단, 제목, 표 셀까지 지원하는 동적 Segment Highlight Rendering

이 구조는 향후 PDF.js Text Layer와 실제 백엔드 영속성을 연결하면 실서비스 문서 리뷰 도구로 확장할 수 있다.
