<script setup>
import { computed } from 'vue'

const props = defineProps({
  page: { type: Number, default: 1 },
  size: { type: Number, default: 20 },
  total: { type: Number, default: 0 },
})
const emit = defineEmits(['update:page'])

const lastPage = computed(() => Math.max(1, Math.ceil(props.total / props.size)))
const pages = computed(() => Array.from({ length: lastPage.value }, (_, i) => i + 1))

function move(next) {
  if (next < 1 || next > lastPage.value || next === props.page) return
  emit('update:page', next)
}
</script>

<template>
  <div class="pagination">
    <span class="pagination__info">전체 {{ total }} 건 · {{ page }} / {{ lastPage }} 페이지</span>
    <div class="pagination__control">
      <button :disabled="page <= 1" @click="move(page - 1)">‹</button>
      <button
        v-for="p in pages"
        :key="p"
        :class="{ 'is-active': p === page }"
        @click="move(p)"
      >
        {{ p }}
      </button>
      <button :disabled="page >= lastPage" @click="move(page + 1)">›</button>
    </div>
  </div>
</template>

<style scoped>
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 4px 4px;
}
.pagination__info {
  font-size: var(--fs-sm);
  color: var(--c-text-muted);
}
.pagination__control {
  display: flex;
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
.pagination__control button.is-active {
  background: var(--c-primary-50);
  color: var(--c-primary-700);
  font-weight: 700;
}
.pagination__control button:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}
</style>
