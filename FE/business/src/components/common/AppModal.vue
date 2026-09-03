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
    <transition name="fade">
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
  background: rgba(28, 25, 23, 0.32);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  z-index: 60;
}
.modal {
  background: var(--c-surface);
  border-radius: var(--r-lg);
  box-shadow: var(--shadow-lg);
  max-width: 100%;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}
.modal__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px 8px;
}
.modal__head h2 {
  font-size: var(--fs-lg);
}
.modal__close {
  font-size: 20px;
  color: var(--c-text-muted);
  line-height: 1;
}
.modal__body {
  padding: 8px 20px 18px;
  overflow-y: auto;
}
.modal__foot {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 14px 20px;
  border-top: 1px solid var(--c-border);
}
</style>
