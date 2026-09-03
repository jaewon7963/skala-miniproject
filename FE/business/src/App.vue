<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppLayout from '@/layouts/AppLayout.vue'
import AuthLayout from '@/layouts/AuthLayout.vue'
import WorkspaceLayout from '@/layouts/WorkspaceLayout.vue'
import AppToast from '@/components/common/AppToast.vue'

const route = useRoute()

const layouts = { auth: AuthLayout, app: AppLayout, workspace: WorkspaceLayout }
const layout = computed(() => layouts[route.meta.layout] || AppLayout)
</script>

<template>
  <component :is="layout">
    <RouterView v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </RouterView>
  </component>

  <AppToast />
</template>
