<script setup>
import { computed } from 'vue'
import { DOC_STATUS, DOC_STATUS_LABEL } from '@/constants/enums'

const props = defineProps({ status: String })

const tone = computed(() => {
  switch (props.status) {
    case DOC_STATUS.DONE:
      return 'done'
    case DOC_STATUS.FAILED:
      return 'failed'
    case DOC_STATUS.REVIEWING:
      return 'active'
    case DOC_STATUS.PARSING:
    case DOC_STATUS.ANALYZING:
      return 'progress'
    default:
      return 'idle'
  }
})
const label = computed(() => DOC_STATUS_LABEL[props.status] ?? props.status)
</script>

<template>
  <span class="badge" :class="`badge--${tone}`">{{ label }}</span>
</template>

<style scoped>
.badge {
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: var(--r-full);
  font-size: var(--fs-sm);
  font-weight: 600;
  white-space: nowrap;
}
.badge--done {
  background: var(--c-success-50);
  color: var(--c-success-500);
}
.badge--failed {
  background: var(--c-danger-50);
  color: var(--c-danger-600);
}
.badge--active {
  background: var(--c-primary-50);
  color: var(--c-primary-700);
}
.badge--progress {
  background: var(--c-primary-50);
  color: var(--c-primary-600);
}
.badge--idle {
  background: var(--c-bg-subtle);
  color: var(--c-text-muted);
}
</style>
