<script setup>
import { useRouter } from 'vue-router'
import AppButton from '@/components/common/AppButton.vue'
import EmptyState from '@/components/common/EmptyState.vue'

const router = useRouter()
</script>

<template>
  <EmptyState
    icon="?"
    title="페이지를 찾을 수 없습니다"
    description="주소를 다시 확인하거나 라이브러리로 이동해주세요"
  >
    <AppButton @click="router.push({ name: 'library' })">라이브러리로</AppButton>
  </EmptyState>
</template>
