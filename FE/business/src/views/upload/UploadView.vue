<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { documentApi, reviewApi } from '@/api'
import { applyName, applyNames } from '@/utils/documentNames'
import { useUiStore } from '@/stores/ui'
import { DOC_STATUS, UPLOAD_LIMIT } from '@/constants/enums'
import { formatBytes } from '@/utils/format'
import AppButton from '@/components/common/AppButton.vue'
import ProgressBar from '@/components/common/ProgressBar.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'

/** UP-01 새 문서 업로드 (F-01, F-04) */
const route = useRoute()
const router = useRouter()
const ui = useUiStore()

const fileInput = ref(null)
const dragging = ref(false)
const uploading = ref(false)
const progress = ref(0)
const rejectReason = ref('')
const uploaded = ref(null) // 업로드 직후 문서
const recent = ref([])
const starting = ref(false)

const acceptLabel = computed(
  () => `최대 ${UPLOAD_LIMIT.MAX_SIZE_MB}MB · ${UPLOAD_LIMIT.MAX_PAGES}페이지`,
)

onMounted(async () => {
  const { items } = await documentApi.list({ size: 5 })
  recent.value = applyNames(items)

  // 라이브러리에서 '미분석 문서 열기'로 들어온 경우 — 최근 목록에 없을 수 있어 직접 조회합니다
  const { documentId } = route.query
  if (!documentId) return
  try {
    uploaded.value = applyName(await documentApi.get(documentId))
  } catch {
    ui.error('문서를 찾을 수 없습니다')
  }
})

function validate(file) {
  if (!file) return '파일을 찾을 수 없습니다'
  if (file.type !== UPLOAD_LIMIT.ACCEPT) return '형식 불일치 — PDF 파일만 업로드할 수 있습니다'
  if (file.size > UPLOAD_LIMIT.MAX_SIZE_MB * 1024 * 1024)
    return `용량 초과 — 최대 ${UPLOAD_LIMIT.MAX_SIZE_MB}MB 까지 업로드할 수 있습니다`
  if (file.size === 0) return '손상된 파일입니다'
  return ''
}

async function handleFile(file) {
  rejectReason.value = validate(file)
  if (rejectReason.value) return

  uploading.value = true
  progress.value = 0
  try {
    uploaded.value = await documentApi.upload(file, { onProgress: (v) => (progress.value = v) })
    ui.success('업로드를 완료했습니다')
  } catch (e) {
    rejectReason.value = e.message
  } finally {
    uploading.value = false
  }
}

function onDrop(event) {
  dragging.value = false
  handleFile(event.dataTransfer.files?.[0])
}
function onPick(event) {
  handleFile(event.target.files?.[0])
  event.target.value = ''
}

/** ANL-01 분석 시작 → 진행 화면으로 이동 */
async function startAnalysis() {
  if (!uploaded.value) return
  starting.value = true
  try {
    const job = await reviewApi.createJob(uploaded.value.id)
    router.push({ name: 'job-progress', params: { jobId: job.id } })
  } catch (e) {
    ui.error(e.message)
  } finally {
    starting.value = false
  }
}
</script>

<template>
  <div class="upload">
    <header class="head">
      <h1 class="head__title">사업계획서 업로드</h1>
      <p class="head__desc">PDF를 올리면 목차 · 표 · 수치를 구조화한 뒤 검토를 시작합니다</p>
    </header>

    <!-- 드롭존 -->
    <section
      v-if="!uploaded"
      class="drop"
      :class="{ 'is-dragging': dragging, 'is-error': rejectReason }"
      @dragover.prevent="dragging = true"
      @dragleave.prevent="dragging = false"
      @drop.prevent="onDrop"
      @click="fileInput.click()"
    >
      <input ref="fileInput" type="file" accept="application/pdf" hidden @change="onPick" />
      <div class="drop__icon">PDF</div>
      <p class="drop__title">PDF를 여기에 끌어다 놓으세요</p>
      <p class="drop__desc">또는 클릭해서 파일 선택 · {{ acceptLabel }}</p>
      <AppButton variant="secondary" size="sm" @click.stop="fileInput.click()">파일 선택</AppButton>

      <div v-if="uploading" class="drop__progress">
        <ProgressBar :value="progress" />
        <span>{{ progress }}%</span>
      </div>
      <p v-if="rejectReason" class="drop__error">{{ rejectReason }}</p>
    </section>

    <!-- 업로드 완료 카드 -->
    <section v-else class="uploaded u-card">
      <div class="uploaded__icon">PDF</div>
      <div class="uploaded__body">
        <p class="uploaded__name">{{ uploaded.name }}</p>
        <p class="uploaded__meta">
          {{ formatBytes(uploaded.sizeBytes) }} · {{ uploaded.pageCount }}페이지 ·
          <StatusBadge :status="uploaded.status" />
        </p>
      </div>
      <div class="uploaded__actions">
        <AppButton variant="ghost" size="sm" @click="uploaded = null">다시 선택</AppButton>
        <AppButton :loading="starting" @click="startAnalysis">분석 시작</AppButton>
      </div>
    </section>

    <!-- 제약 안내 -->
    <ul class="rules">
      <li><b>지원 형식</b> PDF · HWP · DOCX는 파서 어댑터 추가 후 지원 예정</li>
      <li><b>거절 조건</b> 형식 불일치 · 용량 초과 · 손상 파일 · 동일 해시 파일 존재</li>
    </ul>

    <!-- 최근 업로드 (F-04 버전 등록 진입점) -->
    <section class="recent">
      <h2 class="recent__title">최근 업로드</h2>
      <ul class="recent__list u-card">
        <li v-for="doc in recent" :key="doc.id">
          <span class="recent__name">{{ doc.name }}</span>
          <span class="recent__version">v{{ doc.version }}</span>
          <StatusBadge :status="doc.status" />
          <AppButton
            size="sm"
            variant="ghost"
            :disabled="doc.status === DOC_STATUS.FAILED"
            @click="uploaded = doc"
          >
            새 버전 등록
          </AppButton>
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.upload {
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

.drop {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 52px 24px;
  border: 1.5px dashed var(--mat-hairline-strong);
  border-radius: var(--r-2xl);
  background: var(--mat-fill);
  backdrop-filter: blur(var(--mat-blur-sm)) saturate(var(--mat-saturate));
  -webkit-backdrop-filter: blur(var(--mat-blur-sm)) saturate(var(--mat-saturate));
  cursor: pointer;
  transition:
    border-color var(--transition),
    background var(--transition);
}
.drop:hover,
.drop.is-dragging {
  border-color: var(--c-primary-500);
  background: var(--c-primary-50);
}
.drop.is-error {
  border-color: var(--c-danger-500);
  background: var(--c-danger-50);
}
.drop__icon {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: var(--r-md);
  background: var(--c-primary-500);
  color: #fff;
  font-size: var(--fs-xs);
  font-weight: 800;
}
.drop__title {
  font-weight: 600;
}
.drop__desc {
  font-size: var(--fs-md);
  color: var(--c-text-muted);
  margin-bottom: 4px;
}
.drop__progress {
  width: 100%;
  max-width: 320px;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  font-size: var(--fs-sm);
  color: var(--c-text-muted);
}
.drop__error {
  margin-top: 6px;
  color: var(--c-danger-600);
  font-size: var(--fs-md);
  font-weight: 600;
}

.uploaded {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
}
.uploaded__icon {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: var(--r-md);
  background: var(--c-primary-50);
  color: var(--c-primary-600);
  font-size: var(--fs-xs);
  font-weight: 800;
}
.uploaded__body {
  flex: 1;
  min-width: 0;
}
.uploaded__name {
  font-weight: 600;
}
.uploaded__meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--fs-sm);
  color: var(--c-text-muted);
  margin-top: 2px;
}
.uploaded__actions {
  display: flex;
  gap: 6px;
}

.rules {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 14px;
  font-size: var(--fs-sm);
  color: var(--c-text-muted);
}
.rules b {
  color: var(--c-text);
  margin-right: 6px;
}

.recent {
  margin-top: 28px;
}
.recent__title {
  font-size: var(--fs-base);
  margin-bottom: 8px;
}
.recent__list li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--mat-hairline);
  font-size: var(--fs-md);
}
.recent__list li:last-child {
  border-bottom: none;
}
.recent__name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.recent__version {
  font-size: var(--fs-sm);
  font-weight: 700;
  color: var(--c-primary-600);
}
</style>
