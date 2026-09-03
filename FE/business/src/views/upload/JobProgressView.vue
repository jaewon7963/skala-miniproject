<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useReviewStore } from '@/stores/review'
import { useUiStore } from '@/stores/ui'
import { JOB_STATUS } from '@/constants/enums'
import AppButton from '@/components/common/AppButton.vue'
import FindingTypeBadge from '@/components/common/FindingTypeBadge.vue'
import ProgressBar from '@/components/common/ProgressBar.vue'

/** UP-02 파싱 · 구조화 → ANL-01/02 검증 파이프라인 (F-02, F-15, F-16) */
const props = defineProps({ jobId: { type: String, required: true } })

const router = useRouter()
const ui = useUiStore()
const review = useReviewStore()
const { job } = storeToRefs(review)

let alive = true
const failed = ref('')

const isParsing = computed(() => job.value?.phase === 'PARSE')
const isDone = computed(() => job.value?.status === JOB_STATUS.DONE)
const discovered = computed(() => [...(job.value?.discovered ?? [])].reverse())

onMounted(async () => {
  try {
    await review.pollJob(props.jobId, {
      onTick: () => {
        if (!alive) throw new Error('cancelled')
      },
    })
    if (alive) ui.success('분석을 완료했습니다')
  } catch (e) {
    if (alive && e.message !== 'cancelled') failed.value = e.message
  }
})

onUnmounted(() => {
  alive = false
})

function openReview() {
  router.push({ name: 'review', params: { jobId: props.jobId } })
}
</script>

<template>
  <div class="progress">
    <header class="head">
      <h1 class="head__title">{{ isParsing ? '문서를 구조화하고 있습니다' : '검증 파이프라인 실행 중' }}</h1>
      <p class="head__desc">
        {{
          isParsing
            ? '목차 · 표 · 수치 · 주장 · KPI를 페이지 위치와 함께 추출합니다'
            : '결정적 검산과 관계 판단이 병렬로 실행되며, 발견되는 대로 항목이 쌓입니다'
        }}
      </p>
    </header>

    <p v-if="failed" class="alert">{{ failed }}</p>

    <!-- 파싱 단계 -->
    <section class="card u-card">
      <div class="card__head">
        <h2>파싱 · 구조화</h2>
        <span class="card__pct">{{ job?.parseProgress ?? 0 }}%</span>
      </div>
      <ProgressBar :value="job?.parseProgress ?? 0" />
      <ol class="steps">
        <li v-for="step in job?.steps ?? []" :key="step.key" :class="`is-${step.state.toLowerCase()}`">
          <span class="steps__dot" aria-hidden="true" />
          <div>
            <p class="steps__label">{{ step.label }}</p>
            <p class="steps__detail">{{ step.detail }}</p>
          </div>
        </li>
      </ol>

      <div v-for="failure in job?.partialFailures ?? []" :key="failure.page" class="partial">
        <b>부분 실패 1건</b>
        <span>p.{{ failure.page }} {{ failure.reason }}</span>
      </div>
    </section>

    <!-- 분석 단계 -->
    <section class="card u-card">
      <div class="card__head">
        <h2>결정적 수치 검산 → RAG 관계 판단</h2>
        <span class="card__pct">{{ job?.analyzeProgress ?? 0 }}%</span>
      </div>
      <ProgressBar :value="job?.analyzeProgress ?? 0" />

      <ul v-if="discovered.length" class="feed">
        <li v-for="finding in discovered" :key="finding.id">
          <FindingTypeBadge :type="finding.type" />
          <span class="feed__page">p.{{ finding.page }}</span>
          <span class="feed__title">{{ finding.title }}</span>
        </li>
      </ul>
      <p v-else class="feed__empty">
        {{ isParsing ? '파싱이 끝나면 검증이 시작됩니다' : '항목을 찾는 중입니다…' }}
      </p>
    </section>

    <footer class="foot">
      <span class="foot__count">
        {{ isDone ? `${job?.summary?.total ?? 0}개 항목` : '분석 중' }}
      </span>
      <AppButton size="lg" :disabled="!isDone" @click="openReview">검토 화면 열기</AppButton>
    </footer>
  </div>
</template>

<style scoped>
.progress {
  max-width: 760px;
  margin: 0 auto;
}
.head {
  margin-bottom: 18px;
}
.head__title {
  font-size: var(--fs-xl);
  letter-spacing: -0.02em;
}
.head__desc {
  color: var(--c-text-muted);
  font-size: var(--fs-md);
  margin-top: 2px;
}
.alert {
  padding: 10px 12px;
  border-radius: var(--r-md);
  background: var(--c-danger-50);
  color: var(--c-danger-600);
  font-size: var(--fs-md);
  margin-bottom: 14px;
}
.card {
  padding: 18px;
  margin-bottom: 14px;
}
.card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.card__head h2 {
  font-size: var(--fs-base);
}
.card__pct {
  font-size: var(--fs-sm);
  font-weight: 700;
  color: var(--c-primary-600);
}

.steps {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.steps li {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  color: var(--c-text-subtle);
}
.steps__dot {
  width: 8px;
  height: 8px;
  margin-top: 6px;
  border-radius: 50%;
  background: var(--c-border-strong);
  flex: none;
}
.steps li.is-done {
  color: var(--c-text);
}
.steps li.is-done .steps__dot {
  background: var(--c-primary-500);
}
.steps li.is-running {
  color: var(--c-text);
}
.steps li.is-running .steps__dot {
  background: var(--c-primary-500);
  box-shadow: 0 0 0 4px var(--c-primary-100);
  animation: pulse 1.1s ease-in-out infinite;
}
@keyframes pulse {
  50% {
    opacity: 0.45;
  }
}
.steps__label {
  font-size: var(--fs-md);
  font-weight: 600;
}
.steps__detail {
  font-size: var(--fs-sm);
  color: var(--c-text-subtle);
}

.partial {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 14px;
  padding: 10px 12px;
  border-radius: var(--r-md);
  background: var(--c-danger-50);
  font-size: var(--fs-sm);
  color: var(--c-danger-700);
}
.partial b {
  white-space: nowrap;
}

.feed {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.feed li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: var(--r-md);
  background: var(--c-bg-subtle);
  font-size: var(--fs-md);
}
.feed__page {
  font-size: var(--fs-sm);
  color: var(--c-text-subtle);
  white-space: nowrap;
}
.feed__title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.feed__empty {
  margin-top: 12px;
  font-size: var(--fs-md);
  color: var(--c-text-subtle);
}

.foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}
.foot__count {
  font-size: var(--fs-md);
  color: var(--c-text-muted);
}
</style>
