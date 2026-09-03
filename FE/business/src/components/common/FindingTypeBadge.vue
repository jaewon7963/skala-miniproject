<script setup>
import { computed } from 'vue'
import { FINDING_TYPE, FINDING_TYPE_LABEL } from '@/constants/enums'

const props = defineProps({
  type: String,
  size: { type: String, default: 'md' },
})

const tone = computed(
  () =>
    ({
      [FINDING_TYPE.ERROR]: 'error',
      [FINDING_TYPE.NEEDS_CHECK]: 'check',
      [FINDING_TYPE.NO_EVIDENCE]: 'evidence',
    })[props.type] || 'evidence',
)
const label = computed(() => FINDING_TYPE_LABEL[props.type] ?? props.type)
</script>

<template>
  <span class="type" :class="[`type--${tone}`, `type--${size}`]">{{ label }}</span>
</template>

<style scoped>
.type {
  display: inline-flex;
  align-items: center;
  height: 20px;
  padding: 0 8px;
  border-radius: var(--r-full);
  font-size: var(--fs-xs);
  font-weight: 700;
  white-space: nowrap;
}
.type--md {
  height: 22px;
  font-size: var(--fs-sm);
}
.type--error {
  background: var(--c-finding-error-bg);
  color: var(--c-finding-error);
}
.type--check {
  background: var(--c-finding-check-bg);
  color: var(--c-finding-check);
}
.type--evidence {
  background: var(--c-finding-evidence-bg);
  color: var(--c-finding-evidence);
}
</style>
