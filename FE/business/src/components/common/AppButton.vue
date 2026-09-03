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
  border-radius: var(--r-full);
  border: 1px solid transparent;
  font-weight: 600;
  letter-spacing: var(--ls-normal);
  white-space: nowrap;
  transition:
    background var(--transition),
    border-color var(--transition),
    color var(--transition),
    box-shadow var(--transition),
    transform var(--dur-fast) var(--ease-spring);
}
.btn:active:not(:disabled) {
  transform: scale(0.96);
}
.btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn--sm {
  height: 28px;
  padding: 0 12px;
  font-size: var(--fs-sm);
}
.btn--md {
  height: 34px;
  padding: 0 16px;
  font-size: var(--fs-md);
}
.btn--lg {
  height: 44px;
  padding: 0 22px;
  font-size: var(--fs-base);
}
.btn--block {
  width: 100%;
}

/* 주요 액션 : 상단 하이라이트로 볼륨을 줍니다 */
.btn--primary {
  background: var(--c-primary-500);
  color: #fff;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.28),
    0 1px 2px rgba(28, 25, 23, 0.16);
}
.btn--primary:hover:not(:disabled) {
  background: var(--c-primary-600);
}

/* 보조 : 배경이 비치는 유리 버튼 */
.btn--secondary {
  background: var(--mat-fill);
  backdrop-filter: blur(var(--mat-blur-sm)) saturate(var(--mat-saturate));
  -webkit-backdrop-filter: blur(var(--mat-blur-sm)) saturate(var(--mat-saturate));
  border-color: var(--mat-hairline);
  color: var(--c-text);
  box-shadow: var(--shadow-inner-top);
}
.btn--secondary:hover:not(:disabled) {
  background: var(--mat-fill-strong);
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
  background: var(--mat-fill);
  color: var(--c-text);
}

.btn--danger {
  background: var(--c-danger-500);
  color: #fff;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.24),
    0 1px 2px rgba(28, 25, 23, 0.16);
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
