<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useReviewStore } from '@/stores/review'
import { useUiStore } from '@/stores/ui'
import { FINDING_METHOD, FINDING_TYPE, FINDING_TYPE_LABEL } from '@/constants/enums'

/**
 * REV-03 원문 뷰어 / REV-06 양방향 앵커
 * 실제 서비스에서는 PDF 렌더러 + bbox 오버레이로 교체됩니다.
 * (블록 id ↔ finding.evidence[].anchorId 매핑 규칙은 그대로 유지)
 */
const review = useReviewStore()
const ui = useUiStore()
const { currentPage, currentPageData, pages, zoom, showEvidence, findingByAnchor, flashAnchorId, activeFindingId } =
  storeToRefs(review)

const toneMap = {
  [FINDING_TYPE.ERROR]: 'error',
  [FINDING_TYPE.NEEDS_CHECK]: 'check',
  [FINDING_TYPE.NO_EVIDENCE]: 'evidence',
}

const totalPages = computed(() => pages.value.length || 1)
const viewerScroll = ref(null)
const gestureStartZoom = ref(82)
const selection = ref(null)
const optionsOpen = ref(false)
const adding = ref(false)
const annotationText = ref('')

const typeOptions = [FINDING_TYPE.ERROR, FINDING_TYPE.NEEDS_CHECK, FINDING_TYPE.NO_EVIDENCE]

const clampZoom = (value) => Math.min(140, Math.max(55, Math.round(value)))

function handleTrackpadZoom(event) {
  if (!event.ctrlKey && !event.metaKey) return
  event.preventDefault()
  zoom.value = clampZoom(zoom.value - event.deltaY * 0.12)
}

function startTrackpadGesture(event) {
  event.preventDefault()
  gestureStartZoom.value = zoom.value
}

function handleTrackpadGesture(event) {
  event.preventDefault()
  zoom.value = clampZoom(gestureStartZoom.value * event.scale)
}

watch(flashAnchorId, async (anchorId) => {
  if (!anchorId) return
  await nextTick()
  const target = viewerScroll.value?.querySelector(`[data-block-id="${CSS.escape(anchorId)}"]`)
  target?.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'nearest' })
})

function findingOf(blockId) {
  return showEvidence.value ? findingByAnchor.value[blockId] : null
}
function toneOf(blockId) {
  const finding = findingOf(blockId)
  if (!finding) return null
  if (selectedEvidenceOf(finding, blockId)?.selectedText) return null
  return toneMap[finding.type]
}

function selectedEvidenceOf(finding, blockId) {
  return finding?.evidence?.find((item) => item.anchorId === blockId)
}

function annotationPinsOf(blockId) {
  return review.annotations
    .map((annotation, index) => ({ ...annotation, annotationIndex: index + 1 }))
    .filter((annotation) => annotation.anchorId === blockId && !annotation.selectedText)
}

function textSegments(block) {
  const sourceText = String(block.text ?? '')
  const finding = findingOf(block.id)
  const ranges = []
  const findingText = selectedEvidenceOf(finding, block.id)?.selectedText
  if (findingText) {
    const start = sourceText.indexOf(findingText)
    if (start >= 0) {
      ranges.push({
        start,
        end: start + findingText.length,
        kind: 'finding',
        tone: toneMap[finding.type],
      })
    }
  }

  review.annotations.forEach((annotation, index) => {
    if (annotation.anchorId !== block.id || !annotation.selectedText) return
    const start = sourceText.indexOf(annotation.selectedText)
    if (start >= 0) {
      ranges.push({
        start,
        end: start + annotation.selectedText.length,
        kind: 'annotation',
        annotationId: annotation.id,
        annotationIndex: index + 1,
      })
    }
  })

  if (!ranges.length) return [{ text: sourceText, kind: 'plain' }]
  ranges.sort((left, right) => left.start - right.start || left.end - right.end)

  const parts = []
  let cursor = 0
  ranges.forEach((range) => {
    if (range.start < cursor) return
    if (range.start > cursor) parts.push({ text: sourceText.slice(cursor, range.start), kind: 'plain' })
    parts.push({ ...range, text: sourceText.slice(range.start, range.end) })
    cursor = range.end
  })
  if (cursor < sourceText.length) parts.push({ text: sourceText.slice(cursor), kind: 'plain' })
  return parts
}

function clearSelection() {
  selection.value = null
  optionsOpen.value = false
  annotationText.value = ''
  window.getSelection()?.removeAllRanges()
}

function captureSelection(event) {
  if (event.target.closest('.selection-action')) return
  const browserSelection = window.getSelection()
  const text = browserSelection?.toString().trim()
  if (!text || browserSelection.rangeCount === 0) return clearSelection()

  const range = browserSelection.getRangeAt(0)
  const startElement = range.startContainer.nodeType === Node.TEXT_NODE
    ? range.startContainer.parentElement
    : range.startContainer
  const endElement = range.endContainer.nodeType === Node.TEXT_NODE
    ? range.endContainer.parentElement
    : range.endContainer
  const startBlock = startElement?.closest?.('[data-selectable-block]')
  const endBlock = endElement?.closest?.('[data-selectable-block]')
  if (!startBlock || startBlock !== endBlock) return clearSelection()

  const rect = range.getBoundingClientRect()
  selection.value = {
    text,
    blockId: startBlock.dataset.blockId,
    left: Math.min(rect.right + 6, window.innerWidth - 44),
    top: Math.min(rect.bottom + 6, window.innerHeight - 44),
  }
  optionsOpen.value = false
}

function saveAnnotation() {
  const text = annotationText.value.trim()
  if (!selection.value || !text) return
  review.addAnnotation({
    page: currentPage.value,
    anchorId: selection.value.blockId,
    selectedText: selection.value.text,
    text,
  })
  ui.success('주석을 저장했습니다')
  clearSelection()
}

async function addSelectionAsFinding(type) {
  if (!selection.value || adding.value) return
  adding.value = true
  const selected = selection.value
  const section = [...review.sections].reverse().find((item) => currentPage.value >= item.page)
  const shortText = selected.text.length > 36 ? `${selected.text.slice(0, 36)}…` : selected.text
  try {
    await review.addFinding({
      type,
      page: currentPage.value,
      sectionId: section?.id ?? null,
      title: `${FINDING_TYPE_LABEL[type]}: ${shortText}`,
      description: `사용자가 원문에서 직접 선택한 검토 항목입니다: “${selected.text}”`,
      confidence: 1,
      method: FINDING_METHOD.MANUAL,
      evidence: [{
        anchorId: selected.blockId,
        page: currentPage.value,
        label: selected.text,
        selectedText: selected.text,
      }],
    })
    ui.success(`${FINDING_TYPE_LABEL[type]} 검토 항목으로 추가했습니다`)
    clearSelection()
  } catch (error) {
    ui.error(error.message)
  } finally {
    adding.value = false
  }
}

function closeOnEscape(event) {
  if (event.key === 'Escape') clearSelection()
}
window.addEventListener('keydown', closeOnEscape)
onBeforeUnmount(() => window.removeEventListener('keydown', closeOnEscape))
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
    <div
      ref="viewerScroll"
      class="viewer__scroll u-scroll"
      @wheel="handleTrackpadZoom"
      @gesturestart="startTrackpadGesture"
      @gesturechange="handleTrackpadGesture"
    >
      <article
        class="paper"
        :style="{ transform: `scale(${zoom / 100})`, transformOrigin: 'top center' }"
        @mouseup="captureSelection"
      >
        <span class="paper__page">{{ currentPage }} / {{ totalPages }}</span>

        <template v-for="block in currentPageData?.blocks ?? []" :key="block.id">
          <!-- 제목 -->
          <h2
            v-if="block.kind === 'h2'"
            class="paper__h2 selectable-text"
            data-selectable-block
            :data-block-id="block.id"
            @click="findingOf(block.id) && review.selectAnchor(block.id)"
          >
            <template v-for="(part, index) in textSegments(block)" :key="index">
              <mark
                v-if="part.kind === 'finding'"
                class="selection-mark"
                :class="[`hl--${part.tone}`, { 'is-flash': flashAnchorId === block.id }]"
              >{{ part.text }}</mark>
              <mark
                v-else-if="part.kind === 'annotation'"
                class="annotation-mark"
                :class="{ 'is-flash': flashAnchorId === block.id }"
                @click.stop="review.selectAnnotation(part.annotationId)"
              >{{ part.text }}<sup>{{ part.annotationIndex }}</sup></mark>
              <template v-else>{{ part.text }}</template>
            </template>
          </h2>

          <!-- 문단 -->
          <p
            v-else-if="block.kind === 'p'"
            class="paper__p"
            data-selectable-block
            :data-block-id="block.id"
            :class="[
              toneOf(block.id) ? `hl hl--${toneOf(block.id)}` : '',
              { 'is-flash': flashAnchorId === block.id, 'is-active': findingOf(block.id)?.id === activeFindingId },
            ]"
            @click="findingOf(block.id) && review.selectAnchor(block.id)"
          >
            <template v-for="(part, index) in textSegments(block)" :key="index">
              <mark
                v-if="part.kind === 'finding'"
                class="selection-mark"
                :class="[
                  `hl--${part.tone}`,
                  { 'is-flash': flashAnchorId === block.id },
                ]"
              >
                {{ part.text }}
              </mark>
              <mark
                v-else-if="part.kind === 'annotation'"
                class="annotation-mark"
                :class="{ 'is-flash': flashAnchorId === block.id }"
                @click.stop="review.selectAnnotation(part.annotationId)"
              >
                {{ part.text }}<sup>{{ part.annotationIndex }}</sup>
              </mark>
              <template v-else>{{ part.text }}</template>
            </template>
            <span v-if="annotationPinsOf(block.id).length" class="annotation-pins">
              <button
                v-for="annotation in annotationPinsOf(block.id)"
                :key="annotation.id"
                type="button"
                class="annotation-pin"
                :class="{ 'is-flash': flashAnchorId === block.id }"
                :aria-label="`주석 ${annotation.annotationIndex} 보기`"
                @click.stop="review.selectAnnotation(annotation.id)"
              >{{ annotation.annotationIndex }}</button>
            </span>
          </p>

          <!-- 표 -->
          <figure
            v-else-if="block.kind === 'table'"
            class="paper__table"
            :data-block-id="block.id"
            :class="[
              toneOf(block.id) ? `hl hl--${toneOf(block.id)}` : '',
              { 'is-flash': flashAnchorId === block.id },
            ]"
            @click="findingOf(block.id) && review.selectAnchor(block.id)"
          >
            <figcaption
              class="selectable-text"
              data-selectable-block
              :data-block-id="`${block.id}--caption`"
              @click.stop="findingOf(`${block.id}--caption`) && review.selectAnchor(`${block.id}--caption`)"
            >
              <template
                v-for="(part, index) in textSegments({ id: `${block.id}--caption`, text: block.caption })"
                :key="index"
              >
                <mark
                  v-if="part.kind === 'finding'"
                  class="selection-mark"
                  :class="[`hl--${part.tone}`, { 'is-flash': flashAnchorId === `${block.id}--caption` }]"
                >{{ part.text }}</mark>
                <mark
                  v-else-if="part.kind === 'annotation'"
                  class="annotation-mark"
                  :class="{ 'is-flash': flashAnchorId === `${block.id}--caption` }"
                  @click.stop="review.selectAnnotation(part.annotationId)"
                >{{ part.text }}<sup>{{ part.annotationIndex }}</sup></mark>
                <template v-else>{{ part.text }}</template>
              </template>
            </figcaption>
            <table>
              <thead>
                <tr>
                  <th
                    v-for="(head, cellIndex) in block.head"
                    :key="head"
                    class="selectable-text"
                    data-selectable-block
                    :data-block-id="`${block.id}--head-${cellIndex}`"
                    @click.stop="findingOf(`${block.id}--head-${cellIndex}`) && review.selectAnchor(`${block.id}--head-${cellIndex}`)"
                  >
                    <template
                      v-for="(part, index) in textSegments({ id: `${block.id}--head-${cellIndex}`, text: head })"
                      :key="index"
                    >
                      <mark
                        v-if="part.kind === 'finding'"
                        class="selection-mark"
                        :class="[`hl--${part.tone}`, { 'is-flash': flashAnchorId === `${block.id}--head-${cellIndex}` }]"
                      >{{ part.text }}</mark>
                      <mark
                        v-else-if="part.kind === 'annotation'"
                        class="annotation-mark"
                        :class="{ 'is-flash': flashAnchorId === `${block.id}--head-${cellIndex}` }"
                        @click.stop="review.selectAnnotation(part.annotationId)"
                      >{{ part.text }}<sup>{{ part.annotationIndex }}</sup></mark>
                      <template v-else>{{ part.text }}</template>
                    </template>
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, rowIndex) in block.rows" :key="rowIndex">
                  <td
                    v-for="(cell, cellIndex) in row"
                    :key="cellIndex"
                    class="selectable-text"
                    data-selectable-block
                    :data-block-id="`${block.id}--cell-${rowIndex}-${cellIndex}`"
                    @click.stop="findingOf(`${block.id}--cell-${rowIndex}-${cellIndex}`) && review.selectAnchor(`${block.id}--cell-${rowIndex}-${cellIndex}`)"
                  >
                    <template
                      v-for="(part, index) in textSegments({ id: `${block.id}--cell-${rowIndex}-${cellIndex}`, text: cell })"
                      :key="index"
                    >
                      <mark
                        v-if="part.kind === 'finding'"
                        class="selection-mark"
                        :class="[`hl--${part.tone}`, { 'is-flash': flashAnchorId === `${block.id}--cell-${rowIndex}-${cellIndex}` }]"
                      >{{ part.text }}</mark>
                      <mark
                        v-else-if="part.kind === 'annotation'"
                        class="annotation-mark"
                        :class="{ 'is-flash': flashAnchorId === `${block.id}--cell-${rowIndex}-${cellIndex}` }"
                        @click.stop="review.selectAnnotation(part.annotationId)"
                      >{{ part.text }}<sup>{{ part.annotationIndex }}</sup></mark>
                      <template v-else>{{ part.text }}</template>
                    </template>
                  </td>
                </tr>
              </tbody>
            </table>
            <span v-if="annotationPinsOf(block.id).length" class="annotation-pins">
              <button
                v-for="annotation in annotationPinsOf(block.id)"
                :key="annotation.id"
                type="button"
                class="annotation-pin"
                :class="{ 'is-flash': flashAnchorId === block.id }"
                :aria-label="`주석 ${annotation.annotationIndex} 보기`"
                @click.stop="review.selectAnnotation(annotation.id)"
              >{{ annotation.annotationIndex }}</button>
            </span>
          </figure>

          <!-- 이미지 · 그래프 -->
          <div
            v-else
            class="paper__figure"
            :data-block-id="block.id"
            :class="toneOf(block.id) ? `hl hl--${toneOf(block.id)}` : ''"
            @click="findingOf(block.id) && review.selectAnchor(block.id)"
          >
            {{ block.text }}
            <span v-if="annotationPinsOf(block.id).length" class="annotation-pins">
              <button
                v-for="annotation in annotationPinsOf(block.id)"
                :key="annotation.id"
                type="button"
                class="annotation-pin"
                :class="{ 'is-flash': flashAnchorId === block.id }"
                :aria-label="`주석 ${annotation.annotationIndex} 보기`"
                @click.stop="review.selectAnnotation(annotation.id)"
              >{{ annotation.annotationIndex }}</button>
            </span>
          </div>
        </template>
      </article>
    </div>

    <div
      v-if="selection"
      class="selection-action"
      :style="{ left: `${selection.left}px`, top: `${selection.top}px` }"
    >
      <button
        class="selection-action__trigger"
        type="button"
        aria-label="선택한 텍스트를 검토 항목으로 추가"
        title="검토 항목 추가"
        @click="optionsOpen = !optionsOpen"
      >
        ＋
      </button>
      <div v-if="optionsOpen" class="selection-action__menu">
        <div class="selection-action__annotation">
          <label for="selection-annotation">주석</label>
          <textarea
            id="selection-annotation"
            v-model="annotationText"
            rows="3"
            placeholder="선택한 문구에 주석을 남겨보세요"
            @keydown.stop
          />
          <button
            type="button"
            :disabled="!annotationText.trim()"
            @click="saveAnnotation"
          >
            주석 저장
          </button>
        </div>
        <p>검토 항목으로 추가</p>
        <button
          v-for="type in typeOptions"
          :key="type"
          type="button"
          :disabled="adding"
          :class="`is-${toneMap[type]}`"
          @click="addSelectionAsFinding(type)"
        >
          <i />{{ FINDING_TYPE_LABEL[type] }}
        </button>
      </div>
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
.selection-mark {
  cursor: pointer;
  padding: 1px 0;
  border-radius: 2px;
  box-shadow: inset 0 -2px 0 currentColor;
}
.selectable-text {
  cursor: text;
  user-select: text;
}
.annotation-mark {
  position: relative;
  cursor: pointer;
  padding: 1px 0;
  border-radius: 2px;
  background: var(--c-primary-50);
  color: inherit;
  box-shadow: inset 0 -2px 0 var(--c-primary-400);
}
.annotation-mark sup {
  display: inline-grid;
  place-items: center;
  min-width: 14px;
  height: 14px;
  margin-left: 2px;
  padding: 0 3px;
  border-radius: var(--r-full);
  background: var(--c-primary-600);
  color: var(--c-white);
  font-size: 9px;
  font-weight: 700;
  line-height: 1;
  vertical-align: super;
}
.annotation-pins {
  position: absolute;
  top: -7px;
  right: -7px;
  z-index: 2;
  display: flex;
  gap: 3px;
}
.annotation-pin {
  min-width: 17px;
  height: 17px;
  display: grid;
  place-items: center;
  padding: 0 4px;
  border-radius: var(--r-full);
  background: var(--c-primary-600);
  color: var(--c-white);
  font-size: 9px;
  font-weight: 700;
  line-height: 1;
  box-shadow: var(--shadow-sm);
}
.annotation-pin:hover {
  background: var(--c-primary-700);
  transform: translateY(-1px);
}
.selection-action {
  position: fixed;
  z-index: 50;
}
.selection-action__trigger {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: var(--r-full);
  background: var(--c-inverse-bg);
  color: var(--c-inverse-fg);
  font-size: 20px;
  line-height: 1;
  box-shadow: var(--shadow-md);
}
.selection-action__trigger:hover {
  background: var(--c-primary-600);
}
.selection-action__menu {
  position: absolute;
  right: 0;
  top: 36px;
  width: 220px;
  padding: 7px;
  border: 1px solid var(--c-border);
  border-radius: var(--r-md);
  background: var(--c-surface);
  box-shadow: var(--shadow-lg);
}
.selection-action__annotation {
  padding: 3px 3px 8px;
  border-bottom: 1px solid var(--c-border);
  margin-bottom: 4px;
}
.selection-action__annotation label {
  display: block;
  margin: 1px 4px 5px;
  color: var(--c-text-muted);
  font-size: var(--fs-xs);
  font-weight: 700;
}
.selection-action__annotation textarea {
  width: 100%;
  resize: vertical;
  min-height: 58px;
  max-height: 120px;
  padding: 7px 8px;
  border: 1px solid var(--c-border-strong);
  border-radius: var(--r-sm);
  background: var(--c-surface);
  color: var(--c-text);
  font: inherit;
  font-size: var(--fs-sm);
  line-height: 1.4;
}
.selection-action__annotation textarea:focus {
  border-color: var(--c-primary-500);
  box-shadow: var(--ring);
  outline: none;
}
.selection-action__annotation button {
  justify-content: center;
  margin-top: 5px;
  background: var(--c-primary-500);
  color: var(--c-white);
  font-weight: 700;
}
.selection-action__annotation button:hover {
  background: var(--c-primary-600);
  color: var(--c-white);
}
.selection-action__annotation button:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}
.selection-action__menu p {
  padding: 4px 7px 7px;
  color: var(--c-text-subtle);
  font-size: var(--fs-xs);
  font-weight: 700;
}
.selection-action__menu button {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px;
  border-radius: var(--r-sm);
  color: var(--c-text-muted);
  font-size: var(--fs-sm);
  text-align: left;
}
.selection-action__menu button:hover {
  background: var(--c-surface-hover);
  color: var(--c-text);
}
.selection-action__menu i {
  width: 9px;
  height: 9px;
  flex: none;
  border-radius: 50%;
  background: currentColor;
}
.selection-action__menu .is-error { color: var(--c-finding-error); }
.selection-action__menu .is-check { color: var(--c-finding-check); }
.selection-action__menu .is-evidence { color: var(--c-finding-evidence); }
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
  position: relative;
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
  position: relative;
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
.hl.is-flash,
.selection-mark.is-flash,
.annotation-mark.is-flash,
.annotation-pin.is-flash {
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

</style>
