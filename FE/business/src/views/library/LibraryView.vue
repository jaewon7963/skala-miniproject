<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { storeToRefs } from 'pinia'
import { useDocumentStore } from '@/stores/documents'
import { useUiStore } from '@/stores/ui'
import { DOC_PERIOD_LABEL, DOC_SORT_LABEL, DOC_STATUS, DOC_STATUS_LABEL } from '@/constants/enums'
import { formatBytes, formatDateTime } from '@/utils/format'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import AppModal from '@/components/common/AppModal.vue'
import AppPagination from '@/components/common/AppPagination.vue'
import AppSelect from '@/components/common/AppSelect.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import ThemeToggle from '@/components/common/ThemeToggle.vue'

/** DASH-01 ~ DASH-04 (F-09, F-10, F-12) */
const router = useRouter()
const ui = useUiStore()
const store = useDocumentStore()
const { items, folders, total, counts, loading, query } = storeToRefs(store)

const menuOpenId = ref(null)
const renameTarget = ref(null)
const renameValue = ref('')
const deleteTarget = ref(null)

const statusTabs = computed(() => [
  { key: 'ALL', label: '전체 문서', count: counts.value.ALL ?? 0 },
  { key: DOC_STATUS.REVIEWING, label: '검토 중', count: counts.value[DOC_STATUS.REVIEWING] ?? 0 },
  { key: DOC_STATUS.DONE, label: '검토 완료', count: counts.value[DOC_STATUS.DONE] ?? 0 },
  { key: DOC_STATUS.FAILED, label: '파싱 실패', count: counts.value[DOC_STATUS.FAILED] ?? 0 },
])

const periodOptions = Object.entries(DOC_PERIOD_LABEL).map(([value, label]) => ({ value, label }))
const sortOptions = Object.entries(DOC_SORT_LABEL).map(([value, label]) => ({ value, label }))
const statusOptions = [
  { value: 'ALL', label: '전체' },
  ...Object.entries(DOC_STATUS_LABEL).map(([value, label]) => ({ value, label })),
]

const isSearching = computed(() => Boolean(query.value.q) || query.value.status !== 'ALL')
const currentTitle = computed(
  () => statusTabs.value.find((tab) => tab.key === query.value.status)?.label ?? '전체 문서',
)

onMounted(() => {
  store.fetchList()
  store.fetchFolders()
})

function selectStatus(status) {
  store.fetchList({ status, folderId: null, page: 1 })
}
function selectFolder(folderId) {
  store.fetchList({ folderId, status: 'ALL', page: 1 })
}

/** DASH-03 문서 열기 : 분석 이력 유무로 분기 */
function openDocument(doc) {
  if (doc.status === DOC_STATUS.FAILED) {
    ui.error('파싱에 실패한 문서입니다. 다시 업로드해주세요')
    return
  }
  if (doc.latestJobId) router.push({ name: 'review', params: { jobId: doc.latestJobId } })
  else router.push({ name: 'upload', query: { documentId: doc.id } })
}

function openRename(doc) {
  menuOpenId.value = null
  renameTarget.value = doc
  renameValue.value = doc.name
}
async function submitRename() {
  if (!renameValue.value.trim()) return
  await store.rename(renameTarget.value.id, renameValue.value.trim())
  ui.success('문서명을 변경했습니다')
  renameTarget.value = null
}

function openDelete(doc) {
  menuOpenId.value = null
  deleteTarget.value = doc
}
async function submitDelete() {
  await store.remove(deleteTarget.value.id)
  ui.success('문서를 삭제했습니다')
  deleteTarget.value = null
}
</script>

<template>
  <div class="library" @click="menuOpenId = null">
    <!-- 좌측 : 상태 · 폴더 -->
    <aside class="side">
      <p class="side__label">문서</p>
      <ul class="side__list">
        <li v-for="tab in statusTabs" :key="tab.key">
          <button
            :class="{ 'is-active': query.status === tab.key && !query.folderId }"
            @click="selectStatus(tab.key)"
          >
            <span>{{ tab.label }}</span>
            <em>{{ tab.count }}</em>
          </button>
        </li>
      </ul>

      <hr class="side__divider" />

      <p class="side__label">폴더</p>
      <ul class="side__list">
        <li v-for="folder in folders" :key="folder.id">
          <button
            :class="{ 'is-active': query.folderId === folder.id }"
            @click="selectFolder(folder.id)"
          >
            <span>{{ folder.name }}</span>
            <em>{{ folder.count }}</em>
          </button>
        </li>
        <li v-if="!folders.length" class="side__empty">폴더가 없습니다</li>
      </ul>

      <!-- 사이드바 하단 : 다크 모드 전환 -->
      <footer class="side__foot">
        <ThemeToggle />
      </footer>
    </aside>

    <!-- 우측 : 목록 -->
    <section class="main">
      <header class="main__head">
        <div>
          <h1 class="main__title">{{ currentTitle }}</h1>
          <p class="main__desc">업로드한 사업계획서와 검토 결과를 한 곳에서 관리합니다</p>
        </div>
        <AppButton @click="router.push({ name: 'upload' })">문서 업로드</AppButton>
      </header>

      <div class="toolbar">
        <AppInput
          :model-value="query.q"
          placeholder="문서명 · 요약 내용 검색"
          @update:model-value="store.fetchList({ q: $event, page: 1 })"
        />
        <AppSelect
          label="상태"
          size="sm"
          :model-value="query.status"
          :options="statusOptions"
          @update:model-value="store.fetchList({ status: $event, page: 1 })"
        />
        <AppSelect
          label="기간"
          size="sm"
          :model-value="query.period"
          :options="periodOptions"
          @update:model-value="store.fetchList({ period: $event, page: 1 })"
        />
        <AppSelect
          label="정렬"
          size="sm"
          :model-value="query.sort"
          :options="sortOptions"
          @update:model-value="store.fetchList({ sort: $event, page: 1 })"
        />
      </div>

      <div class="table-wrap u-card">
        <table class="table">
          <thead>
            <tr>
              <th>문서명</th>
              <th style="width: 110px">상태</th>
              <th style="width: 150px">최근 수정</th>
              <th style="width: 44px"></th>
            </tr>
          </thead>
          <tbody v-if="items.length">
            <tr v-for="doc in items" :key="doc.id" @click="openDocument(doc)">
              <td>
                <p class="doc__name">{{ doc.name }}</p>
                <p class="doc__meta">
                  {{ doc.pageCount }}페이지 · {{ formatBytes(doc.sizeBytes) }}
                </p>
              </td>
              <td><StatusBadge :status="doc.status" /></td>
              <td class="doc__date">{{ formatDateTime(doc.updatedAt) }}</td>
              <td class="doc__menu" @click.stop>
                <button class="icon-btn" @click="menuOpenId = menuOpenId === doc.id ? null : doc.id">
                  ⋮
                </button>
                <ul v-if="menuOpenId === doc.id" class="menu">
                  <li><button @click="openRename(doc)">이름 변경</button></li>
                  <li>
                    <button
                      :disabled="!doc.latestJobId"
                      @click="
                        router.push({ name: 'review-report', params: { jobId: doc.latestJobId } })
                      "
                    >
                      요약 내보내기
                    </button>
                  </li>
                  <li><button class="is-danger" @click="openDelete(doc)">삭제</button></li>
                </ul>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-if="loading" class="state">불러오는 중…</div>

        <!-- 빈 상태 2종 (F-10) -->
        <EmptyState
          v-else-if="!items.length && isSearching"
          icon="⌕"
          title="조건에 맞는 문서가 없습니다"
          description="검색어나 필터를 변경해보세요"
        >
          <AppButton variant="secondary" size="sm" @click="store.resetQuery(); store.fetchList()">
            필터 초기화
          </AppButton>
        </EmptyState>
        <EmptyState
          v-else-if="!items.length"
          icon="＋"
          title="아직 업로드한 문서가 없습니다"
          description="PDF 사업계획서를 올리면 검토를 시작할 수 있습니다"
        >
          <AppButton @click="router.push({ name: 'upload' })">문서 업로드</AppButton>
        </EmptyState>
      </div>

      <AppPagination
        v-if="items.length"
        :page="query.page"
        :size="query.size"
        :total="total"
        @update:page="store.fetchList({ page: $event })"
      />
    </section>

    <!-- 이름 변경 -->
    <AppModal :open="Boolean(renameTarget)" title="문서 이름 변경" @close="renameTarget = null">
      <AppInput v-model="renameValue" label="문서명" @enter="submitRename" />
      <template #footer>
        <AppButton variant="secondary" @click="renameTarget = null">취소</AppButton>
        <AppButton @click="submitRename">저장</AppButton>
      </template>
    </AppModal>

    <!-- 삭제 -->
    <AppModal :open="Boolean(deleteTarget)" title="문서를 삭제할까요?" @close="deleteTarget = null">
      <p class="confirm">
        <b>{{ deleteTarget?.name }}</b
        >와 연결된 분석 작업 · 검토 항목이 함께 삭제됩니다. 이 작업은 되돌릴 수 없습니다.
      </p>
      <template #footer>
        <AppButton variant="secondary" @click="deleteTarget = null">취소</AppButton>
        <AppButton variant="danger" @click="submitDelete">삭제</AppButton>
      </template>
    </AppModal>
  </div>
</template>

<style scoped>
.library {
  display: grid;
  grid-template-columns: var(--sidebar-w) minmax(0, 1fr);
  gap: 28px;
  align-items: start;
}

/* --- 사이드바 --- */
.side {
  position: sticky;
  top: calc(var(--header-h) + 24px);
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - var(--header-h) - 72px);
  padding-right: 16px;
  border-right: 1px solid var(--c-border);
}
.side__divider {
  height: 1px;
  margin: 4px 8px 14px;
  border: none;
  background: var(--c-border);
}
.side__foot {
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid var(--c-border);
}
.side__label {
  font-size: var(--fs-sm);
  font-weight: 700;
  color: var(--c-text-subtle);
  padding: 0 8px;
  margin: 6px 0;
}
.side__list {
  margin-bottom: 14px;
}
.side__list button {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  height: 34px;
  padding: 0 8px;
  border-radius: var(--r-md);
  font-size: var(--fs-md);
  color: var(--c-text-muted);
}
.side__list button:hover {
  background: var(--c-surface-hover);
  color: var(--c-text);
}
.side__list button.is-active {
  background: var(--c-primary-50);
  color: var(--c-primary-700);
  font-weight: 700;
}
.side__list em {
  font-style: normal;
  font-size: var(--fs-sm);
  color: var(--c-text-subtle);
}
.side__empty {
  padding: 0 8px;
  font-size: var(--fs-sm);
  color: var(--c-text-subtle);
}

/* --- 헤더 --- */
.main__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}
.main__title {
  font-size: var(--fs-xl);
  letter-spacing: -0.02em;
}
.main__desc {
  color: var(--c-text-muted);
  font-size: var(--fs-md);
  margin-top: 2px;
}

/* --- 툴바 --- */
.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}
.toolbar > :first-child {
  max-width: 300px;
}

/* --- 테이블 --- */
.table-wrap {
  overflow: visible;
}
.table {
  font-size: var(--fs-md);
}
.table th {
  text-align: left;
  font-size: var(--fs-sm);
  font-weight: 600;
  color: var(--c-text-subtle);
  padding: 12px 16px;
  border-bottom: 1px solid var(--c-border);
}
.table td {
  padding: 12px 16px;
  border-bottom: 1px solid var(--c-border);
  vertical-align: middle;
}
.table tbody tr:last-child td {
  border-bottom: none;
}
.table tbody tr {
  cursor: pointer;
}
.table tbody tr:hover {
  background: var(--c-bg-subtle);
}
.doc__name {
  font-weight: 600;
}
.doc__meta {
  font-size: var(--fs-sm);
  color: var(--c-text-subtle);
  margin-top: 2px;
}
.doc__date {
  color: var(--c-text-muted);
  font-size: var(--fs-sm);
}
.doc__menu {
  position: relative;
  text-align: right;
}
.icon-btn {
  width: 28px;
  height: 28px;
  border-radius: var(--r-sm);
  color: var(--c-text-muted);
}
.icon-btn:hover {
  background: var(--c-surface-hover);
}
.menu {
  position: absolute;
  right: 8px;
  top: 34px;
  z-index: 20;
  min-width: 148px;
  padding: 4px;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--r-md);
  box-shadow: var(--shadow-md);
  text-align: left;
}
.menu button {
  width: 100%;
  text-align: left;
  padding: 7px 10px;
  border-radius: var(--r-sm);
  font-size: var(--fs-md);
}
.menu button:hover:not(:disabled) {
  background: var(--c-surface-hover);
}
.menu button:disabled {
  color: var(--c-text-subtle);
  cursor: not-allowed;
}
.menu .is-danger {
  color: var(--c-danger-600);
}
.state {
  padding: 40px;
  text-align: center;
  color: var(--c-text-muted);
  font-size: var(--fs-md);
}
.confirm {
  font-size: var(--fs-md);
  color: var(--c-text-muted);
  line-height: 1.6;
}
</style>
