<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useReveal } from '@/composables/useReveal'
import AppButton from '@/components/common/AppButton.vue'

/**
 * 웰컴(랜딩) 페이지 — 미로그인 방문자의 첫 화면
 * 참고: 유스케이스 AUTH-01은 세션 분기만 정의하고 있어, 소개 페이지는 이 화면으로 추가했습니다.
 * 시각 언어 · 모션 타이밍은 DESIGN.md (Bold / Editorial / Motion-aware) 를 따릅니다.
 */
const router = useRouter()
const auth = useAuthStore()

const page = ref(null)
useReveal(page)

const pains = [
  { icon: '≠', text: '같은 지표가 페이지마다\n다른 값으로 적혀 있음' },
  { icon: '∑', text: '합계 · 단가 × 수량을\n매번 손으로 검산' },
]

const values = [
  { icon: '⤢', title: '문서 탐색 감소', desc: '흩어진 수치와 원문을 한 화면에서 비교합니다' },
  { icon: '⚑', title: '검토 우선순위', desc: '유형과 확신도로 먼저 볼 항목을 알려줍니다' },
  { icon: '⌕', title: '근거 중심 결과', desc: '원문 위치 · 계산식 · 적용 기준을 함께 제시합니다' },
]

const features = [
  { tag: 'CALCULATION', title: '수치 검산', desc: '합계 · 단가 × 수량 · 인원 × 기간을 자동으로 다시 계산해 차이를 보여줍니다' },
  { tag: 'CONSISTENCY', title: '항목 간 모순', desc: '고객 · 채널 · 수익모델 · 매출계획이 서로 이어지는지 검증합니다' },
  { tag: 'EVIDENCE', title: '주장–근거 연결', desc: '근거가 없는 주장과 출처가 빠진 인용을 찾아냅니다' },
  { tag: 'TECH · KPI', title: '기술 적합성', desc: '기능–기술–운영환경, KPI–평가방법을 교차 검토합니다' },
  { tag: 'EXPORT', title: '검토 의견서', desc: '판정한 항목만 모아 수정 지시 형태로 내보냅니다' },
]

const facts = [
  { value: '3유형', label: '오류 · 확인 필요 · 근거 부족', caption: 'RESULT TYPES' },
  { value: '원문 근거', label: '모든 지적에 페이지와 위치를 연결', caption: 'EVIDENCE LINKED' },
  { value: '최종 판단', label: '검토 반영 · 오류 아님은 사용자가 결정', caption: 'YOUR DECISION' },
  { value: 'PDF 50MB', label: '최대 200페이지까지 업로드', caption: 'SUPPORTED INPUT' },
]

const steps = [
  { no: '01', title: '사업계획서 업로드', desc: 'PDF를 올리면 목차 · 표 · 수치 · KPI를 구조화합니다' },
  { no: '02', title: '자동 검증', desc: '결정적 검산과 관계 판단이 병렬로 실행되며 항목이 쌓입니다' },
  { no: '03', title: '근거 확인 후 판정', desc: '원문과 항목을 오가며 검토 반영 · 오류 아님을 기록합니다' },
]

const personas = [
  {
    title: '정부지원사업\n평가위원',
    desc: '제한된 시간에 다수의 계획서를 검토하며, 평가에 영향을 줄 수 있는 오류를 놓치지 않아야 합니다.',
  },
  {
    title: '투자심사역\n기술 컨설턴트',
    desc: '매출 추정과 기술 구성의 전제를 빠르게 확인하고, 추가 질의가 필요한 지점을 선별합니다.',
  },
  {
    title: '기업\n내부 검토자',
    desc: '제출 전에 수치 불일치와 근거 누락을 미리 잡아 재제출과 보완 요청을 줄입니다.',
  },
]

const faqs = [
  {
    q: '어떤 파일을 올릴 수 있나요?',
    a: 'PDF 형식의 사업계획서를 지원합니다. 최대 50MB · 200페이지까지 업로드할 수 있으며, 형식 불일치 · 용량 초과 · 손상 파일 · 중복 파일은 업로드 단계에서 사유와 함께 안내합니다. HWP와 DOCX는 파서 어댑터 추가 후 지원할 예정입니다.',
  },
  {
    q: '사업의 성공 가능성도 판정해 주나요?',
    a: '아니요. BizXray는 사업 성공 가능성을 자동으로 판정하지 않습니다. 검산할 수 있는 오류와 전문가의 추가 판단이 필요한 항목을 구분해 제시하는 것이 원칙입니다.',
  },
  {
    q: 'AI가 잘못 지적하면 어떻게 하나요?',
    a: '모든 지적에는 원문 위치 · 계산식 · 확신도가 함께 제공됩니다. 확인 후 오류 아님으로 판정할 수 있고, 판정은 언제든 되돌릴 수 있습니다. 최종 판단은 항상 검토자에게 있습니다.',
  },
  {
    q: '근거를 찾지 못하면 어떻게 되나요?',
    a: '판단에 필요한 근거나 조건이 부족한 경우에는 임의로 결론을 만들지 않고 확인이 필요한 항목으로 표시합니다.',
  },
]

const start = () => router.push({ name: auth.isAuthenticated ? 'library' : 'signup' })

/* ---------------- 기능 캐러셀 (DESIGN 8.2) ---------------- */
const track = ref(null)
const atStart = ref(true)
const atEnd = ref(false)

function syncEdges() {
  const el = track.value
  if (!el) return
  atStart.value = el.scrollLeft <= 2
  atEnd.value = el.scrollLeft + el.clientWidth >= el.scrollWidth - 2
}

/**
 * 화살표 이동
 * scrollBy({ behavior: 'smooth' }) 는 스냅 컨테이너에서 무시되는 경우가 있어
 * scrollLeft 를 직접 지정하고, 부드러운 이동은 CSS scroll-behavior 에 맡깁니다.
 * (탭이 백그라운드일 때 rAF 가 멈추는 문제도 함께 피합니다)
 */
function slide(direction) {
  const el = track.value
  if (!el) return
  const card = el.querySelector('.f-card')
  const step = (card?.offsetWidth ?? 300) + 16
  const max = el.scrollWidth - el.clientWidth
  const target = Math.max(0, Math.min(max, el.scrollLeft + direction * step))
  el.scrollLeft = target

  // smooth 스크롤이 끝나기 전이라 scrollLeft 되읽기 대신 목표값으로 갱신합니다
  atStart.value = target <= 2
  atEnd.value = target >= max - 2
}

/* ---------------- 히어로를 지나면 뜨는 시작 버튼 (DESIGN 6.4) ---------------- */
const hero = ref(null)
const showFab = ref(false)
let heroObserver = null

onMounted(() => {
  syncEdges()
  window.addEventListener('resize', syncEdges)

  if (hero.value && 'IntersectionObserver' in window) {
    heroObserver = new IntersectionObserver(
      ([entry]) => (showFab.value = !entry.isIntersecting),
      { threshold: 0.1 },
    )
    heroObserver.observe(hero.value)
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncEdges)
  heroObserver?.disconnect()
})
</script>

<template>
  <div ref="page" class="welcome">
    <!-- ================= HERO ================= -->
    <section ref="hero" class="hero">
      <span class="hero__eyebrow" data-reveal>AI 기반 사업계획서 검토 지원 서비스</span>

      <h1 class="hero__title">
        <span data-reveal style="--d: 60ms">사업계획서의</span>
        <span data-reveal style="--d: 140ms"><em>흩어진 근거를</em></span>
        <span data-reveal style="--d: 220ms">연결합니다</span>
      </h1>

      <p class="hero__desc" data-reveal style="--d: 320ms">
        수치 · 주장 · 기술 · KPI를 이어 붙여, 검토자가 확인해야 할 지점을 근거와 함께 선별합니다
      </p>

      <div class="hero__cta" data-reveal style="--d: 400ms">
        <AppButton size="lg" @click="start">무료로 시작하기</AppButton>
        <AppButton size="lg" variant="secondary" @click="router.push({ name: 'login' })">
          로그인
        </AppButton>
      </div>

      <!-- 제품 미리보기 : 문서를 훑는 스캔 라인 -->
      <div class="preview" data-reveal style="--d: 480ms">
        <div class="preview__bar">
          <span /><span /><span />
          <b>AI 매장 안내 로봇 사업계획서 v3</b>
        </div>

        <div class="preview__body">
          <ul class="preview__outline">
            <li v-for="n in 6" :key="n"><i /></li>
          </ul>

          <div class="preview__paper">
            <i class="line line--title" />
            <i class="line" />
            <i class="line line--hl-error" />
            <i class="line line--short" />
            <i class="line line--hl-check" />
            <i class="line" />
            <i class="line line--short" />
          </div>

          <div class="preview__panel">
            <div class="preview__counts">
              <b class="is-error">3</b><b class="is-check">3</b><b class="is-evidence">2</b>
            </div>
            <div v-for="n in 3" :key="n" class="preview__card">
              <i class="chip" :class="`chip--${n}`" />
              <i class="line line--short" />
              <i class="line" />
            </div>
          </div>
        </div>

        <span class="preview__scan" aria-hidden="true" />
        <div class="preview__float preview__float--left">p.11 매출 합계 불일치 · 3,200만 원</div>
        <div class="preview__float preview__float--right">확신도 96% · 결정적 검산</div>
      </div>
    </section>

    <!-- ================= PROBLEM ================= -->
    <section id="problem" class="section">
      <h2 class="section__title" data-reveal>
        사업계획서 검토, 아직도 <em>문서를 오가며</em> 하고 계신가요?
      </h2>

      <div class="pains">
        <div
          v-for="(pain, index) in pains"
          :key="pain.text"
          class="pains__item"
          data-reveal
          :style="{ '--d': `${index * 90}ms` }"
        >
          <span class="pains__icon">{{ pain.icon }}</span>
          <p>{{ pain.text }}</p>
        </div>
      </div>

      <div class="dots" data-reveal aria-hidden="true"><i /><i /><i /></div>

      <h2 class="section__title" data-reveal>
        AI의 결론만 받아들이지 말고<br />
        <em>근거까지 확인하는 BizXray</em>와 함께하세요
      </h2>

      <div class="values">
        <article
          v-for="(value, index) in values"
          :key="value.title"
          class="values__card"
          data-reveal
          :style="{ '--d': `${index * 90}ms` }"
        >
          <span class="values__icon">{{ value.icon }}</span>
          <h3>{{ value.title }}</h3>
          <p>{{ value.desc }}</p>
        </article>
      </div>
    </section>

    <!-- ================= FEATURES (캐러셀) ================= -->
    <section id="features" class="band">
      <header class="band__head">
        <div data-reveal>
          <p class="band__eyebrow">FEATURES</p>
          <h2 class="band__title">검토가 쉬워지는 BizXray 기능들</h2>
          <p class="band__desc">원문 뷰어와 검토 항목을 한 화면에 붙여 확인 시간을 줄입니다</p>
        </div>

        <div class="band__nav" data-reveal>
          <button
            type="button"
            aria-label="이전 기능 보기"
            :disabled="atStart"
            @click="slide(-1)"
          >
            ‹
          </button>
          <button type="button" aria-label="다음 기능 보기" :disabled="atEnd" @click="slide(1)">
            ›
          </button>
        </div>
      </header>

      <div
        ref="track"
        class="f-track u-scroll"
        role="region"
        aria-label="BizXray 기능 목록"
        tabindex="0"
        @scroll.passive="syncEdges"
      >
        <article
          v-for="(feature, index) in features"
          :key="feature.title"
          class="f-card"
          data-reveal
          :style="{ '--d': `${index * 70}ms` }"
        >
          <p class="f-card__tag">{{ feature.tag }}</p>
          <h3 class="f-card__title">{{ feature.title }}</h3>
          <p class="f-card__desc">{{ feature.desc }}</p>
          <span class="f-card__index">{{ String(index + 1).padStart(2, '0') }}</span>
        </article>
      </div>

      <div class="band__foot" data-reveal>
        <AppButton size="lg" variant="secondary" @click="start">무료로 시작하기</AppButton>
      </div>
    </section>

    <!-- ================= FACTS ================= -->
    <section class="facts">
      <div
        v-for="(fact, index) in facts"
        :key="fact.caption"
        class="facts__item"
        data-reveal
        :style="{ '--d': `${index * 80}ms` }"
      >
        <b>{{ fact.value }}</b>
        <span>{{ fact.label }}</span>
        <em>{{ fact.caption }}</em>
      </div>
    </section>

    <!-- ================= FLOW ================= -->
    <section id="flow" class="section">
      <h2 class="section__title" data-reveal>업로드부터 <em>검토 의견서</em>까지 세 단계</h2>

      <ol class="steps">
        <li
          v-for="(step, index) in steps"
          :key="step.no"
          class="steps__item"
          data-reveal
          :style="{ '--d': `${index * 110}ms` }"
        >
          <span class="steps__no">{{ step.no }}</span>
          <h3>{{ step.title }}</h3>
          <p>{{ step.desc }}</p>
        </li>
      </ol>
    </section>

    <!-- ================= PERSONA ================= -->
    <section class="section section--subtle">
      <h2 class="section__title" data-reveal>이런 분들의 검토를 돕습니다</h2>

      <div class="personas">
        <article
          v-for="(persona, index) in personas"
          :key="persona.title"
          class="personas__card"
          data-reveal
          :style="{ '--d': `${index * 90}ms` }"
        >
          <h3>{{ persona.title }}</h3>
          <p>{{ persona.desc }}</p>
        </article>
      </div>
    </section>

    <!-- ================= FAQ ================= -->
    <section id="faq" class="section">
      <h2 class="section__title" data-reveal>자주 묻는 질문</h2>

      <div class="faq">
        <details
          v-for="(faq, index) in faqs"
          :key="faq.q"
          class="faq__item"
          data-reveal
          :style="{ '--d': `${index * 70}ms` }"
        >
          <summary>
            <span><b>Q.</b> {{ faq.q }}</span>
            <i aria-hidden="true">⌄</i>
          </summary>
          <p>{{ faq.a }}</p>
        </details>
      </div>
    </section>

    <!-- ================= CLOSING ================= -->
    <section class="closing">
      <h2 data-reveal>검토해야 할 지점을 먼저 확인하세요</h2>
      <p data-reveal style="--d: 90ms">계정을 만들면 사업계획서를 바로 올릴 수 있습니다</p>
      <div class="closing__cta" data-reveal style="--d: 180ms">
        <AppButton size="lg" @click="start">무료로 시작하기</AppButton>
        <AppButton size="lg" variant="ghost" @click="router.push({ name: 'login' })">
          이미 계정이 있어요
        </AppButton>
      </div>
    </section>

    <!-- 히어로를 지나면 떠오르는 시작 버튼 -->
    <transition name="fab">
      <button v-if="showFab" class="fab" type="button" @click="start">
        무료로 시작하기
        <i aria-hidden="true">→</i>
      </button>
    </transition>
  </div>
</template>

<style scoped>
.welcome {
  --gutter: clamp(20px, 5vw, 72px);
}
section {
  scroll-margin-top: 72px;
}

/* ---------------- 등장 모션 ---------------- */
[data-reveal] {
  opacity: 0;
  transform: translateY(20px);
  transition:
    opacity 560ms var(--ease-out),
    transform 640ms var(--ease-spring);
  transition-delay: var(--d, 0ms);
  will-change: opacity, transform;
}
[data-reveal].is-visible {
  opacity: 1;
  transform: none;
}

/* ---------------- HERO ---------------- */
.hero {
  position: relative;
  max-width: var(--content-max);
  margin: 0 auto;
  padding: clamp(56px, 9vw, 104px) var(--gutter) 48px;
  text-align: center;
  overflow: hidden;
}
.hero::before {
  content: '';
  position: absolute;
  inset: -200px 0 auto;
  height: 560px;
  background: radial-gradient(660px 320px at 50% 60%, var(--c-primary-100), transparent 70%);
  z-index: -1;
}
.hero__eyebrow {
  display: inline-block;
  padding: 6px 14px;
  border-radius: var(--r-full);
  background: var(--mat-fill);
  border: 1px solid var(--mat-hairline);
  color: var(--c-text-muted);
  font-size: var(--fs-sm);
  font-weight: 600;
  letter-spacing: var(--ls-caps);
}
.hero__title {
  margin-top: 22px;
  font-size: clamp(38px, 6vw, 62px);
  font-weight: 800;
  line-height: 1.12;
  letter-spacing: -0.032em;
}
.hero__title span {
  display: block;
}
.hero__title em {
  font-style: normal;
  background: linear-gradient(96deg, var(--c-primary-500), var(--c-danger-500));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.hero__desc {
  max-width: 34em;
  margin: 20px auto 0;
  color: var(--c-text-muted);
  font-size: clamp(15px, 1.4vw, 18px);
  line-height: 1.6;
}
.hero__cta {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 30px;
}

/* 제품 미리보기 */
.preview {
  position: relative;
  max-width: 940px;
  margin: clamp(40px, 6vw, 64px) auto 0;
  border: 1px solid var(--mat-hairline);
  border-radius: var(--r-2xl);
  background: var(--c-surface);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  text-align: left;
}
.preview__bar {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 14px;
  border-bottom: 1px solid var(--mat-hairline);
  background: var(--mat-fill);
}
.preview__bar span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--mat-hairline-strong);
}
.preview__bar b {
  margin-left: 10px;
  font-size: var(--fs-sm);
  color: var(--c-text-muted);
}
.preview__body {
  display: grid;
  grid-template-columns: 138px minmax(0, 1fr) 210px;
  height: 312px;
}
.preview__outline {
  border-right: 1px solid var(--mat-hairline);
  background: var(--mat-fill);
  padding: 14px 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.preview__outline i {
  display: block;
  height: 7px;
  border-radius: var(--r-full);
  background: var(--mat-hairline-strong);
}
.preview__outline li:nth-child(2) i {
  background: var(--c-primary-200);
  width: 80%;
}
.preview__paper {
  padding: 24px 28px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.line {
  display: block;
  height: 8px;
  border-radius: var(--r-full);
  background: var(--mat-hairline-strong);
}
.line--title {
  width: 45%;
  height: 12px;
  background: var(--c-border-strong);
}
.line--short {
  width: 62%;
}
.line--hl-error,
.line--hl-check {
  transform-origin: left center;
  animation: draw 620ms var(--ease-out) both;
}
.line--hl-error {
  width: 88%;
  background: var(--c-danger-200);
  animation-delay: 900ms;
}
.line--hl-check {
  width: 74%;
  background: var(--c-primary-200);
  animation-delay: 1080ms;
}
@keyframes draw {
  from {
    transform: scaleX(0);
    opacity: 0.4;
  }
}
.preview__panel {
  border-left: 1px solid var(--mat-hairline);
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.preview__counts {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
  margin-bottom: 4px;
  padding: 3px;
  border-radius: var(--r-md);
  background: var(--mat-fill);
}
.preview__counts b {
  padding: 6px 0;
  border-radius: var(--r-sm);
  background: var(--c-surface);
  box-shadow: var(--shadow-sm);
  text-align: center;
  font-size: var(--fs-md);
}
.preview__counts .is-error {
  color: var(--c-finding-error);
}
.preview__counts .is-check {
  color: var(--c-finding-check);
}
.preview__counts .is-evidence {
  color: var(--c-finding-evidence);
}
.preview__card {
  padding: 10px;
  border: 1px solid var(--mat-hairline);
  border-radius: var(--r-md);
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.chip {
  display: block;
  width: 34px;
  height: 12px;
  border-radius: var(--r-full);
}
.chip--1 {
  background: var(--c-finding-error-bg);
}
.chip--2 {
  background: var(--c-finding-check-bg);
}
.chip--3 {
  background: var(--c-finding-evidence-bg);
}

/* 문서를 훑고 지나가는 스캔 라인 */
.preview__scan {
  position: absolute;
  left: 0;
  right: 0;
  top: 36px;
  height: 34%;
  pointer-events: none;
  background: linear-gradient(
    to bottom,
    transparent,
    rgba(255, 106, 0, 0.06) 60%,
    rgba(255, 106, 0, 0.16) 92%,
    transparent
  );
  border-bottom: 1px solid rgba(255, 106, 0, 0.5);
  animation: scan 5s cubic-bezier(0.55, 0, 0.45, 1) 1.4s infinite;
}
@keyframes scan {
  0% {
    transform: translateY(-40%);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  80% {
    opacity: 1;
  }
  100% {
    transform: translateY(255%);
    opacity: 0;
  }
}

.preview__float {
  position: absolute;
  padding: 9px 13px;
  border-radius: var(--r-full);
  background: var(--mat-card);
  backdrop-filter: blur(var(--mat-blur)) saturate(var(--mat-saturate));
  -webkit-backdrop-filter: blur(var(--mat-blur)) saturate(var(--mat-saturate));
  border: 1px solid var(--mat-hairline);
  box-shadow: var(--shadow-inner-top), var(--shadow-md);
  font-size: var(--fs-sm);
  font-weight: 600;
  white-space: nowrap;
  animation: float 6s ease-in-out infinite;
}
.preview__float--left {
  left: 18px;
  bottom: 44px;
  color: var(--c-danger-600);
}
.preview__float--right {
  right: 18px;
  bottom: 104px;
  color: var(--c-primary-700);
  animation-delay: 1.2s;
}
@keyframes float {
  50% {
    transform: translateY(-7px);
  }
}

/* ---------------- 공통 섹션 ---------------- */
.section {
  max-width: var(--content-max);
  margin: 0 auto;
  padding: clamp(72px, 9vw, 112px) var(--gutter);
  text-align: center;
}
.section--subtle {
  max-width: none;
  background: var(--c-bg-subtle);
}
.section--subtle > * {
  max-width: var(--content-max);
  margin-inline: auto;
}
.section__title {
  font-size: clamp(24px, 3.4vw, 38px);
  font-weight: 800;
  line-height: 1.32;
  letter-spacing: -0.026em;
  text-wrap: balance;
}
.section__title em {
  font-style: normal;
  color: var(--c-primary-600);
}

/* 문제 제기 */
.pains {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 40px;
}
.pains__item {
  width: 186px;
  height: 186px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 18px;
  background: var(--c-danger-50);
  color: var(--c-danger-700);
  font-size: var(--fs-md);
  font-weight: 600;
  line-height: 1.6;
  white-space: pre-line;
  transition: transform var(--dur) var(--ease-spring);
}
.pains__item:hover {
  transform: translateY(-4px) scale(1.02);
}
.pains__item:last-child {
  background: var(--c-primary-50);
  color: var(--c-primary-700);
}
.pains__icon {
  font-size: 24px;
}
.dots {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  margin: 36px 0;
}
.dots i {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--c-text-subtle);
  animation: blink 1.8s ease-in-out infinite;
}
.dots i:nth-child(2) {
  animation-delay: 0.2s;
}
.dots i:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes blink {
  0%,
  100% {
    opacity: 0.25;
  }
  50% {
    opacity: 1;
  }
}

/* 가치 카드 */
.values {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 44px;
}
.values__card {
  padding: 30px 22px;
  border: 1px solid var(--mat-hairline);
  border-radius: var(--r-xl);
  background: var(--mat-card);
  transition:
    transform var(--dur) var(--ease-spring),
    box-shadow var(--dur) var(--ease-out),
    border-color var(--transition);
}
.values__card:hover {
  transform: translateY(-6px);
  border-color: var(--mat-hairline-strong);
  box-shadow: var(--shadow-md);
}
.values__icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  margin: 0 auto 14px;
  border-radius: var(--r-md);
  background: var(--c-primary-50);
  color: var(--c-primary-600);
  font-size: 18px;
}
.values__card h3 {
  font-size: var(--fs-lg);
  font-weight: 700;
}
.values__card p {
  margin-top: 8px;
  font-size: var(--fs-md);
  color: var(--c-text-muted);
  line-height: 1.65;
}

/* ---------------- 기능 밴드 + 캐러셀 ---------------- */
.band {
  max-width: calc(var(--content-max) - 24px);
  margin: 0 auto;
  padding: clamp(48px, 6vw, 72px) 0 clamp(40px, 5vw, 60px);
  border-radius: var(--r-2xl);
  color: #fff;
  background:
    radial-gradient(680px 320px at 15% 0%, rgba(255, 255, 255, 0.16), transparent 70%),
    linear-gradient(135deg, #17181c 0%, #2a1b14 55%, #47210d 100%);
  overflow: hidden;
}
.band__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding: 0 clamp(24px, 4vw, 48px);
  text-align: left;
}
.band__eyebrow {
  font-size: var(--fs-xs);
  font-weight: 700;
  letter-spacing: var(--ls-caps);
  color: var(--c-primary-400);
}
.band__title {
  margin-top: 8px;
  font-size: clamp(24px, 3.2vw, 36px);
  font-weight: 800;
  letter-spacing: -0.026em;
}
.band__desc {
  margin-top: 10px;
  font-size: var(--fs-base);
  color: rgba(255, 255, 255, 0.66);
}
.band__nav {
  display: flex;
  gap: 8px;
  flex: none;
}
.band__nav button {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.24);
  color: #fff;
  font-size: 18px;
  line-height: 1;
  transition:
    background var(--transition),
    opacity var(--transition),
    transform var(--dur-fast) var(--ease-spring);
}
.band__nav button:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.14);
}
.band__nav button:active:not(:disabled) {
  transform: scale(0.92);
}
.band__nav button:disabled {
  opacity: 0.28;
  cursor: not-allowed;
}

.f-track {
  display: flex;
  gap: 16px;
  margin-top: 32px;
  padding: 4px clamp(24px, 4vw, 48px) 20px;
  overflow-x: auto;
  scroll-behavior: smooth;
  scroll-snap-type: x proximity;
  /* 스냅 기준을 좌우 패딩 안쪽으로 맞춥니다 (없으면 첫 카드가 가장자리에 붙습니다) */
  scroll-padding-inline: clamp(24px, 4vw, 48px);
  scrollbar-width: none;
}
.f-track::-webkit-scrollbar {
  display: none;
}
.f-track:focus-visible {
  outline: 2px solid var(--c-primary-400);
  outline-offset: -2px;
}
.f-card {
  position: relative;
  flex: 0 0 clamp(230px, 25vw, 292px);
  min-height: 232px;
  padding: 26px 24px;
  border-radius: var(--r-xl);
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.12);
  text-align: left;
  scroll-snap-align: start;
  transition:
    background var(--dur) var(--ease-out),
    transform var(--dur) var(--ease-spring);
}
.f-card:hover {
  background: rgba(255, 255, 255, 0.13);
  transform: translateY(-6px);
}
.f-card__tag {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: var(--ls-caps);
  color: var(--c-primary-400);
}
.f-card__title {
  margin-top: 12px;
  font-size: var(--fs-xl);
  font-weight: 700;
  letter-spacing: -0.02em;
}
.f-card__desc {
  margin-top: 10px;
  font-size: var(--fs-md);
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.68);
}
.f-card__index {
  position: absolute;
  right: 20px;
  bottom: 18px;
  font-size: var(--fs-sm);
  font-weight: 700;
  color: rgba(255, 255, 255, 0.28);
  font-variant-numeric: tabular-nums;
}
.band__foot {
  display: flex;
  justify-content: center;
  padding: 8px clamp(24px, 4vw, 48px) 0;
}
/* 어두운 밴드 위에 놓이므로 글자와 테두리를 흰색 계열로 올립니다 */
.band__foot :deep(.btn--secondary) {
  color: #fff;
  background: rgba(255, 255, 255, 0.14);
  border-color: rgba(255, 255, 255, 0.26);
}
.band__foot :deep(.btn--secondary:hover:not(:disabled)) {
  background: rgba(255, 255, 255, 0.22);
}

/* ---------------- 제품 사실 ---------------- */
.facts {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  max-width: var(--content-max);
  margin: 0 auto;
  padding: clamp(64px, 7vw, 88px) var(--gutter);
  text-align: center;
}
.facts__item b {
  display: block;
  font-size: clamp(22px, 2.4vw, 30px);
  font-weight: 800;
  letter-spacing: -0.026em;
  color: var(--c-primary-600);
}
.facts__item span {
  display: block;
  margin-top: 8px;
  font-size: var(--fs-md);
  color: var(--c-text-muted);
}
.facts__item em {
  display: block;
  margin-top: 10px;
  font-style: normal;
  font-size: var(--fs-xs);
  font-weight: 700;
  letter-spacing: var(--ls-caps);
  color: var(--c-text-subtle);
}

/* ---------------- 이용 흐름 ---------------- */
.steps {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 44px;
  text-align: left;
}
.steps__item {
  position: relative;
  padding: 28px 24px;
  border: 1px solid var(--mat-hairline);
  border-radius: var(--r-xl);
  background: var(--mat-card);
  transition:
    transform var(--dur) var(--ease-spring),
    box-shadow var(--dur) var(--ease-out);
}
.steps__item:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-md);
}
.steps__no {
  display: inline-block;
  font-size: var(--fs-sm);
  font-weight: 800;
  letter-spacing: var(--ls-caps);
  color: var(--c-primary-500);
  font-variant-numeric: tabular-nums;
}
.steps__item h3 {
  margin-top: 10px;
  font-size: var(--fs-lg);
  font-weight: 700;
}
.steps__item p {
  margin-top: 8px;
  font-size: var(--fs-md);
  color: var(--c-text-muted);
  line-height: 1.65;
}

/* ---------------- 페르소나 ---------------- */
.personas {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-top: 44px;
  text-align: left;
}
.personas__card {
  padding: 30px 26px;
  border: 1px solid var(--mat-hairline);
  border-radius: var(--r-xl);
  background: var(--c-surface);
  transition:
    transform var(--dur) var(--ease-spring),
    box-shadow var(--dur) var(--ease-out);
}
.personas__card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-md);
}
.personas__card h3 {
  font-size: var(--fs-lg);
  font-weight: 700;
  line-height: 1.4;
  white-space: pre-line;
  color: var(--c-primary-700);
}
.personas__card p {
  margin-top: 12px;
  font-size: var(--fs-md);
  color: var(--c-text-muted);
  line-height: 1.75;
}

/* ---------------- FAQ ---------------- */
.faq {
  max-width: 760px;
  margin: 36px auto 0;
  text-align: left;
}
.faq__item {
  border: 1px solid var(--mat-hairline);
  border-radius: var(--r-lg);
  background: var(--mat-card);
  margin-bottom: 10px;
  overflow: hidden;
  transition: background var(--transition);
}
.faq__item:hover {
  background: var(--mat-fill-strong);
}
.faq__item summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 17px 20px;
  cursor: pointer;
  font-size: var(--fs-base);
  font-weight: 600;
  list-style: none;
}
.faq__item summary::-webkit-details-marker {
  display: none;
}
.faq__item summary b {
  color: var(--c-primary-600);
  margin-right: 6px;
}
.faq__item summary i {
  font-style: normal;
  color: var(--c-text-subtle);
  transition: transform var(--dur) var(--ease-spring);
}
.faq__item[open] summary i {
  transform: rotate(180deg);
}
.faq__item p {
  padding: 0 20px 18px;
  font-size: var(--fs-md);
  color: var(--c-text-muted);
  line-height: 1.8;
  animation: unfold 240ms var(--ease-out);
}
@keyframes unfold {
  from {
    opacity: 0;
    transform: translateY(-6px);
  }
}

/* ---------------- 클로징 ---------------- */
.closing {
  padding: clamp(80px, 9vw, 112px) var(--gutter);
  text-align: center;
  background:
    radial-gradient(560px 260px at 50% 100%, var(--c-primary-100), transparent 70%),
    var(--c-bg);
}
.closing h2 {
  font-size: clamp(26px, 3.6vw, 40px);
  font-weight: 800;
  letter-spacing: -0.028em;
}
.closing p {
  margin-top: 12px;
  color: var(--c-text-muted);
  font-size: var(--fs-base);
}
.closing__cta {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 28px;
}

/* ---------------- 떠오르는 시작 버튼 ---------------- */
.fab {
  position: fixed;
  right: clamp(16px, 3vw, 32px);
  bottom: clamp(16px, 3vw, 32px);
  z-index: 50;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 52px;
  padding: 0 22px;
  border-radius: var(--r-full);
  background: var(--c-primary-500);
  color: #fff;
  font-size: var(--fs-base);
  font-weight: 700;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.28),
    0 10px 28px rgba(0, 0, 0, 0.22);
  transition:
    background var(--transition),
    transform var(--dur-fast) var(--ease-spring);
}
.fab:hover {
  background: var(--c-primary-600);
}
.fab:active {
  transform: scale(0.95);
}
.fab i {
  transition: transform var(--dur) var(--ease-spring);
}
.fab:hover i {
  transform: translateX(3px);
}
.fab-enter-active {
  transition:
    opacity var(--dur) var(--ease-out),
    transform var(--dur) var(--ease-spring);
}
.fab-leave-active {
  transition:
    opacity var(--dur-fast) var(--ease-out),
    transform var(--dur-fast) var(--ease-out);
}
.fab-enter-from,
.fab-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.9);
}

/* ---------------- 반응형 ---------------- */
@media (max-width: 1000px) {
  .facts {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 760px) {
  .preview__body {
    grid-template-columns: 96px minmax(0, 1fr);
    height: 250px;
  }
  .preview__panel,
  .preview__float {
    display: none;
  }
  .values,
  .steps,
  .personas {
    grid-template-columns: 1fr;
  }
  .band__head {
    flex-direction: column;
    align-items: flex-start;
  }
  .pains {
    flex-wrap: wrap;
  }
  .hero__cta,
  .closing__cta {
    flex-direction: column;
  }
}

/* ---------------- 모션 최소화 (DESIGN 8.4) ---------------- */
@media (prefers-reduced-motion: reduce) {
  .f-track {
    scroll-behavior: auto;
  }
  [data-reveal] {
    opacity: 1;
    transform: none;
    transition: none;
  }
  .preview__scan,
  .dots i,
  .preview__float,
  .line--hl-error,
  .line--hl-check {
    animation: none;
  }
  .preview__scan {
    display: none;
  }
}
</style>
