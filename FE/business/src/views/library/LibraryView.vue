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
const { items, tags, total, counts, loading, query } = storeToRefs(store)

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

const isSearching = computed(
  () => Boolean(query.value.q) || query.value.status !== 'ALL' || Boolean(query.value.tag),
)
const tagNames = computed(() => Object.fromEntries(tags.value.map((tag) => [tag.id, tag.name])))
const currentTitle = computed(() => {
  if (query.value.tag) return `# ${tagNames.value[query.value.tag] ?? query.value.tag}`
  return statusTabs.value.find((tab) => tab.key === query.value.status)?.label ?? '전체 문서'
})

onMounted(() => {
  store.fetchList()
  store.fetchTags()
})

function selectStatus(status) {
  store.fetchList({ status, tag: null, page: 1 })
}
/** 이미 선택된 태그를 다시 누르면 선택을 해제합니다 */
function selectTag(tag) {
  const next = query.value.tag === tag ? null : tag
  store.fetchList({ tag: next, status: 'ALL', page: 1 })
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
    <!-- 좌측 : 상태 · 태그 -->
    <aside class="side">
      <div class="side__scroll u-scroll">
        <p class="side__label">문서</p>
        <ul class="side__list">
        <li v-for="tab in statusTabs" :key="tab.key">
          <button
            :class="{ 'is-active': query.status === tab.key && !query.tag }"
            @click="selectStatus(tab.key)"
          >
            <span>{{ tab.label }}</span>
            <em>{{ tab.count }}</em>
          </button>
        </li>
      </ul>

      <hr class="side__divider" />

      <p class="side__label">태그</p>
      <ul class="side__list">
        <li v-for="tag in tags" :key="tag.id">
          <button
            :class="{ 'is-active': query.tag === tag.id }"
            :aria-pressed="query.tag === tag.id"
            :title="query.tag === tag.id ? '태그 선택 해제' : `${tag.name} 태그로 필터`"
            @click="selectTag(tag.id)"
          >
            <span># {{ tag.name }}</span>
            <em>{{ tag.count }}</em>
          </button>
        </li>
          <li v-if="!tags.length" class="side__empty">태그가 없습니다</li>
        </ul>
      </div>

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
        <AppButton @click="router.push({ name: 'upload' })">
          <svg class="btn-icon" viewBox="0 0 16 16" aria-hidden="true" focusable="false">
            <path d="M8 11V2.6M8 2.6 4.9 5.7M8 2.6l3.1 3.1" />
            <path d="M2.6 10.4v1.9a1.7 1.7 0 0 0 1.7 1.7h7.4a1.7 1.7 0 0 0 1.7-1.7v-1.9" />
          </svg>
          문서 업로드
        </AppButton>
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
              <th style="width: 230px">태그</th>
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
              <td>
                <div class="doc__tags">
                  <span v-for="tag in doc.tags" :key="tag"># {{ tagNames[tag] ?? tag }}</span>
                  <span v-if="!doc.tags?.length" class="is-empty">태그 없음</span>
                </div>
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
          <AppButton @click="router.push({ name: 'upload' })">
          <svg class="btn-icon" viewBox="0 0 16 16" aria-hidden="true" focusable="false">
            <path d="M8 11V2.6M8 2.6 4.9 5.7M8 2.6l3.1 3.1" />
            <path d="M2.6 10.4v1.9a1.7 1.7 0 0 0 1.7 1.7h7.4a1.7 1.7 0 0 0 1.7-1.7v-1.9" />
          </svg>
          문서 업로드
        </AppButton>
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

.btn-icon {
  width: 15px;
  height: 15px;
  flex: none;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.6;
  stroke-linecap: round;
  stroke-linejoin: round;
}

/* --- 사이드바 --- */
.side {
  position: sticky;
  top: calc(var(--header-h) + 24px);
  display: flex;
  flex-direction: column;
  /* 목록이 길어져도 사이드바는 화면에 고정됩니다. 내용이 넘치면 사이드바 안에서만 스크롤합니다 */
  max-height: calc(100vh - var(--header-h) - 48px);
  overflow: hidden;
  padding: 10px;
  border-radius: var(--r-xl);
  background: var(--mat-sidebar);
  backdrop-filter: blur(var(--mat-blur)) saturate(var(--mat-saturate));
  -webkit-backdrop-filter: blur(var(--mat-blur)) saturate(var(--mat-saturate));
  border: 1px solid var(--mat-hairline);
  box-shadow: var(--shadow-inner-top);
}
.side__scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  margin: -2px -4px 0;
  padding: 2px 4px 0;
}
.side__divider {
  height: 1px;
  margin: 4px 8px 14px;
  border: none;
  background: var(--mat-hairline);
}
.side__foot {
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px solid var(--mat-hairline);
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
  height: 32px;
  padding: 0 10px;
  border-radius: var(--r-sm);
  font-size: var(--fs-md);
  color: var(--c-text-muted);
  transition:
    background var(--transition),
    color var(--transition);
}
.side__list button:hover {
  background: var(--mat-fill);
  color: var(--c-text);
}
.side__list button.is-active {
  background: var(--c-primary-500);
  color: #fff;
  font-weight: 600;
  box-shadow: var(--shadow-inner-top);
}
.side__list button.is-active em {
  color: rgba(255, 255, 255, 0.75);
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
  border-radius: var(--r-xl);
}
.table {
  font-size: var(--fs-md);
}
.table th {
  text-align: left;
  font-size: var(--fs-sm);
  font-weight: 600;
  color: var(--c-text-subtle);
  padding: 11px 18px;
  border-bottom: 1px solid var(--mat-hairline);
}
.table td {
  padding: 13px 18px;
  border-bottom: 1px solid var(--mat-hairline);
  vertical-align: middle;
}
.table tbody tr:last-child td {
  border-bottom: none;
}
.table tbody tr {
  cursor: pointer;
}
.table tbody tr {
  transition: background var(--transition);
}
.table tbody tr:hover {
  background: var(--mat-fill);
}
.table tbody tr:first-child:hover td:first-child {
  border-top-left-radius: var(--r-lg);
}
.doc__name {
  font-weight: 600;
}
.doc__meta {
  font-size: var(--fs-sm);
  color: var(--c-text-subtle);
  margin-top: 2px;
}
.doc__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}
.doc__tags span {
  padding: 3px 7px;
  border-radius: var(--r-full);
  background: var(--c-primary-50);
  color: var(--c-primary-700);
  font-size: var(--fs-xs);
  font-weight: 600;
  white-space: nowrap;
}
.doc__tags span.is-empty {
  background: var(--c-bg-subtle);
  color: var(--c-text-subtle);
  font-weight: 400;
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
  background: var(--mat-fill);
}
.menu {
  position: absolute;
  right: 8px;
  top: 34px;
  z-index: 20;
  min-width: 158px;
  padding: 5px;
  background: var(--mat-card);
  backdrop-filter: blur(30px) saturate(var(--mat-saturate));
  -webkit-backdrop-filter: blur(30px) saturate(var(--mat-saturate));
  border: 1px solid var(--mat-hairline);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow-inner-top), var(--shadow-lg);
  text-align: left;
  animation: pop var(--dur-fast) var(--ease-spring);
  transform-origin: top right;
}
@keyframes pop {
  from {
    opacity: 0;
    transform: scale(0.94);
  }
}
.menu button {
  width: 100%;
  text-align: left;
  padding: 7px 10px;
  border-radius: var(--r-sm);
  font-size: var(--fs-md);
}
.menu button:hover:not(:disabled) {
  background: var(--mat-fill);
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
