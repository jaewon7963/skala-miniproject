<script setup>
import BrandMark from '@/components/common/BrandMark.vue'

/**
 * 로그인 · 회원가입 공통 레이아웃
 * 좌측 화살표는 히스토리와 무관하게 항상 웰컴 페이지로 이동합니다.
 */
</script>

<template>
  <div class="auth">
    <div class="auth__panel">
      <header class="auth__head">
        <RouterLink class="auth__back" :to="{ name: 'welcome' }" aria-label="처음 화면으로">
          <svg viewBox="0 0 16 16" aria-hidden="true" focusable="false">
            <path d="M9.9 3 4.9 8l5 5" />
          </svg>
        </RouterLink>
        <RouterLink to="/" class="auth__brand"><BrandMark tagline /></RouterLink>
      </header>
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
.auth__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 22px;
  margin-left: -4px;
}
.auth__back {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 28px;
  flex: none;
  color: var(--c-text-subtle);
  transition:
    color var(--transition),
    transform var(--dur-fast) var(--ease-spring);
}
.auth__back:hover {
  color: var(--c-text);
}
.auth__back:active {
  transform: translateX(-2px);
}
.auth__back svg {
  width: 17px;
  height: 17px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}
.auth__brand {
  display: inline-flex;
}
.auth__foot {
  font-size: var(--fs-sm);
  color: var(--c-text-subtle);
}
</style>
