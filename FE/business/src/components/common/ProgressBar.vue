<script setup>
defineProps({
  value: { type: Number, default: 0 },
  tone: { type: String, default: 'primary' },
})
</script>

<template>
  <div class="bar" role="progressbar" :aria-valuenow="value" aria-valuemin="0" aria-valuemax="100">
    <span class="bar__fill" :class="`bar__fill--${tone}`" :style="{ width: `${Math.min(100, Math.max(0, value))}%` }" />
  </div>
</template>

<style scoped>
.bar {
  width: 100%;
  height: 6px;
  border-radius: var(--r-full);
  background: var(--c-border);
  overflow: hidden;
}
.bar__fill {
  display: block;
  height: 100%;
  border-radius: var(--r-full);
  transition: width 240ms ease;
}
.bar__fill--primary {
  background: var(--c-primary-500);
}
.bar__fill--danger {
  background: var(--c-danger-500);
}
.bar__fill--neutral {
  background: var(--c-text-subtle);
}
</style>
