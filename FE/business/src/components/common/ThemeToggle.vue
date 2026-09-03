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
  padding: 0 10px;
  border-radius: var(--r-md);
  color: var(--c-text-muted);
  font-size: var(--fs-md);
  transition: background var(--transition);
}
.theme:hover {
  background: var(--mat-fill);
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
  width: 38px;
  height: 22px;
  flex: none;
  border-radius: var(--r-full);
  background: var(--mat-fill-strong);
  box-shadow: inset 0 1px 2px rgba(28, 25, 23, 0.12);
  transition: background var(--dur) var(--ease-out);
}
.theme__track i {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(28, 25, 23, 0.3);
  transition: transform var(--dur) var(--ease-spring);
}
.theme__track.is-on {
  background: var(--c-primary-500);
}
.theme__track.is-on i {
  transform: translateX(16px);
}
</style>
