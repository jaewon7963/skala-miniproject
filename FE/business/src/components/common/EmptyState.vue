<script setup>
defineProps({
  icon: { type: String, default: '□' },
  title: String,
  description: String,
})
</script>

<template>
  <div class="empty">
    <div class="empty__icon" aria-hidden="true">{{ icon }}</div>
    <p class="empty__title">{{ title }}</p>
    <p v-if="description" class="empty__desc">{{ description }}</p>
    <div v-if="$slots.default" class="empty__action"><slot /></div>
  </div>
</template>

<style scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 56px 20px;
  color: var(--c-text-muted);
}
.empty__icon {
  width: 44px;
  height: 44px;
  display: grid;
  place-items: center;
  border-radius: var(--r-md);
  background: var(--c-primary-50);
  color: var(--c-primary-500);
  font-size: 20px;
  margin-bottom: 12px;
}
.empty__title {
  color: var(--c-text);
  font-weight: 600;
  font-size: var(--fs-base);
}
.empty__desc {
  font-size: var(--fs-md);
  margin-top: 4px;
}
.empty__action {
  margin-top: 16px;
}
</style>
