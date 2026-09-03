<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { documentApi } from '@/api'
import { applyName } from '@/utils/documentNames'
import { useReviewStore } from '@/stores/review'
import { useUiStore } from '@/stores/ui'
import AppButton from '@/components/common/AppButton.vue'
import BrandMark from '@/components/common/BrandMark.vue'
import DocumentViewer from '@/components/review/DocumentViewer.vue'
import FindingList from '@/components/review/FindingList.vue'
import SectionOutline from '@/components/review/SectionOutline.vue'
import AskPanel from '@/components/review/AskPanel.vue'
import AnnotationList from '@/components/review/AnnotationList.vue'

/** REV-01 검토 화면 (3분할) · SHR-01 검토 완료 */
const props = defineProps({ jobId: { type: String, required: true } })

const router = useRouter()
const ui = useUiStore()
const review = useReviewStore()
const { findings, decidedCount, loading, error, showEvidence, panelWidth, panelTab } = storeToRefs(review)
const completing = ref(false)

/* ---------------- 사이드바 너비 드래그 ---------------- */
const resizing = ref(null)
let stopResize = null

function startResize(target, event) {
  event.preventDefault()
  resizing.value = target

  const startX = event.clientX
  const startOutline = review.outlineWidth
  const startPanel = review.panelWidth

  const onMove = (moveEvent) => {
    const delta = moveEvent.clientX - startX
    if (target === 'outline') review.setOutlineWidth(startOutline + delta)
    else review.setPanelWidth(startPanel - delta)
  }

  stopResize = () => {
    resizing.value = null
    review.persistLayout()
    document.body.classList.remove('is-resizing')
    window.removeEventListener('pointermove', onMove)
    window.removeEventListener('pointerup', stopResize)
    stopResize = null
  }

  document.body.classList.add('is-resizing')
  window.addEventListener('pointermove', onMove)
  window.addEventListener('pointerup', stopResize)
}

const documentTitle = ref('문서')

onMounted(async () => {
  await review.load(props.jobId)
  if (review.job?.documentId) {
    const doc = applyName(await documentApi.get(review.job.documentId))
    documentTitle.value = doc.name
  }
})
onUnmounted(() => {
  stopResize?.()
  review.reset()
})

async function complete() {
  completing.value = true
  try {
    await review.complete()
    router.push({ name: 'review-done', params: { jobId: props.jobId } })
  } catch (e) {
    ui.error(e.message)
  } finally {
    completing.value = false
  }
}
</script>

<template>
  <!-- 상단 바 -->
  <header class="bar">
    <RouterLink class="bar__back" :to="{ name: 'library' }">← 라이브러리</RouterLink>
    <BrandMark compact />
    <span class="bar__title">{{ documentTitle }}</span>

    <span class="u-spacer" />

    <!-- REV-03 원문 근거 표시 (좌측 사이드바에서 상단 바로 이동) -->
    <label class="bar__toggle">
      <input v-model="showEvidence" type="checkbox" />
      원문 근거 표시
    </label>
    <span class="bar__divider" aria-hidden="true" />

    <span class="bar__progress">판정 {{ decidedCount }} / {{ findings.length }}</span>
    <!-- SHR-02 공유는 보류. 자리만 유지합니다. -->
    <AppButton size="sm" variant="ghost" disabled>공유</AppButton>
    <AppButton size="sm" :loading="completing" @click="complete">검토 완료</AppButton>
  </header>

  <!-- 3분할 본문 -->
  <div class="body">
    <SectionOutline />

    <div
      class="resizer"
      :class="{ 'is-active': resizing === 'outline' }"
      role="separator"
      aria-orientation="vertical"
      title="드래그해서 너비 조절 · 더블클릭 시 초기화"
      @pointerdown="startResize('outline', $event)"
      @dblclick="review.resetLayout()"
    />

    <DocumentViewer />

    <div
      class="resizer"
      :class="{ 'is-active': resizing === 'panel' }"
      role="separator"
      aria-orientation="vertical"
      title="드래그해서 너비 조절 · 더블클릭 시 초기화"
      @pointerdown="startResize('panel', $event)"
      @dblclick="review.resetLayout()"
    />

    <aside class="panel" :style="{ width: `${panelWidth}px` }">
      <div class="panel__content">
        <FindingList v-if="panelTab === 'findings'" />
        <AskPanel v-else-if="panelTab === 'ask'" />
        <AnnotationList v-else />
      </div>

      <nav class="panel__rail" aria-label="검토 패널 전환">
        <button
          type="button"
          :class="{ 'is-active': panelTab === 'findings' }"
          aria-label="검토 결과"
          title="검토 결과"
          @click="panelTab = 'findings'"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M9 6h11M9 12h11M9 18h11M4 6h.01M4 12h.01M4 18h.01" />
          </svg>
          <span v-if="findings.length" class="panel__count">{{ findings.length }}</span>
        </button>
        <button
          type="button"
          :class="{ 'is-active': panelTab === 'ask' }"
          aria-label="AI 질문"
          title="AI 질문"
          @click="panelTab = 'ask'"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M8.5 9a3.5 3.5 0 1 1 5.8 2.63C13.1 12.6 12 13.2 12 15M12 19h.01" />
          </svg>
        </button>
        <button
          type="button"
          :class="{ 'is-active': panelTab === 'annotations' }"
          aria-label="주석"
          title="주석"
          @click="panelTab = 'annotations'"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 20h4l10.5-10.5a2.83 2.83 0 0 0-4-4L4 16v4ZM13.5 6.5l4 4" />
          </svg>
        </button>
      </nav>
    </aside>
  </div>

  <div v-if="loading" class="overlay">검토 데이터를 불러오는 중…</div>
  <div v-else-if="error" class="overlay overlay--error">{{ error }}</div>
</template>

<style scoped>
.bar {
  height: var(--header-h);
  flex: none;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 16px;
  border-bottom: 1px solid var(--c-border);
  background: var(--c-surface);
}
.bar__back {
  font-size: var(--fs-md);
  color: var(--c-text-muted);
}
.bar__back:hover {
  color: var(--c-primary-600);
}
.bar__title {
  font-size: var(--fs-md);
  font-weight: 600;
}
.bar__progress {
  font-size: var(--fs-sm);
  color: var(--c-text-muted);
}
.bar__toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--fs-sm);
  color: var(--c-text-muted);
  cursor: pointer;
  white-space: nowrap;
}
.bar__toggle:hover {
  color: var(--c-text);
}
.bar__divider {
  width: 1px;
  height: 18px;
  background: var(--c-border);
}

.body {
  flex: 1;
  min-height: 0;
  display: flex;
}
.panel {
  flex: none;
  display: flex;
  background: var(--c-surface);
}
.panel__content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.panel__rail {
  width: 48px;
  flex: none;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 8px 6px;
  border-left: 1px solid var(--c-border);
  background: var(--c-bg-subtle);
}
.panel__rail button {
  position: relative;
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: var(--r-md);
  color: var(--c-text-muted);
}
.panel__rail button:hover {
  background: var(--c-surface-hover);
  color: var(--c-text);
}
.panel__rail button.is-active {
  background: var(--c-primary-50);
  color: var(--c-primary-700);
}
.panel__rail svg {
  width: 20px;
  height: 20px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.panel__count {
  position: absolute;
  top: 1px;
  right: 1px;
  min-width: 15px;
  height: 15px;
  display: grid;
  place-items: center;
  padding: 0 3px;
  border-radius: var(--r-full);
  background: var(--c-primary-500);
  color: var(--c-white);
  font-size: 9px;
  font-weight: 700;
}

.resizer {
  width: 5px;
  flex: none;
  cursor: col-resize;
  background: transparent;
  transition: background var(--transition);
}
.resizer::after {
  content: '';
  display: block;
  width: 1px;
  height: 100%;
  margin: 0 auto;
  background: var(--c-border);
}
.resizer:hover,
.resizer.is-active {
  background: var(--c-primary-200);
}
.resizer:hover::after,
.resizer.is-active::after {
  background: var(--c-primary-500);
}

.overlay {
  position: fixed;
  inset: var(--header-h) 0 0;
  display: grid;
  place-items: center;
  background: var(--c-surface-blur);
  color: var(--c-text-muted);
  font-size: var(--fs-md);
}
.overlay--error {
  color: var(--c-danger-600);
}
</style>
