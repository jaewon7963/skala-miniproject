# BizXray Design Specification

> Reference: `화면 기록 2026-09-03 오후 8.03.24.mov`  
> 작성 기준: 영상에서 관찰한 LG Careers 웹 UI의 시각 언어와 인터랙션을 BizXray에 적용할 수 있도록 재구성한 디자인 명세  
> 주의: 영상에 등장하는 브랜드명, 문구, 이미지와 로고는 디자인 참고 자료이며 그대로 복제하거나 제품 콘텐츠로 사용하지 않는다.

## 1. Design direction

### 1.1 핵심 인상

이 디자인은 **강한 흑백 대비**, **넓은 여백**, **굵은 제목**, **모듈형 카드**, **절제된 포인트 컬러**를 사용한다. 정보량이 많아도 장식 요소를 줄이고 큰 타이포그래피와 명확한 그룹 구분으로 사용자가 현재 맥락을 빠르게 파악하게 한다.

핵심 키워드는 다음과 같다.

- Bold: 큰 제목과 높은 글자 굵기로 명확한 정보 위계를 만든다.
- Modular: 검색, 필터, 카드, 태그를 독립된 모듈로 구성한다.
- Editorial: 일반적인 관리자 화면보다 편집 디자인에 가까운 넓은 여백과 강한 제목을 사용한다.
- Monochrome-first: 흑백과 회색을 기본으로 사용하고 강조색은 제한적으로 사용한다.
- Motion-aware: 캐러셀, 호버, 배경 영상과 같은 움직임으로 탐색성을 높인다.

### 1.2 제품 적용 원칙

BizXray에서는 원본 영상의 채용 사이트 구조를 그대로 복제하지 않는다. 대신 다음 시각 원칙만 가져온다.

1. 검토 화면의 복잡한 정보는 명도 차이와 공간 구획으로 분리한다.
2. 주요 CTA만 포인트 컬러를 사용한다.
3. 제목과 핵심 수치는 크게, 보조 설명은 작고 옅게 표현한다.
4. 카드와 필터는 둥근 모서리와 단순한 면으로 구성한다.
5. 하나의 화면에서 포인트 컬러를 과도하게 혼합하지 않는다.

## 2. Color system

영상에서 관찰된 색상을 기반으로 한 권장 팔레트다. 영상 압축과 디스플레이 환경에 따라 실제 브랜드 원색과 차이가 있을 수 있다.

### 2.1 Neutral colors

| Token | Value | Usage |
|---|---:|---|
| `--color-black` | `#17181C` | 헤더, 주요 버튼, 강한 텍스트 |
| `--color-charcoal` | `#222329` | 다크 배경, 하단 고정 영역 |
| `--color-graphite` | `#3A3B40` | 호버 오버레이, 보조 다크 면 |
| `--color-gray-700` | `#5F6168` | 보조 텍스트 |
| `--color-gray-500` | `#8F9299` | 메타 정보, 비활성 아이콘 |
| `--color-gray-300` | `#D8DADE` | 구분선, 비활성 테두리 |
| `--color-gray-200` | `#E9EAEC` | 카드 배경, 입력 배경 |
| `--color-gray-100` | `#F4F5F6` | 페이지 보조 배경 |
| `--color-white` | `#FFFFFF` | 기본 표면과 다크 배경 위 텍스트 |

### 2.2 Accent colors

| Token | Value | Usage |
|---|---:|---|
| `--color-accent` | `#E60046` | Floating Action, 가장 중요한 CTA |
| `--color-accent-hover` | `#C9003D` | Accent 버튼 호버 |
| `--color-lime` | `#B8FF68` | 캠페인·히어로 비주얼 배경 |
| `--color-link` | `#A8D5FF` | 다크 배경의 링크 또는 보조 강조 |

### 2.3 BizXray semantic colors

검토 도메인의 의미 색상은 영상의 브랜드 포인트색과 분리한다.

| Meaning | Foreground | Background | Usage |
|---|---:|---:|---|
| 오류 | `#D92D20` | `#FEECEB` | 오류 검토 항목, 원문 하이라이트 |
| 확인 필요 | `#E85E00` | `#FFF1E6` | 확인 필요 항목 |
| 근거 부족 | `#2878B8` | `#EAF6FF` | 근거 부족 항목 |
| 성공 | `#15803D` | `#EEF8F1` | 완료, 저장 성공 |
| 주석 | `#8A4B00` | `#FFF4D8` | 사용자 주석과 인덱스 |

### 2.4 컬러 사용 규칙

- 한 화면에서 브랜드 Accent와 검토 Semantic Color를 같은 비중으로 사용하지 않는다.
- Accent는 CTA에, Semantic Color는 상태 전달에 사용한다.
- 본문 배경은 기본적으로 흰색을 유지한다.
- 다크 배경에서 본문 텍스트는 순백색보다 `#F4F5F6`을 우선 사용한다.
- 회색 텍스트는 WCAG AA 명암비를 만족하도록 배경에 따라 조정한다.

## 3. Typography

### 3.1 Font family

한글과 영문 모두 획이 명확한 산세리프를 사용한다.

```css
font-family:
  'Pretendard',
  -apple-system,
  BlinkMacSystemFont,
  'Apple SD Gothic Neo',
  'Noto Sans KR',
  'Malgun Gothic',
  sans-serif;
```

### 3.2 Type scale

| Style | Desktop | Weight | Line height | Usage |
|---|---:|---:|---:|---|
| Display | 48–64px | 700–800 | 1.12 | 랜딩 핵심 메시지 |
| H1 | 36–44px | 700–800 | 1.2 | 주요 섹션 제목 |
| H2 | 28–32px | 700 | 1.3 | 화면 제목, 카드 그룹 제목 |
| H3 | 20–24px | 700 | 1.4 | 카드 제목 |
| Body Large | 17–18px | 500–600 | 1.6 | 강조 설명 |
| Body | 14–16px | 400–500 | 1.6 | 일반 본문 |
| Label | 12–14px | 600–700 | 1.4 | 태그, 필터, 메타 정보 |
| Caption | 11–12px | 400–600 | 1.4 | 날짜, 상태 보조 설명 |

### 3.3 Typography rules

- 제목은 자간을 약간 좁게 사용한다: `letter-spacing: -0.02em`.
- 큰 제목은 두 줄 이내로 제한한다.
- 카드 제목은 2–3줄까지 허용하되 말줄임 여부를 명확히 정한다.
- 메타 정보는 제목보다 최소 두 단계 낮은 위계를 사용한다.
- 숫자, 페이지 번호, 인덱스는 동일한 크기와 정렬 규칙을 사용한다.

## 4. Spacing and sizing

### 4.1 Spacing scale

4px 기반 스케일을 사용한다.

```text
4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 / 48 / 64 / 80 / 96
```

### 4.2 Layout width

- 데스크톱 콘텐츠 최대 너비: `1280–1440px`
- 일반 페이지 좌우 패딩: `24–40px`
- 랜딩 섹션 좌우 패딩: `64–72px`
- 모바일 좌우 패딩: `16–20px`
- 카드 간격: `16–24px`
- 대형 섹션 간격: `80–120px`

### 4.3 Radius

영상에서는 검색 컨트롤과 필터에 비교적 큰 Radius가 사용되고, 대형 카드에는 절제된 Radius가 사용된다.

| Token | Value | Usage |
|---|---:|---|
| `--radius-sm` | `6px` | 작은 버튼, 배지 |
| `--radius-md` | `10px` | 입력창, 카드 |
| `--radius-lg` | `16px` | 대형 검색 영역, 모달 |
| `--radius-pill` | `999px` | 태그, 세그먼트 필터, FAB |

## 5. Layout system

### 5.1 Global header

영상의 헤더는 어두운 배경 위에 밝은 텍스트를 사용하며, 하단에 대형 검색 바가 결합된다.

권장 구조:

```text
┌─────────────────────────────────────────────────────────────┐
│ Logo                     Navigation               Account   │
│ Product slogan / current context                            │
│ ┌ Keyword ┬ Company ┬ Type ┬ Role ┬ Region ┬ Search ┐      │
│ └─────────┴─────────┴──────┴──────┴────────┴────────┘      │
└─────────────────────────────────────────────────────────────┘
```

BizXray에서는 검색 바를 문서 라이브러리의 통합 검색·상태·태그 필터로 변환할 수 있다.

### 5.2 Content sections

- 섹션 제목은 좌측 정렬한다.
- 캐러셀 제어는 제목 우측에 둔다.
- 활성 필터는 제목 아래 또는 카드 위에 배치한다.
- 한 섹션에 하나의 주된 사용자 행동만 강조한다.

### 5.3 Review workspace

검토 화면은 정보 밀도가 높으므로 랜딩 페이지보다 작은 타입 스케일을 적용한다.

```text
┌──────────────────────────────────────────────────────────────┐
│ Document title                 Evidence  Progress  Complete   │
├────────────┬───────────────────────────────┬────────────┬─────┤
│ Outline    │ Document canvas               │ Panel      │Rail │
│            │                               │ content    │icons│
└────────────┴───────────────────────────────┴────────────┴─────┘
```

- 문서 캔버스는 가장 밝은 표면을 사용한다.
- 좌우 사이드바는 한 단계 어두운 배경을 사용한다.
- 선택된 검토 항목은 테두리와 얕은 그림자로 구분한다.
- 우측 아이콘 레일은 폭 `44–52px`를 유지한다.

## 6. Component specifications

### 6.1 Search bar

- 전체 높이: `60–68px`
- 외부 배경: White
- 내부 필드 배경: Gray 100
- 필드 사이 간격: `8px`
- 검색 버튼: Black background / White text
- 아이콘 크기: `20–24px`
- 포커스 시 2px outline 또는 명확한 focus ring 제공

### 6.2 Segmented filter

영상의 계열 필터처럼 하나의 회색 Pill 안에 여러 선택지를 둔다.

- 컨테이너: Gray 100, pill radius
- 선택 항목: Charcoal background, White text
- 비선택 항목: Transparent, Black text
- 높이: `40–44px`
- 항목 간 패딩: `12–18px`

태그 필터가 많아지면 한 줄 가로 스크롤을 허용한다.

### 6.3 Content card

- 카드 배경: Gray 100 또는 이미지
- 최소 높이: `360–480px`
- 카드 내부 패딩: `28–32px`
- 상단에 작은 태그 배치
- 제목은 크고 굵게 표시
- 보조 정보는 Gray 500
- 이동 액션은 우측 상단 원형 화살표로 표시

호버 상태:

- 배경 명도를 낮추거나 이미지 위에 어두운 오버레이를 추가한다.
- 원형 화살표의 테두리와 아이콘 명암을 강화한다.
- 카드 전체가 클릭 가능하면 `cursor: pointer`를 사용한다.

### 6.4 Buttons

#### Primary dark button

- Background: `#17181C`
- Foreground: White
- Height: `44–52px`
- Radius: 8–10px
- Hover: `#303137`

#### Accent floating action button

- Size: `56–64px`
- Shape: Circle
- Background: Accent Red
- Icon: White, 24px
- Position: viewport bottom/right `24–32px`
- Shadow: `0 8px 24px rgba(0, 0, 0, 0.20)`

BizXray에서는 FAB를 새 문서 업로드처럼 화면 전역에서 중요한 단일 행동에만 사용한다.

### 6.5 Tags and badges

- Pill 형태 사용
- 높이: `24–28px`
- 좌우 패딩: `8–10px`
- 기본 태그는 흰색 또는 Gray 100
- 상태 배지는 Semantic Color 사용
- 태그와 상태는 색상뿐 아니라 텍스트로 의미를 전달한다.

### 6.6 Form controls

- Label은 입력창 위에 배치한다.
- 입력 높이: `44–48px`
- Border: Gray 300
- Background: White 또는 Gray 100
- Error: Danger border + message
- Disabled: 낮은 명암과 `not-allowed` cursor
- Placeholder는 본문보다 1단계 낮은 명암 사용

### 6.7 Review finding card

- 기본 배경: White
- Border: 1px Gray 300
- 선택 상태: 2px Accent 또는 기존 BizXray Orange
- 상단: 번호 / 유형 / 페이지
- 중단: 제목 / 설명 / 근거
- 하단: 주석 / 판정 액션 / 신뢰도
- 판정 완료 상태에서는 배경을 Gray 100으로 낮추되 유형 배지는 유지한다.

### 6.8 Annotation card

- 좌측 또는 상단에 순번 배지 표시
- 본문은 2–3줄까지 표시
- 연결된 원문은 작은 인용문 스타일로 표시
- 옵션 메뉴는 우측 상단 Ellipsis 사용
- 수정 시 카드 내부 인라인 편집을 우선한다.

## 7. Imagery and media

영상 후반부는 고채도의 라임 배경, 거대한 흰색 로고, 꽃·자연 오브젝트를 결합한 풀블리드 미디어를 사용한다.

적용 원칙:

- 핵심 캠페인이나 랜딩 히어로에만 사용한다.
- 업무 화면에서는 배경 영상을 사용하지 않는다.
- 이미지 위 텍스트는 충분한 오버레이를 추가한다.
- 브랜드 로고는 원본 비율을 유지한다.
- 자동재생 영상은 음소거, 정지 버튼, `prefers-reduced-motion` 대응이 필요하다.

## 8. Motion and interaction

### 8.1 Timing

| Motion | Duration | Easing |
|---|---:|---|
| Hover | 120–160ms | ease-out |
| Panel transition | 180–240ms | ease-in-out |
| Modal | 200ms | ease-out |
| Carousel | 320–480ms | cubic-bezier |
| Finding flash | 700–900ms | ease |

### 8.2 Carousel

- 좌우 화살표는 섹션 제목 오른쪽에 배치한다.
- 다음 카드의 일부를 노출해 수평 탐색 가능성을 보여준다.
- 마우스, 키보드, 터치 스와이프를 모두 지원한다.
- 현재 위치를 스크린리더에 전달한다.

### 8.3 Hover

- 카드 전체가 어두워지거나 명도가 변한다.
- 링크 또는 원형 화살표가 명확하게 강조된다.
- 레이아웃 크기가 변하는 호버는 피한다.

### 8.4 Reduced motion

```css
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    scroll-behavior: auto !important;
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
```

## 9. Responsive behavior

### Desktop: 1200px+

- 복합 검색 바를 한 줄로 표시
- 카드 3개 이상 노출
- 리뷰 화면 3분할 유지
- 콘텐츠 최대 너비 적용

### Tablet: 768–1199px

- 검색 필터를 2줄로 재배치
- 카드 2개 노출
- 리뷰 목차는 접을 수 있게 제공
- 우측 패널 폭 축소

### Mobile: 767px 이하

- 헤더 메뉴 축약
- 검색 필터는 Bottom Sheet 또는 Drawer 사용
- 카드 1개 + 다음 카드 일부 노출
- 리뷰 화면은 문서 / 목차 / 검토 결과를 탭으로 전환
- FAB는 하단 Safe Area를 고려한다.

## 10. Accessibility

- 일반 텍스트는 최소 4.5:1 명암비를 확보한다.
- 큰 텍스트는 최소 3:1을 확보한다.
- 아이콘 버튼에는 `aria-label`과 `title`을 제공한다.
- 모든 기능은 키보드로 조작 가능해야 한다.
- 포커스 링을 제거하지 않는다.
- 색상만으로 상태를 표현하지 않는다.
- 캐러셀은 이전/다음 버튼과 현재 위치 정보를 제공한다.
- 영상은 정지 기능과 자막 또는 대체 설명을 제공한다.
- 클릭 영역은 최소 `40×40px`, 모바일은 `44×44px`를 권장한다.

## 11. Recommended CSS tokens

```css
:root {
  --color-black: #17181c;
  --color-charcoal: #222329;
  --color-graphite: #3a3b40;
  --color-gray-700: #5f6168;
  --color-gray-500: #8f9299;
  --color-gray-300: #d8dade;
  --color-gray-200: #e9eaec;
  --color-gray-100: #f4f5f6;
  --color-white: #ffffff;

  --color-accent: #e60046;
  --color-accent-hover: #c9003d;
  --color-lime: #b8ff68;

  --space-1: 4px;
  --space-2: 8px;
  --space-3: 12px;
  --space-4: 16px;
  --space-5: 20px;
  --space-6: 24px;
  --space-8: 32px;
  --space-10: 40px;
  --space-12: 48px;
  --space-16: 64px;

  --radius-sm: 6px;
  --radius-md: 10px;
  --radius-lg: 16px;
  --radius-pill: 999px;

  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.08);
  --shadow-md: 0 6px 20px rgba(0, 0, 0, 0.12);
  --shadow-float: 0 10px 28px rgba(0, 0, 0, 0.22);

  --transition-fast: 140ms ease-out;
  --transition-base: 220ms ease-in-out;
}
```

## 12. BizXray application checklist

### Library

- [ ] 상단 검색과 상태·기간·태그 필터의 시각적 그룹 강화
- [ ] 선택 태그를 Segmented/Pill 스타일로 표현
- [ ] 문서 목록의 제목 위계 강화
- [ ] 업로드 CTA를 Primary 또는 FAB 중 하나로 통일
- [ ] 빈 상태에도 동일한 타이포그래피 위계 적용

### Review workspace

- [ ] 문서 캔버스와 사이드 패널의 명도 차이 유지
- [ ] 우측 아이콘 레일의 선택 상태 대비 강화
- [ ] 검토 결과 색상은 Semantic Color만 사용
- [ ] 주석 인덱스 크기와 위치 일관성 유지
- [ ] 확대·축소 및 페이지 이동 컨트롤의 클릭 영역 확대

### Authentication and settings

- [ ] 폼 너비와 간격을 단순하게 유지
- [ ] 가장 중요한 제출 버튼만 진한 색상 사용
- [ ] 오류 메시지를 입력창과 가까이 배치
- [ ] 이메일 기반 사용자 모델을 일관되게 표시

## 13. Do and don't

### Do

- 큰 제목과 넓은 여백으로 정보 위계를 만든다.
- 흑백을 기반으로 Accent와 Semantic Color를 분리한다.
- 태그, 상태, 액션의 모양과 역할을 일관되게 유지한다.
- 카드 전체 클릭 영역과 명확한 화살표 액션을 제공한다.
- 애니메이션은 탐색 위치나 상태 변화를 설명하는 데 사용한다.

### Don't

- 모든 버튼과 링크에 Accent Red를 사용하지 않는다.
- 검토 오류 색상과 브랜드 Accent를 같은 의미로 혼용하지 않는다.
- 업무용 검토 화면에 고채도 히어로 배경을 사용하지 않는다.
- 지나치게 작은 텍스트나 낮은 명암의 회색을 사용하지 않는다.
- 영상의 브랜드 로고, 이미지, 카피를 제품에 그대로 복제하지 않는다.

---

이 문서는 화면 녹화에서 관찰한 시각적 특징을 재사용 가능한 디자인 언어로 번역한 것이다. 실제 구현 시에는 기존 `tokens.css`의 Semantic Color와 사용자 검토 흐름을 유지하면서, 레이아웃·타이포그래피·카드·검색 UI부터 단계적으로 적용한다.
