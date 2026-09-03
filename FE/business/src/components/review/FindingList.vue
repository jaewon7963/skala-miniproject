<script setup>
import { storeToRefs } from 'pinia'
import { useReviewStore } from '@/stores/review'
import { useUiStore } from '@/stores/ui'
import {
  FINDING_SORT_LABEL,
  FINDING_TYPE_LABEL,
  FINDING_TYPE_ORDER,
  VERDICT_LABEL,
} from '@/constants/enums'
import AppSelect from '@/components/common/AppSelect.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import FindingCard from '@/components/review/FindingCard.vue'

/** REV-04 검토 항목 리스트 (F-19) */
const review = useReviewStore()
const ui = useUiStore()
const { visibleFindings, countsByType, filterType, sort, undecidedOnly, activeFindingId } =
  storeToRefs(review)

const sortOptions = Object.entries(FINDING_SORT_LABEL).map(([value, label]) => ({ value, label }))

async function setVerdict(finding, verdict) {
  try {
    await review.setVerdict(finding.id, verdict)
    ui.success(`${VERDICT_LABEL[verdict]}(으)로 표시했습니다`)
  } catch (e) {
    ui.error(e.message)
  }
}
async function undo(finding) {
  await review.undoVerdict(finding.id)
  ui.info('판정을 되돌렸습니다')
}
</script>

<template>
  <div class="list">
    <!-- 유형 카운트 : 클릭 시 필터, 재클릭 해제 -->
    <div class="counts">
      <button
        v-for="type in FINDING_TYPE_ORDER"
        :key="type"
        class="counts__item"
        :class="[`counts__item--${type.toLowerCase()}`, { 'is-on': filterType === type }]"
        @click="review.toggleTypeFilter(type)"
      >
        <b>{{ countsByType[type] ?? 0 }}</b>
        <span>{{ FINDING_TYPE_LABEL[type] }}</span>
      </button>
    </div>

    <!-- 정렬 · 필터 -->
    <div class="filters">
      <AppSelect v-model="sort" label="정렬" size="sm" :options="sortOptions" />
      <label class="filters__check">
        <input v-model="undecidedOnly" type="checkbox" />
        미판정만 보기
      </label>
    </div>

    <!-- 카드 목록 -->
    <div class="cards u-scroll">
      <FindingCard
        v-for="(finding, index) in visibleFindings"
        :key="finding.id"
        :finding="finding"
        :index="index + 1"
        :selected="activeFindingId === finding.id"
        @select="review.selectFinding(finding.id)"
        @jump="review.selectFinding(finding.id, $event)"
        @verdict="setVerdict(finding, $event)"
        @undo="undo(finding)"
      />

      <EmptyState
        v-if="!visibleFindings.length"
        icon="✓"
        title="조건에 맞는 항목이 없습니다"
        description="필터를 변경하거나 미판정만 보기를 해제해보세요"
      />
    </div>
  </div>
</template>

<style scoped>
.list {
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1;
}
.counts {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  padding: 10px 12px;
}
.counts__item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
  padding: 8px 4px;
  border: 1px solid var(--c-border);
  border-radius: var(--r-md);
  background: var(--c-surface);
}
.counts__item b {
  font-size: var(--fs-lg);
}
.counts__item span {
  font-size: var(--fs-xs);
  color: var(--c-text-muted);
}
.counts__item:hover {
  border-color: var(--c-border-strong);
}
.counts__item--error b {
  color: var(--c-finding-error);
}
.counts__item--needs_check b {
  color: var(--c-finding-check);
}
.counts__item--no_evidence b {
  color: var(--c-finding-evidence);
}
.counts__item.is-on {
  border-color: var(--c-primary-500);
  background: var(--c-primary-50);
}

.filters {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 12px 10px;
}
.filters__check {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: var(--fs-sm);
  color: var(--c-text-muted);
  cursor: pointer;
}

.cards {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0 12px 16px;
}
</style>
