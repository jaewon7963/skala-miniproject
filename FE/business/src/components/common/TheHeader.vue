<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import AppButton from '@/components/common/AppButton.vue'
import BrandMark from '@/components/common/BrandMark.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const ui = useUiStore()

const initial = computed(() => auth.user?.name?.charAt(0) ?? '·')
/** 라이브러리를 벗어난 화면에서는 되돌아가기 버튼을 보여줍니다. */
const showBack = computed(() => route.name !== 'library')

async function logout() {
  await auth.logout()
  ui.info('로그아웃되었습니다')
  router.push({ name: 'login' })
}
</script>

<template>
  <header class="header no-print">
    <div class="header__inner">
      <RouterLink v-if="showBack" class="header__back" :to="{ name: 'library' }">
        ← 라이브러리
      </RouterLink>
      <RouterLink :to="{ name: 'library' }"><BrandMark /></RouterLink>

      <span class="u-spacer" />

      <AppButton
        v-if="route.name !== 'upload'"
        size="sm"
        variant="secondary"
        @click="router.push({ name: 'upload' })"
      >
        문서 업로드
      </AppButton>
      <button class="header__avatar" title="설정" @click="router.push({ name: 'settings' })">
        {{ initial }}
      </button>
      <AppButton size="sm" variant="ghost" @click="logout">로그아웃</AppButton>
    </div>
  </header>
</template>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 30;
  height: var(--header-h);
  border-bottom: 1px solid var(--c-border);
  background: var(--c-surface-blur);
  backdrop-filter: blur(6px);
}
.header__inner {
  height: 100%;
  max-width: var(--content-max);
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  gap: 12px;
}
.header__back {
  font-size: var(--fs-md);
  color: var(--c-text-muted);
  padding-right: 4px;
}
.header__back:hover {
  color: var(--c-primary-600);
}
.header__avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--c-primary-50);
  color: var(--c-primary-700);
  font-weight: 700;
  font-size: var(--fs-sm);
}
.header__avatar:hover {
  background: var(--c-primary-100);
}
</style>
