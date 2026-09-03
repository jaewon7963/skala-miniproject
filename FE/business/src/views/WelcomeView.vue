<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppButton from '@/components/common/AppButton.vue'

/**
 * 웰컴(랜딩) 페이지 — 미로그인 방문자의 첫 화면
 * 참고: 유스케이스 AUTH-01은 세션 분기만 정의하고 있어, 소개 페이지는 이 화면으로 추가했습니다.
 */
const router = useRouter()
const auth = useAuthStore()

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
  { title: '수치 검산', desc: '합계 · 단가 × 수량 · 인원 × 기간 자동 검산' },
  { title: '항목 간 모순', desc: '고객 · 채널 · 수익모델 · 매출계획 연결 검증' },
  { title: '주장–근거 연결', desc: '근거가 없는 주장과 출처 누락 탐지' },
  { title: '기술 · KPI 적합성', desc: '기능–기술–운영환경, KPI–평가방법 교차 검토' },
  { title: '검토 의견서', desc: '판정한 항목만 모아 수정 지시로 내보내기' },
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

const start = () =>
  router.push({ name: auth.isAuthenticated ? 'library' : 'signup' })
</script>

<template>
  <div class="welcome">
    <!-- ================= HERO ================= -->
    <section class="hero">
      <span class="hero__eyebrow">AI 기반 사업계획서 검토 플랫폼</span>
      <h1 class="hero__title">
        사업계획서의<br />
        <em>흩어진 근거를</em><br />
        연결합니다
      </h1>
      <p class="hero__desc">
        수치 · 주장 · 기술 · KPI를 이어 붙여, 검토자가 확인해야 할 지점을 근거와 함께 선별합니다
      </p>

      <div class="hero__cta">
        <AppButton size="lg" @click="start">무료로 시작하기</AppButton>
        <AppButton size="lg" variant="secondary" @click="router.push({ name: 'login' })">
          로그인
        </AppButton>
      </div>

      <!-- 제품 미리보기 (검토 화면 축소 재현) -->
      <div class="preview">
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
        <div class="preview__float preview__float--left">p.11 매출 합계 불일치 · 3,200만 원</div>
        <div class="preview__float preview__float--right">확신도 96% · 결정적 검산</div>
      </div>
    </section>

    <!-- ================= PROBLEM ================= -->
    <section id="problem" class="section">
      <h2 class="section__title">
        사업계획서 검토, 아직도 <em>문서를 오가며</em> 하고 계신가요?
      </h2>

      <div class="pains">
        <div v-for="pain in pains" :key="pain.text" class="pains__item">
          <span class="pains__icon">{{ pain.icon }}</span>
          <p>{{ pain.text }}</p>
        </div>
      </div>

      <div class="dots" aria-hidden="true"><i /><i /><i /></div>

      <h2 class="section__title">
        AI의 결론만 받아들이지 말고<br />
        <em>근거까지 확인하는 BizXray</em>와 함께하세요
      </h2>

      <div class="values">
        <article v-for="value in values" :key="value.title" class="values__card">
          <span class="values__icon">{{ value.icon }}</span>
          <h3>{{ value.title }}</h3>
          <p>{{ value.desc }}</p>
        </article>
      </div>
    </section>

    <!-- ================= FEATURES ================= -->
    <section id="features" class="band">
      <h2 class="band__title">검토가 쉬워지는 BizXray 기능들</h2>
      <p class="band__desc">원문 뷰어와 검토 항목을 한 화면에 붙여 확인 시간을 줄입니다</p>

      <ul class="tiles">
        <li v-for="feature in features" :key="feature.title" class="tiles__item">
          <h3>{{ feature.title }}</h3>
          <p>{{ feature.desc }}</p>
        </li>
      </ul>

      <AppButton size="lg" variant="secondary" @click="start">무료로 시작하기</AppButton>
    </section>

    <!-- ================= FACTS ================= -->
    <section class="facts">
      <div v-for="fact in facts" :key="fact.caption" class="facts__item">
        <b>{{ fact.value }}</b>
        <span>{{ fact.label }}</span>
        <em>{{ fact.caption }}</em>
      </div>
    </section>

    <!-- ================= FLOW ================= -->
    <section id="flow" class="section">
      <h2 class="section__title">업로드부터 <em>검토 의견서</em>까지 세 단계</h2>

      <ol class="steps">
        <li v-for="step in steps" :key="step.no" class="steps__item">
          <span class="steps__no">{{ step.no }}</span>
          <h3>{{ step.title }}</h3>
          <p>{{ step.desc }}</p>
        </li>
      </ol>
    </section>

    <!-- ================= PERSONA ================= -->
    <section class="section section--subtle">
      <h2 class="section__title">이런 분들의 검토를 돕습니다</h2>

      <div class="personas">
        <article v-for="persona in personas" :key="persona.title" class="personas__card">
          <h3>{{ persona.title }}</h3>
          <p>{{ persona.desc }}</p>
        </article>
      </div>
    </section>

    <!-- ================= FAQ ================= -->
    <section id="faq" class="section">
      <h2 class="section__title">자주 묻는 질문</h2>

      <div class="faq">
        <details v-for="faq in faqs" :key="faq.q" class="faq__item">
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
      <h2>검토해야 할 지점을 먼저 확인하세요</h2>
      <p>계정을 만들면 사업계획서를 바로 올릴 수 있습니다</p>
      <div class="closing__cta">
        <AppButton size="lg" @click="start">무료로 시작하기</AppButton>
        <AppButton size="lg" variant="ghost" @click="router.push({ name: 'login' })">
          이미 계정이 있어요
        </AppButton>
      </div>
    </section>
  </div>
</template>

<style scoped>
.welcome {
  --gutter: 24px;
}
section {
  scroll-margin-top: 72px;
}

/* ---------------- HERO ---------------- */
.hero {
  position: relative;
  max-width: var(--content-max);
  margin: 0 auto;
  padding: 72px var(--gutter) 40px;
  text-align: center;
  overflow: hidden;
}
.hero::before {
  content: '';
  position: absolute;
  inset: -180px 0 auto;
  height: 520px;
  background: radial-gradient(620px 300px at 50% 60%, var(--c-primary-50), transparent 70%);
  z-index: -1;
}
.hero__eyebrow {
  display: inline-block;
  padding: 5px 12px;
  border-radius: var(--r-full);
  background: var(--c-primary-50);
  color: var(--c-primary-700);
  font-size: var(--fs-sm);
  font-weight: 700;
}
.hero__title {
  margin-top: 18px;
  font-size: clamp(32px, 5.4vw, 52px);
  line-height: 1.25;
  letter-spacing: -0.03em;
}
.hero__title em {
  font-style: normal;
  background: linear-gradient(90deg, var(--c-primary-500), var(--c-danger-500));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.hero__desc {
  margin-top: 16px;
  color: var(--c-text-muted);
  font-size: var(--fs-lg);
}
.hero__cta {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 26px;
}

/* 제품 미리보기 */
.preview {
  position: relative;
  max-width: 900px;
  margin: 46px auto 0;
  border: 1px solid var(--c-border);
  border-radius: var(--r-lg);
  background: var(--c-surface);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  text-align: left;
}
.preview__bar {
  display: flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 12px;
  border-bottom: 1px solid var(--c-border);
  background: var(--c-bg-subtle);
}
.preview__bar span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--c-border-strong);
}
.preview__bar b {
  margin-left: 10px;
  font-size: var(--fs-sm);
  color: var(--c-text-muted);
}
.preview__body {
  display: grid;
  grid-template-columns: 130px minmax(0, 1fr) 200px;
  height: 300px;
}
.preview__outline {
  border-right: 1px solid var(--c-border);
  background: var(--c-bg-subtle);
  padding: 12px 10px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.preview__outline i {
  display: block;
  height: 7px;
  border-radius: var(--r-full);
  background: var(--c-border);
}
.preview__outline li:nth-child(2) i {
  background: var(--c-primary-200);
  width: 80%;
}
.preview__paper {
  padding: 22px 26px;
  display: flex;
  flex-direction: column;
  gap: 11px;
}
.line {
  display: block;
  height: 8px;
  border-radius: var(--r-full);
  background: var(--c-border);
}
.line--title {
  width: 45%;
  height: 12px;
  background: var(--c-border-strong);
}
.line--short {
  width: 62%;
}
.line--hl-error {
  width: 88%;
  background: var(--c-danger-200);
}
.line--hl-check {
  width: 74%;
  background: var(--c-primary-200);
}
.preview__panel {
  border-left: 1px solid var(--c-border);
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.preview__counts {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 5px;
  margin-bottom: 4px;
}
.preview__counts b {
  padding: 6px 0;
  border: 1px solid var(--c-border);
  border-radius: var(--r-sm);
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
  padding: 9px;
  border: 1px solid var(--c-border);
  border-radius: var(--r-sm);
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.chip {
  display: block;
  width: 34px;
  height: 12px;
  border-radius: var(--r-sm);
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
.preview__float {
  position: absolute;
  padding: 8px 11px;
  border-radius: var(--r-md);
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  box-shadow: var(--shadow-md);
  font-size: var(--fs-sm);
  font-weight: 600;
  white-space: nowrap;
}
.preview__float--left {
  left: 18px;
  bottom: 44px;
  color: var(--c-danger-600);
}
.preview__float--right {
  right: 18px;
  bottom: 96px;
  color: var(--c-primary-700);
}

/* ---------------- 공통 섹션 ---------------- */
.section {
  max-width: var(--content-max);
  margin: 0 auto;
  padding: 84px var(--gutter);
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
  font-size: clamp(22px, 3.2vw, 32px);
  line-height: 1.4;
  letter-spacing: -0.02em;
}
.section__title em {
  font-style: normal;
  color: var(--c-primary-600);
}

/* 문제 제기 */
.pains {
  display: flex;
  justify-content: center;
  gap: 18px;
  margin-top: 34px;
}
.pains__item {
  width: 178px;
  height: 178px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  background: var(--c-danger-50);
  color: var(--c-danger-700);
  font-size: var(--fs-md);
  font-weight: 600;
  line-height: 1.6;
  white-space: pre-line;
}
.pains__item:last-child {
  background: var(--c-primary-50);
  color: var(--c-primary-700);
}
.pains__icon {
  font-size: 22px;
}
.dots {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  margin: 32px 0;
}
.dots i {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--c-border-strong);
}

/* 가치 카드 */
.values {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-top: 36px;
}
.values__card {
  padding: 26px 20px;
  border: 1px solid var(--c-border);
  border-radius: var(--r-lg);
  background: var(--c-surface);
  transition: border-color var(--transition);
}
.values__card:hover {
  border-color: var(--c-primary-300);
}
.values__icon {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  margin: 0 auto 12px;
  border-radius: var(--r-md);
  background: var(--c-primary-50);
  color: var(--c-primary-600);
  font-size: 17px;
}
.values__card h3 {
  font-size: var(--fs-lg);
}
.values__card p {
  margin-top: 6px;
  font-size: var(--fs-md);
  color: var(--c-text-muted);
  line-height: 1.6;
}

/* ---------------- 기능 밴드 ---------------- */
.band {
  max-width: calc(var(--content-max) - 24px);
  margin: 0 auto;
  padding: 66px 32px 58px;
  border-radius: 28px;
  text-align: center;
  color: #fff;
  background:
    radial-gradient(680px 320px at 50% 0%, rgba(255, 255, 255, 0.18), transparent 70%),
    linear-gradient(135deg, var(--c-primary-600) 0%, var(--c-danger-600) 100%);
}
.band__title {
  font-size: clamp(22px, 3.2vw, 32px);
  letter-spacing: -0.02em;
}
.band__desc {
  margin-top: 8px;
  font-size: var(--fs-base);
  color: rgba(255, 255, 255, 0.82);
}
.tiles {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  margin: 34px 0 30px;
}
.tiles__item {
  padding: 18px 14px;
  border-radius: var(--r-lg);
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
  text-align: left;
}
.tiles__item h3 {
  font-size: var(--fs-base);
}
.tiles__item p {
  margin-top: 6px;
  font-size: var(--fs-sm);
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.55;
}

/* ---------------- 제품 사실 ---------------- */
.facts {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  max-width: var(--content-max);
  margin: 0 auto;
  padding: 64px var(--gutter);
  text-align: center;
}
.facts__item b {
  display: block;
  font-size: 26px;
  letter-spacing: -0.02em;
  color: var(--c-primary-600);
}
.facts__item span {
  display: block;
  margin-top: 6px;
  font-size: var(--fs-md);
  color: var(--c-text-muted);
}
.facts__item em {
  display: block;
  margin-top: 8px;
  font-style: normal;
  font-size: var(--fs-xs);
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--c-text-subtle);
}

/* ---------------- 이용 흐름 ---------------- */
.steps {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-top: 36px;
  text-align: left;
}
.steps__item {
  padding: 24px 20px;
  border: 1px solid var(--c-border);
  border-radius: var(--r-lg);
  background: var(--c-surface);
}
.steps__no {
  display: inline-block;
  font-size: var(--fs-sm);
  font-weight: 800;
  color: var(--c-primary-500);
  letter-spacing: 0.06em;
}
.steps__item h3 {
  margin-top: 8px;
  font-size: var(--fs-lg);
}
.steps__item p {
  margin-top: 6px;
  font-size: var(--fs-md);
  color: var(--c-text-muted);
  line-height: 1.6;
}

/* ---------------- 페르소나 ---------------- */
.personas {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-top: 36px;
  text-align: left;
}
.personas__card {
  padding: 26px 22px;
  border: 1px solid var(--c-border);
  border-radius: var(--r-lg);
  background: var(--c-surface);
}
.personas__card h3 {
  font-size: var(--fs-lg);
  line-height: 1.4;
  white-space: pre-line;
  color: var(--c-primary-700);
}
.personas__card p {
  margin-top: 10px;
  font-size: var(--fs-md);
  color: var(--c-text-muted);
  line-height: 1.7;
}

/* ---------------- FAQ ---------------- */
.faq {
  max-width: 760px;
  margin: 30px auto 0;
  text-align: left;
}
.faq__item {
  border: 1px solid var(--c-border);
  border-radius: var(--r-md);
  background: var(--c-bg-subtle);
  margin-bottom: 8px;
  overflow: hidden;
}
.faq__item summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 15px 18px;
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
  transition: transform var(--transition);
}
.faq__item[open] summary i {
  transform: rotate(180deg);
}
.faq__item p {
  padding: 0 18px 16px;
  font-size: var(--fs-md);
  color: var(--c-text-muted);
  line-height: 1.75;
}

/* ---------------- 클로징 ---------------- */
.closing {
  padding: 86px var(--gutter);
  text-align: center;
  background:
    radial-gradient(560px 260px at 50% 100%, var(--c-primary-50), transparent 70%),
    var(--c-bg);
}
.closing h2 {
  font-size: clamp(24px, 3.4vw, 34px);
  letter-spacing: -0.02em;
}
.closing p {
  margin-top: 10px;
  color: var(--c-text-muted);
  font-size: var(--fs-base);
}
.closing__cta {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 24px;
}

/* ---------------- 반응형 ---------------- */
@media (max-width: 1000px) {
  .tiles {
    grid-template-columns: repeat(2, 1fr);
  }
  .facts {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 760px) {
  .preview__body {
    grid-template-columns: 90px minmax(0, 1fr);
    height: 240px;
  }
  .preview__panel,
  .preview__float {
    display: none;
  }
  .values,
  .steps,
  .personas,
  .tiles {
    grid-template-columns: 1fr;
  }
  .pains {
    flex-wrap: wrap;
  }
  .hero__cta,
  .closing__cta {
    flex-direction: column;
  }
}
</style>
