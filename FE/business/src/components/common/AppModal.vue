<script setup>
defineProps({
  open: Boolean,
  title: String,
  width: { type: String, default: '440px' },
})
const emit = defineEmits(['close'])
</script>

<template>
  <Teleport to="body">
    <transition name="sheet">
      <div v-if="open" class="backdrop" @click.self="emit('close')">
        <section class="modal" :style="{ width }" role="dialog" aria-modal="true">
          <header class="modal__head">
            <h2>{{ title }}</h2>
            <button class="modal__close" aria-label="닫기" @click="emit('close')">×</button>
          </header>
          <div class="modal__body"><slot /></div>
          <footer v-if="$slots.footer" class="modal__foot"><slot name="footer" /></footer>
        </section>
      </div>
    </transition>
  </Teleport>
</template>

<style scoped>
.backdrop {
  position: fixed;
  inset: 0;
  background: var(--mat-scrim);
  backdrop-filter: blur(3px);
  -webkit-backdrop-filter: blur(3px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  z-index: 60;
}
.modal {
  background: var(--mat-card);
  backdrop-filter: blur(30px) saturate(var(--mat-saturate));
  -webkit-backdrop-filter: blur(30px) saturate(var(--mat-saturate));
  border: 1px solid var(--mat-hairline);
  border-radius: var(--r-2xl);
  box-shadow: var(--shadow-inner-top), var(--shadow-lg);
  max-width: 100%;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}
.modal__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 22px 8px;
}
.modal__head h2 {
  font-size: var(--fs-lg);
  letter-spacing: var(--ls-tight);
}
.modal__close {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--mat-fill);
  color: var(--c-text-muted);
  font-size: 17px;
  line-height: 1;
  transition: background var(--transition);
}
.modal__close:hover {
  background: var(--mat-fill-strong);
  color: var(--c-text);
}
.modal__body {
  padding: 8px 22px 20px;
  overflow-y: auto;
}
.modal__foot {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 14px 22px;
  border-top: 1px solid var(--mat-hairline);
}

/* 시트 전환 */
.sheet-enter-active {
  transition:
    opacity var(--dur) var(--ease-out),
    transform var(--dur) var(--ease-spring);
}
.sheet-leave-active {
  transition:
    opacity var(--dur-fast) var(--ease-out),
    transform var(--dur-fast) var(--ease-out);
}
.sheet-enter-from,
.sheet-leave-to {
  opacity: 0;
}
.sheet-enter-from .modal {
  transform: scale(0.94) translateY(10px);
}
.sheet-leave-to .modal {
  transform: scale(0.98);
}
</style>
