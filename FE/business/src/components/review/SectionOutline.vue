<script setup>
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useReviewStore } from '@/stores/review'
import ThemeToggle from '@/components/common/ThemeToggle.vue'

/** REV-02 목차 네비게이션 */
const review = useReviewStore()
const { sections, currentPage, outlineWidth } = storeToRefs(review)

/** 현재 페이지가 속한 섹션 1개만 활성 표시 */
const activeSectionId = computed(
  () => [...sections.value].reverse().find((s) => currentPage.value >= s.page)?.id ?? null,
)
</script>

<template>
  <aside class="outline" :style="{ width: `${outlineWidth}px` }">
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
      <ThemeToggle />
    </footer>
  </aside>
</template>

<style scoped>
.outline {
  flex: none;
  display: flex;
  flex-direction: column;
  background: var(--mat-sidebar);
  backdrop-filter: blur(var(--mat-blur)) saturate(var(--mat-saturate));
  -webkit-backdrop-filter: blur(var(--mat-blur)) saturate(var(--mat-saturate));
}
.outline__head {
  height: 40px;
  display: flex;
  align-items: center;
  padding: 0 14px;
  font-size: var(--fs-sm);
  font-weight: 700;
  color: var(--c-text-subtle);
  border-bottom: 1px solid var(--mat-hairline);
}
.outline__list {
  flex: 1;
  padding: 8px;
}
.outline__foot {
  flex: none;
  padding: 10px 8px;
  border-top: 1px solid var(--c-border);
}
.outline__list button {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 8px;
  border-radius: var(--r-sm);
  font-size: var(--fs-md);
  color: var(--c-text-muted);
  text-align: left;
}
.outline__list button {
  transition:
    background var(--transition),
    color var(--transition);
}
.outline__list button:hover {
  background: var(--mat-fill);
  color: var(--c-text);
}
.outline__list li.is-level-2 button {
  padding-left: 20px;
  font-size: var(--fs-sm);
}
.outline__list li.is-active > button {
  background: var(--mat-fill-strong);
  color: var(--c-primary-700);
  font-weight: 700;
  box-shadow: var(--shadow-inner-top);
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
}</style>
