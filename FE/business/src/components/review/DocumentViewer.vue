<script setup>
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useReviewStore } from '@/stores/review'
import { FINDING_TYPE } from '@/constants/enums'
import FindingTypeBadge from '@/components/common/FindingTypeBadge.vue'

/**
 * REV-03 원문 뷰어 / REV-06 양방향 앵커
 * 실제 서비스에서는 PDF 렌더러 + bbox 오버레이로 교체됩니다.
 * (블록 id ↔ finding.evidence[].anchorId 매핑 규칙은 그대로 유지)
 */
const review = useReviewStore()
const { currentPage, currentPageData, pages, zoom, showEvidence, findingByAnchor, flashAnchorId, activeFindingId } =
  storeToRefs(review)

const toneMap = {
  [FINDING_TYPE.ERROR]: 'error',
  [FINDING_TYPE.NEEDS_CHECK]: 'check',
  [FINDING_TYPE.NO_EVIDENCE]: 'evidence',
}

const totalPages = computed(() => pages.value.length || 1)

function findingOf(blockId) {
  return showEvidence.value ? findingByAnchor.value[blockId] : null
}
function toneOf(blockId) {
  const finding = findingOf(blockId)
  if (!finding) return null
  return toneMap[finding.type]
}
</script>

<template>
  <section class="viewer">
    <!-- 툴바 -->
    <header class="toolbar">
      <div class="toolbar__group">
        <button @click="zoom = Math.max(55, zoom - 10)">−</button>
        <button class="toolbar__zoom" @click="zoom = 82">{{ zoom }}%</button>
        <button @click="zoom = Math.min(140, zoom + 10)">＋</button>
      </div>
      <div class="toolbar__group">
        <button @click="review.goToPage(currentPage - 1)">‹</button>
        <input
          class="toolbar__page"
          :value="currentPage"
          @change="review.goToPage($event.target.value)"
        />
        <span class="toolbar__total">/ {{ totalPages }}</span>
        <button @click="review.goToPage(currentPage + 1)">›</button>
      </div>
      <span class="u-spacer" />
      <span class="toolbar__section">{{ currentPageData?.sectionTitle }}</span>
    </header>

    <!-- 지면 -->
    <div class="viewer__scroll u-scroll">
      <article
        class="paper"
        :style="{ transform: `scale(${zoom / 100})`, transformOrigin: 'top center' }"
      >
        <span class="paper__page">{{ currentPage }} / {{ totalPages }}</span>

        <template v-for="block in currentPageData?.blocks ?? []" :key="block.id">
          <!-- 제목 -->
          <h2 v-if="block.kind === 'h2'" class="paper__h2">{{ block.text }}</h2>

          <!-- 문단 -->
          <p
            v-else-if="block.kind === 'p'"
            class="paper__p"
            :class="[
              toneOf(block.id) ? `hl hl--${toneOf(block.id)}` : '',
              { 'is-flash': flashAnchorId === block.id, 'is-active': findingOf(block.id)?.id === activeFindingId },
            ]"
            @click="findingOf(block.id) && review.selectAnchor(block.id)"
          >
            {{ block.text }}
            <span v-if="findingOf(block.id)" class="mini">
              <FindingTypeBadge :type="findingOf(block.id).type" size="sm" />
              {{ findingOf(block.id).title }}
            </span>
          </p>

          <!-- 표 -->
          <figure
            v-else-if="block.kind === 'table'"
            class="paper__table"
            :class="[
              toneOf(block.id) ? `hl hl--${toneOf(block.id)}` : '',
              { 'is-flash': flashAnchorId === block.id },
            ]"
            @click="findingOf(block.id) && review.selectAnchor(block.id)"
          >
            <figcaption>{{ block.caption }}</figcaption>
            <table>
              <thead>
                <tr>
                  <th v-for="head in block.head" :key="head">{{ head }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, rowIndex) in block.rows" :key="rowIndex">
                  <td v-for="(cell, cellIndex) in row" :key="cellIndex">{{ cell }}</td>
                </tr>
              </tbody>
            </table>
          </figure>

          <!-- 이미지 · 그래프 -->
          <div
            v-else
            class="paper__figure"
            :class="toneOf(block.id) ? `hl hl--${toneOf(block.id)}` : ''"
            @click="findingOf(block.id) && review.selectAnchor(block.id)"
          >
            {{ block.text }}
          </div>
        </template>
      </article>
    </div>
  </section>
</template>

<style scoped>
.viewer {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--c-bg-subtle);
}
.toolbar {
  height: 40px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 14px;
  border-bottom: 1px solid var(--c-border);
  background: var(--c-surface);
}
.toolbar__group {
  display: flex;
  align-items: center;
  gap: 2px;
}
.toolbar__group button {
  width: 26px;
  height: 26px;
  border-radius: var(--r-sm);
  color: var(--c-text-muted);
}
.toolbar__group button:hover {
  background: var(--c-surface-hover);
}
.toolbar__zoom {
  width: auto !important;
  padding: 0 6px;
  font-size: var(--fs-sm);
  font-weight: 600;
}
.toolbar__page {
  width: 40px;
  height: 26px;
  text-align: center;
  border: 1px solid var(--c-border);
  border-radius: var(--r-sm);
  font-size: var(--fs-sm);
}
.toolbar__total {
  font-size: var(--fs-sm);
  color: var(--c-text-muted);
}
.toolbar__section {
  font-size: var(--fs-sm);
  color: var(--c-text-subtle);
}

.viewer__scroll {
  flex: 1;
  padding: 20px;
}
.paper {
  max-width: 660px;
  margin: 0 auto;
  min-height: 900px;
  padding: 44px 48px;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: 4px;
  box-shadow: var(--shadow-sm);
  position: relative;
}
.paper__page {
  position: absolute;
  top: 16px;
  right: 20px;
  font-size: var(--fs-xs);
  color: var(--c-text-subtle);
}
.paper__h2 {
  font-size: var(--fs-lg);
  margin-bottom: 14px;
}
.paper__p {
  position: relative;
  font-size: var(--fs-md);
  line-height: 1.9;
  margin-bottom: 12px;
  padding: 2px 4px;
  border-radius: var(--r-sm);
}
.paper__table {
  margin: 16px 0;
  padding: 6px;
  border-radius: var(--r-sm);
}
.paper__table figcaption {
  font-size: var(--fs-sm);
  font-weight: 600;
  margin-bottom: 6px;
}
.paper__table th,
.paper__table td {
  border: 1px solid var(--c-border);
  padding: 6px 8px;
  font-size: var(--fs-sm);
}
.paper__table th {
  background: var(--c-bg-subtle);
  font-weight: 600;
}
.paper__figure {
  height: 120px;
  display: grid;
  place-items: center;
  margin: 16px 0;
  border: 1px dashed var(--c-border-strong);
  border-radius: var(--r-md);
  color: var(--c-text-subtle);
  font-size: var(--fs-sm);
}

/* 근거 하이라이트 */
.hl {
  cursor: pointer;
  box-shadow: inset 0 -2px 0 currentColor;
}
.hl--error {
  background: var(--c-finding-error-bg);
  color: var(--c-finding-error);
}
.hl--check {
  background: var(--c-finding-check-bg);
  color: var(--c-finding-check);
}
.hl--evidence {
  background: var(--c-finding-evidence-bg);
  color: var(--c-finding-evidence);
}
.hl.is-active {
  outline: 2px solid currentColor;
  outline-offset: 1px;
}
.hl.is-flash {
  animation: flash 0.85s ease;
}
@keyframes flash {
  0%,
  100% {
    filter: none;
  }
  40% {
    filter: brightness(0.9) saturate(1.6);
  }
}

/* 하이라이트 호버 미니 카드 */
.mini {
  display: none;
  position: absolute;
  left: 0;
  bottom: calc(100% + 6px);
  z-index: 10;
  max-width: 320px;
  align-items: center;
  gap: 6px;
  padding: 7px 9px;
  border-radius: var(--r-md);
  background: var(--c-inverse-bg);
  color: var(--c-inverse-fg);
  font-size: var(--fs-sm);
  line-height: 1.4;
  box-shadow: var(--shadow-md);
}
.hl:hover .mini {
  display: flex;
}
</style>
