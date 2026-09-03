<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppLayout from '@/layouts/AppLayout.vue'
import AuthLayout from '@/layouts/AuthLayout.vue'
import LandingLayout from '@/layouts/LandingLayout.vue'
import WorkspaceLayout from '@/layouts/WorkspaceLayout.vue'
import AppToast from '@/components/common/AppToast.vue'

const route = useRoute()

const layouts = {
  landing: LandingLayout,
  auth: AuthLayout,
  app: AppLayout,
  workspace: WorkspaceLayout,
}
const layout = computed(() => layouts[route.meta.layout] || AppLayout)
</script>

<template>
  <component :is="layout">
    <RouterView v-slot="{ Component }">
      <!--
        duration 을 명시하면 transitionend 를 기다리지 않고 타이머로 전환을 끝냅니다.
        (탭이 백그라운드라 CSS 트랜지션이 진행되지 않을 때 화면이 멈추는 것을 방지)
      -->
      <transition name="fade" mode="out-in" :duration="{ enter: 240, leave: 150 }">
        <component :is="Component" />
      </transition>
    </RouterView>
  </component>

  <AppToast />
</template>
