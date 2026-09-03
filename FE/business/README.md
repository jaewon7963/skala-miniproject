# LogicCheck FE

사업계획서 검토 플랫폼의 프론트엔드입니다. **Vue 3 (script setup) + Vite + Vue Router + Pinia** 구성입니다.

## 실행

```bash
npm install
npm run dev      # http://localhost:5173
npm run build
npm run lint
```

## Mock ↔ 실제 백엔드 전환

이 프로젝트의 화면은 **API 계약(JSON 구조)** 위에서 동작합니다.
BE가 준비되면 `.env.development` 의 값 하나만 바꾸면 됩니다.

```env
VITE_USE_MOCK=false                          # true = Mock, false = 실제 API
VITE_API_BASE_URL=/api
VITE_API_PROXY_TARGET=http://localhost:8081  # Spring Boot
```

전환 지점은 `src/api/modules/*.js` 한 곳뿐입니다.

```js
list: (params) => (USE_MOCK ? mock.documents.list(params) : http.get(EP.documents.list, { params }))
```

- 경로가 바뀌면 → `src/api/endpoints.js` 만 수정
- 응답 필드가 바뀌면 → `src/constants/enums.js` + 해당 모듈의 주석 계약만 수정
- 화면(views)과 스토어는 `http.js` 를 직접 호출하지 않습니다.

## 디렉터리

```
src/
├─ api/
│  ├─ http.js          fetch 래퍼 (baseURL · 토큰 · 에러 정규화 · 업로드 진행률)
│  ├─ endpoints.js     API 경로 상수 — BE 명세 확정 시 여기만 수정
│  ├─ config.js        USE_MOCK 스위치
│  ├─ modules/         auth · documents · reviews  (화면이 호출하는 유일한 창구)
│  └─ mock/            db.js(픽스처) · handlers.js  ← BE 연동 후 삭제 대상
├─ assets/styles/
│  ├─ tokens.css       디자인 토큰 (배경 화이트 / 메인 오렌지 / 서브 레드)
│  └─ base.css         리셋 · 공통 유틸 · 인쇄 스타일
├─ components/
│  ├─ common/          AppButton · AppInput · AppSelect · AppModal · AppToast
│  │                   StatusBadge · FindingTypeBadge · ProgressBar · Pagination · EmptyState
│  └─ review/          SectionOutline · DocumentViewer · FindingList · FindingCard · AskPanel
├─ constants/enums.js  도메인 enum + 표기 라벨 (단일 소스)
├─ layouts/            AuthLayout · AppLayout · WorkspaceLayout
├─ router/index.js     라우트 + 인증 가드
├─ stores/             auth · documents · review · ui(토스트)
├─ utils/              format · validators
└─ views/              화면 10개
```

## 화면 ↔ 유스케이스

| 라우트 | 화면 | 유스케이스 |
|---|---|---|
| `/` | 웰컴(랜딩) — 서비스 소개 · 로그인/회원가입 진입 | AUTH-01 |
| `/login`, `/signup` | 로그인 · 회원가입 | AUTH-01~03 |
| `/library` | 문서 라이브러리 | DASH-01~04 |
| `/upload` | 업로드 · 버전 등록 | UP-01 |
| `/jobs/:jobId/progress` | 파싱 · 분석 진행 | UP-02, ANL-01~03 |
| `/review/:jobId` | 검토 화면 (3분할) | REV-01~07 |
| `/review/:jobId/done` | 검토 완료 | SHR-01 |
| `/review/:jobId/report` | 검토 의견서 · 내보내기 | SHR-03 |
| `/settings` | 프로필 · 계정 | AUTH-05, AUTH-06 |

## 테마

색상은 전부 `src/assets/styles/tokens.css` 의 CSS 변수입니다. 하드코딩된 색상값은 없습니다.

- 배경 `--c-bg` 화이트
- 메인 `--c-primary-500` `#ff6a00` (버튼 · 활성 상태 · 강조)
- 서브 `--c-danger-500` `#e5342b` (오류 유형 · 파괴적 동작)
- 검토 유형: 오류=레드 / 확인 필요=오렌지 / 근거 부족=슬레이트

## 기획 확정이 필요한 부분 (코드에 TODO 주석으로 표시)

1. 검토 결과 유형 — 현재 3종(`FINDING_TYPE`). PRD의 4종으로 확정되면 enum만 교체
2. 판정 — 현재 2종 + 되돌리기. `VERDICT.HOLD`(보류)는 주석 처리 상태
3. 검토 점수 — `summary.score` 로 분리해 두었습니다. 정책 미확정 시 화면에서 제거 가능
4. 공유(SHR-02) · 비밀번호 재설정(AUTH-04) · Google/SSO — 비활성 버튼으로 자리만 유지
5. AI 질문 탭 — 3일 범위 포함 여부 미정 (`AskPanel.vue` 단독 제거 가능)
6. 웰컴 페이지의 지표 영역 — 실제 사용자 수치가 없어 제품 사실(유형 수 · 업로드 제한 등)로 채웠습니다. 실측치가 생기면 교체
