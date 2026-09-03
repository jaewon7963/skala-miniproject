<script setup>
import { computed } from 'vue'

const props = defineProps({
  page: { type: Number, default: 1 },
  size: { type: Number, default: 20 },
  total: { type: Number, default: 0 },
})
const emit = defineEmits(['update:page'])

const lastPage = computed(() => Math.max(1, Math.ceil(props.total / props.size)))

function move(next) {
  if (next < 1 || next > lastPage.value || next === props.page) return
  emit('update:page', next)
}
</script>

<template>
  <div class="pagination">
    <span class="pagination__info">전체 {{ total }} 건 · {{ page }} / {{ lastPage }} 페이지</span>
    <div class="pagination__control">
      <button :disabled="page <= 1" aria-label="이전 페이지" @click="move(page - 1)">‹</button>
      <span class="pagination__page" aria-live="polite">{{ page }}</span>
      <button :disabled="page >= lastPage" aria-label="다음 페이지" @click="move(page + 1)">›</button>
    </div>
  </div>
</template>

<style scoped>
.pagination {
  /* 좌측 정보 너비와 무관하게 페이지 컨트롤을 가운데에 둡니다 */
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  padding: 14px 4px 4px;
}
.pagination__info {
  font-size: var(--fs-sm);
  color: var(--c-text-muted);
}
.pagination__control {
  grid-column: 2;
  display: flex;
  justify-content: center;
  gap: 4px;
}
.pagination__control button {
  min-width: 28px;
  height: 28px;
  border-radius: var(--r-sm);
  font-size: var(--fs-sm);
  color: var(--c-text-muted);
}
.pagination__control button:hover:not(:disabled) {
  background: var(--c-surface-hover);
}
.pagination__page {
  min-width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: var(--r-sm);
  background: var(--c-primary-50);
  color: var(--c-primary-700);
  font-size: var(--fs-sm);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.pagination__control button:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
</style>
