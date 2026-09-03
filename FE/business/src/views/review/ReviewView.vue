<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { documentApi } from '@/api'
import { useReviewStore } from '@/stores/review'
import { useUiStore } from '@/stores/ui'
import AppButton from '@/components/common/AppButton.vue'
import BrandMark from '@/components/common/BrandMark.vue'
import DocumentViewer from '@/components/review/DocumentViewer.vue'
import FindingList from '@/components/review/FindingList.vue'
import SectionOutline from '@/components/review/SectionOutline.vue'
import AskPanel from '@/components/review/AskPanel.vue'

/** REV-01 검토 화면 (3분할) · SHR-01 검토 완료 */
const props = defineProps({ jobId: { type: String, required: true } })

const router = useRouter()
const ui = useUiStore()
const review = useReviewStore()
const { findings, decidedCount, loading, error, showEvidence, panelWidth } = storeToRefs(review)

const tab = ref('findings')
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
    const doc = await documentApi.get(review.job.documentId)
    documentTitle.value = `${doc.name} v${doc.version}`
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
      <div class="panel__tabs">
        <button :class="{ 'is-active': tab === 'findings' }" @click="tab = 'findings'">
          검토 결과 <em>{{ findings.length }}</em>
        </button>
        <button :class="{ 'is-active': tab === 'ask' }" @click="tab = 'ask'">AI 질문</button>
      </div>

      <FindingList v-if="tab === 'findings'" />
      <AskPanel v-else />
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
  flex-direction: column;
  background: var(--c-surface);
}
.panel__tabs {
  display: flex;
  border-bottom: 1px solid var(--c-border);
}
.panel__tabs button {
  flex: 1;
  height: 40px;
  font-size: var(--fs-md);
  font-weight: 600;
  color: var(--c-text-muted);
  border-bottom: 2px solid transparent;
}
.panel__tabs button.is-active {
  color: var(--c-primary-700);
  border-bottom-color: var(--c-primary-500);
}
.panel__tabs em {
  font-style: normal;
  font-size: var(--fs-sm);
  color: var(--c-text-subtle);
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
