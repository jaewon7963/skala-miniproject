<script setup>
defineProps({ compact: Boolean })
</script>

<template>
  <span class="brand">
    <span class="brand__mark" aria-hidden="true">L</span>
    <b v-if="!compact" class="brand__text">LogicCheck</b>
  </span>
</template>

<style scoped>
.brand {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.brand__mark {
  width: 24px;
  height: 24px;
  border-radius: 7px;
  display: grid;
  place-items: center;
  background: var(--c-primary-500);
  color: #fff;
  font-weight: 800;
  font-size: var(--fs-md);
}
.brand__text {
  font-size: var(--fs-base);
  letter-spacing: -0.01em;
}
</style>
