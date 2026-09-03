<script setup>
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useReviewStore } from '@/stores/review'

/** REV-02 목차 네비게이션 */
const review = useReviewStore()
const { sections, currentPage, showEvidence } = storeToRefs(review)

/** 현재 페이지가 속한 섹션 1개만 활성 표시 */
const activeSectionId = computed(
  () => [...sections.value].reverse().find((s) => currentPage.value >= s.page)?.id ?? null,
)
</script>

<template>
  <aside class="outline">
    <header class="outline__head">문서 목차</header>

    <ol class="outline__list u-scroll">
      <li
        v-for="section in sections"
        :key="section.id"
        :class="[`is-level-${section.level}`, { 'is-active': activeSectionId === section.id }]"
      >
        <button @click="review.goToPage(section.page)">
          <span class="outline__title">{{ section.title }}</span>
          <em v-if="section.findingCount" class="outline__count">{{ section.findingCount }}</em>
          <span class="outline__page">p.{{ section.page }}</span>
        </button>
      </li>
    </ol>

    <footer class="outline__foot">
      <label class="toggle">
        <input v-model="showEvidence" type="checkbox" />
        원문 근거 표시
      </label>
    </footer>
  </aside>
</template>

<style scoped>
.outline {
  width: var(--outline-w);
  flex: none;
  display: flex;
  flex-direction: column;
  border-right: 1px solid var(--c-border);
  background: var(--c-bg-subtle);
}
.outline__head {
  height: 40px;
  display: flex;
  align-items: center;
  padding: 0 14px;
  font-size: var(--fs-sm);
  font-weight: 700;
  color: var(--c-text-subtle);
  border-bottom: 1px solid var(--c-border);
}
.outline__list {
  flex: 1;
  padding: 8px;
}
.outline__list button {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 8px;
  border-radius: var(--r-md);
  font-size: var(--fs-md);
  color: var(--c-text-muted);
  text-align: left;
}
.outline__list button:hover {
  background: var(--c-surface);
  color: var(--c-text);
}
.outline__list li.is-level-2 button {
  padding-left: 20px;
  font-size: var(--fs-sm);
}
.outline__list li.is-active > button {
  background: var(--c-surface);
  color: var(--c-primary-700);
  font-weight: 700;
}
.outline__title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.outline__count {
  font-style: normal;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: var(--r-full);
  background: var(--c-primary-100);
  color: var(--c-primary-700);
  font-size: var(--fs-xs);
  font-weight: 700;
  display: grid;
  place-items: center;
}
.outline__page {
  font-size: var(--fs-xs);
  color: var(--c-text-subtle);
}
.outline__foot {
  border-top: 1px solid var(--c-border);
  padding: 10px 14px;
}
.toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--fs-sm);
  color: var(--c-text-muted);
  cursor: pointer;
}
</style>
