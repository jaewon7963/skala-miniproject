<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppButton from '@/components/common/AppButton.vue'
import BrandMark from '@/components/common/BrandMark.vue'

/** 웰컴(랜딩) 전용 레이아웃 — 상단 네비게이션 + 푸터 */
const router = useRouter()
const auth = useAuthStore()

const scrolled = ref(false)
const onScroll = () => (scrolled.value = window.scrollY > 8)

onMounted(() => window.addEventListener('scroll', onScroll, { passive: true }))
onUnmounted(() => window.removeEventListener('scroll', onScroll))

const navLinks = [
  { href: '#problem', label: '문제' },
  { href: '#features', label: '기능' },
  { href: '#flow', label: '이용 흐름' },
  { href: '#faq', label: 'FAQ' },
]
</script>

<template>
  <div class="landing">
    <!-- 네비게이션 -->
    <header class="nav" :class="{ 'is-scrolled': scrolled }">
      <div class="nav__inner">
        <RouterLink to="/" class="nav__brand"><BrandMark /></RouterLink>

        <nav class="nav__links">
          <a v-for="link in navLinks" :key="link.href" :href="link.href">{{ link.label }}</a>
        </nav>

        <div class="nav__actions">
          <template v-if="auth.isAuthenticated">
            <AppButton size="sm" @click="router.push({ name: 'library' })">
              내 라이브러리로
            </AppButton>
          </template>
          <template v-else>
            <AppButton size="sm" variant="ghost" @click="router.push({ name: 'login' })">
              로그인
            </AppButton>
            <AppButton size="sm" @click="router.push({ name: 'signup' })">회원가입</AppButton>
          </template>
        </div>
      </div>
    </header>

    <main><slot /></main>

    <!-- 푸터 -->
    <footer class="foot">
      <BrandMark />
      <nav class="foot__links">
        <a href="#problem">문제</a>
        <a href="#features">기능</a>
        <a href="#flow">이용 흐름</a>
        <a href="#faq">FAQ</a>
        <RouterLink :to="{ name: 'login' }">로그인</RouterLink>
        <RouterLink :to="{ name: 'signup' }">회원가입</RouterLink>
      </nav>
      <p class="foot__info">
        6조 주차장 개발자들 · AI-Ready Web Service Mini-project<br />
        업로드한 문서는 검토 목적으로만 처리됩니다
      </p>
      <p class="foot__copy">© 2026 LogicCheck. All rights reserved.</p>
    </footer>
  </div>
</template>

<style scoped>
.landing {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--c-bg);
}

/* --- 네비게이션 --- */
.nav {
  position: sticky;
  top: 0;
  z-index: 40;
  border-bottom: 1px solid transparent;
  transition:
    background var(--transition),
    border-color var(--transition);
}
.nav.is-scrolled {
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(10px);
  border-bottom-color: var(--c-border);
}
.nav__inner {
  height: 60px;
  max-width: var(--content-max);
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 24px;
}
.nav__links {
  display: flex;
  gap: 20px;
  flex: 1;
}
.nav__links a {
  font-size: var(--fs-md);
  font-weight: 600;
  color: var(--c-text-muted);
}
.nav__links a:hover {
  color: var(--c-primary-600);
}
.nav__actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* --- 푸터 --- */
.foot {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 44px 24px 52px;
  border-top: 1px solid var(--c-border);
  text-align: center;
}
.foot__links {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 18px;
}
.foot__links a {
  font-size: var(--fs-md);
  color: var(--c-text-muted);
}
.foot__links a:hover {
  color: var(--c-primary-600);
}
.foot__info {
  font-size: var(--fs-sm);
  color: var(--c-text-muted);
  line-height: 1.7;
}
.foot__copy {
  font-size: var(--fs-sm);
  color: var(--c-text-subtle);
}

@media (max-width: 720px) {
  .nav__links {
    display: none;
  }
}
</style>
