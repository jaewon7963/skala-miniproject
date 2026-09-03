<script setup>
import { storeToRefs } from 'pinia'
import { useUiStore } from '@/stores/ui'

const ui = useUiStore()
const { toasts } = storeToRefs(ui)
</script>

<template>
  <Teleport to="body">
    <div class="toast-host no-print">
      <transition-group name="fade">
        <div v-for="toast in toasts" :key="toast.id" class="toast" :class="`toast--${toast.type}`">
          {{ toast.message }}
        </div>
      </transition-group>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-host {
  position: fixed;
  right: 20px;
  bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  z-index: 80;
}
.toast {
  min-width: 200px;
  max-width: 380px;
  padding: 11px 18px;
  border-radius: var(--r-full);
  background: var(--c-inverse-bg);
  backdrop-filter: blur(var(--mat-blur)) saturate(var(--mat-saturate));
  -webkit-backdrop-filter: blur(var(--mat-blur)) saturate(var(--mat-saturate));
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: var(--c-inverse-fg);
  font-size: var(--fs-md);
  font-weight: 500;
  box-shadow: var(--shadow-lg);
}
.toast--success {
  background: rgba(232, 94, 0, 0.88);
}
.toast--error {
  background: rgba(201, 42, 34, 0.9);
}

.fade-enter-active {
  transition:
    opacity var(--dur) var(--ease-out),
    transform var(--dur) var(--ease-spring);
}
.fade-leave-active {
  transition:
    opacity var(--dur-fast) var(--ease-out),
    transform var(--dur-fast) var(--ease-out);
}
.fade-enter-from {
  opacity: 0;
  transform: translateY(14px) scale(0.96);
}
.fade-leave-to {
  opacity: 0;
  transform: scale(0.96);
}
</style>
