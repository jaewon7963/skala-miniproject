<script setup>
import BrandMark from '@/components/common/BrandMark.vue'
</script>

<template>
  <div class="auth">
    <div class="auth__panel">
      <RouterLink to="/" class="auth__brand"><BrandMark tagline /></RouterLink>
      <slot />
    </div>
    <p class="auth__foot">업로드한 문서는 검토 목적으로만 처리됩니다.</p>
  </div>
</template>

<style scoped>
.auth {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  padding: 40px 20px;
  background:
    radial-gradient(720px 340px at 50% -8%, var(--c-primary-100), transparent 72%),
    radial-gradient(520px 300px at 90% 100%, var(--c-danger-50), transparent 70%),
    var(--c-bg);
}
.auth__panel {
  width: 100%;
  max-width: 400px;
  background: var(--mat-card);
  backdrop-filter: blur(var(--mat-blur)) saturate(var(--mat-saturate));
  -webkit-backdrop-filter: blur(var(--mat-blur)) saturate(var(--mat-saturate));
  border: 1px solid var(--mat-hairline);
  border-radius: var(--r-2xl);
  box-shadow: var(--shadow-inner-top), var(--shadow-lg);
  padding: 30px 28px 28px;
}
.auth__brand {
  display: inline-flex;
  margin-bottom: 20px;
}
.auth__foot {
  font-size: var(--fs-sm);
  color: var(--c-text-subtle);
}
</style>
