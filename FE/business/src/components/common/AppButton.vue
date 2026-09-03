<script setup>
defineProps({
  variant: { type: String, default: 'primary' }, // primary | secondary | ghost | danger | subtle
  size: { type: String, default: 'md' }, // sm | md | lg
  block: Boolean,
  disabled: Boolean,
  loading: Boolean,
  type: { type: String, default: 'button' },
})
</script>

<template>
  <button
    :type="type"
    class="btn"
    :class="[`btn--${variant}`, `btn--${size}`, { 'btn--block': block, 'is-loading': loading }]"
    :disabled="disabled || loading"
  >
    <span v-if="loading" class="btn__spinner" aria-hidden="true" />
    <slot />
  </button>
</template>

<style scoped>
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: var(--r-md);
  border: 1px solid transparent;
  font-weight: 600;
  white-space: nowrap;
  transition:
    background var(--transition),
    border-color var(--transition),
    color var(--transition);
}
.btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.btn--sm {
  height: 30px;
  padding: 0 10px;
  font-size: var(--fs-sm);
}
.btn--md {
  height: 36px;
  padding: 0 14px;
  font-size: var(--fs-md);
}
.btn--lg {
  height: 44px;
  padding: 0 18px;
  font-size: var(--fs-base);
}
.btn--block {
  width: 100%;
}

.btn--primary {
  background: var(--c-primary-500);
  color: #fff;
}
.btn--primary:hover:not(:disabled) {
  background: var(--c-primary-600);
}

.btn--secondary {
  background: var(--c-surface);
  border-color: var(--c-border-strong);
  color: var(--c-text);
}
.btn--secondary:hover:not(:disabled) {
  background: var(--c-surface-hover);
}

.btn--subtle {
  background: var(--c-primary-50);
  color: var(--c-primary-700);
}
.btn--subtle:hover:not(:disabled) {
  background: var(--c-primary-100);
}

.btn--ghost {
  background: transparent;
  color: var(--c-text-muted);
}
.btn--ghost:hover:not(:disabled) {
  background: var(--c-surface-hover);
  color: var(--c-text);
}

.btn--danger {
  background: var(--c-danger-500);
  color: #fff;
}
.btn--danger:hover:not(:disabled) {
  background: var(--c-danger-600);
}

.btn__spinner {
  width: 12px;
  height: 12px;
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
