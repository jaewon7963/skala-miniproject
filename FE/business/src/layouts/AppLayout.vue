<script setup>
import TheHeader from '@/components/common/TheHeader.vue'
</script>

<template>
  <div class="app">
    <TheHeader />
    <main class="app__main"><slot /></main>
  </div>
</template>

<style scoped>
.app {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--c-bg);
}
.app__main {
  flex: 1;
  width: 100%;
  max-width: var(--content-max);
  margin: 0 auto;
  padding: 24px 24px 48px;
}
</style>
