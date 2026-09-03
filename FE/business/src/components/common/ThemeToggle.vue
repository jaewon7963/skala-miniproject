<script setup>
import { useTheme } from '@/composables/useTheme'

/** 라이트 · 다크 전환 스위치 */
defineProps({ label: { type: String, default: '다크 모드' } })

const { isDark, toggle } = useTheme()
</script>

<template>
  <button
    class="theme"
    type="button"
    role="switch"
    :aria-checked="isDark"
    :title="isDark ? '라이트 모드로 전환' : '다크 모드로 전환'"
    @click="toggle"
  >
    <span class="theme__icon" aria-hidden="true">{{ isDark ? '☾' : '☀' }}</span>
    <span class="theme__label">{{ label }}</span>
    <span class="theme__track" :class="{ 'is-on': isDark }"><i /></span>
  </button>
</template>

<style scoped>
.theme {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 34px;
  padding: 0 8px;
  border-radius: var(--r-md);
  color: var(--c-text-muted);
  font-size: var(--fs-md);
}
.theme:hover {
  background: var(--c-surface-hover);
  color: var(--c-text);
}
.theme__icon {
  width: 16px;
  text-align: center;
  color: var(--c-primary-500);
}
.theme__label {
  flex: 1;
  text-align: left;
}
.theme__track {
  position: relative;
  width: 32px;
  height: 18px;
  flex: none;
  border-radius: var(--r-full);
  background: var(--c-border-strong);
  transition: background var(--transition);
}
.theme__track i {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--c-surface);
  box-shadow: var(--shadow-sm);
  transition: transform var(--transition);
}
.theme__track.is-on {
  background: var(--c-primary-500);
}
.theme__track.is-on i {
  transform: translateX(14px);
}
</style>
