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
  min-width: 220px;
  max-width: 380px;
  padding: 11px 14px;
  border-radius: var(--r-md);
  background: var(--c-text);
  color: #fff;
  font-size: var(--fs-md);
  box-shadow: var(--shadow-md);
}
.toast--success {
  background: var(--c-primary-600);
}
.toast--error {
  background: var(--c-danger-600);
}
</style>
