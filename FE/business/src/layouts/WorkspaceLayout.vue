<script setup>
/** 검토 화면처럼 전체 높이를 쓰는 3분할 작업 화면용 레이아웃 */
</script>

<template>
  <div class="workspace"><slot /></div>
</template>

<style scoped>
.workspace {
  height: 100vh;
  min-width: var(--app-min-w);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--c-bg);
}
</style>
