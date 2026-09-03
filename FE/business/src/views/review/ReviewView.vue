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
const { findings, decidedCount, loading, error } = storeToRefs(review)

const tab = ref('findings')
const completing = ref(false)

const documentTitle = ref('문서')

onMounted(async () => {
  await review.load(props.jobId)
  if (review.job?.documentId) {
    const doc = await documentApi.get(review.job.documentId)
    documentTitle.value = `${doc.name} v${doc.version}`
  }
})
onUnmounted(() => review.reset())

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

    <span class="bar__progress">판정 {{ decidedCount }} / {{ findings.length }}</span>
    <!-- SHR-02 공유는 보류. 자리만 유지합니다. -->
    <AppButton size="sm" variant="ghost" disabled>공유</AppButton>
    <AppButton size="sm" :loading="completing" @click="complete">검토 완료</AppButton>
  </header>

  <!-- 3분할 본문 -->
  <div class="body">
    <SectionOutline />
    <DocumentViewer />

    <aside class="panel">
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

.body {
  flex: 1;
  min-height: 0;
  display: flex;
}
.panel {
  width: var(--review-w);
  flex: none;
  display: flex;
  flex-direction: column;
  border-left: 1px solid var(--c-border);
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

.overlay {
  position: fixed;
  inset: var(--header-h) 0 0;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.8);
  color: var(--c-text-muted);
  font-size: var(--fs-md);
}
.overlay--error {
  color: var(--c-danger-600);
}
</style>
