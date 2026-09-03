<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { reviewApi } from '@/api'
import AppButton from '@/components/common/AppButton.vue'

/** SHR-01 검토 완료 (F-17) */
const props = defineProps({ jobId: { type: String, required: true } })
const router = useRouter()

const summary = ref(null)

onMounted(async () => {
  const job = await reviewApi.getJob(props.jobId)
  summary.value = job.summary
})
</script>

<template>
  <div class="done">
    <div class="done__mark" aria-hidden="true">✓</div>
    <h1 class="done__title">검토를 완료했습니다</h1>
    <p class="done__desc">최종 요약을 스냅샷으로 저장했습니다</p>

    <div class="stats">
      <div class="stats__item">
        <b>{{ summary?.total ?? '-' }}</b>
        <span>총 검토 항목</span>
      </div>
      <div class="stats__item">
        <b>{{ summary?.accepted ?? '-' }}</b>
        <span>검토 반영</span>
      </div>
      <div class="stats__item">
        <b>{{ summary?.rejected ?? '-' }}</b>
        <span>오류 아님</span>
      </div>
    </div>

    <div class="done__actions">
      <AppButton variant="secondary" @click="router.push({ name: 'library' })">
        라이브러리로
      </AppButton>
      <AppButton @click="router.push({ name: 'review-report', params: { jobId } })">
        검토 의견서 만들기
      </AppButton>
    </div>
  </div>
</template>

<style scoped>
.done {
  max-width: 520px;
  margin: 60px auto 0;
  text-align: center;
}
.done__mark {
  width: 52px;
  height: 52px;
  margin: 0 auto 16px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--c-primary-50);
  color: var(--c-primary-600);
  font-size: 22px;
}
.done__title {
  font-size: var(--fs-2xl);
  letter-spacing: -0.02em;
}
.done__desc {
  color: var(--c-text-muted);
  font-size: var(--fs-md);
  margin-top: 4px;
}
.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin: 28px 0;
}
.stats__item {
  padding: 18px 8px;
  border: 1px solid var(--c-border);
  border-radius: var(--r-lg);
}
.stats__item b {
  display: block;
  font-size: var(--fs-2xl);
  color: var(--c-primary-600);
}
.stats__item span {
  font-size: var(--fs-sm);
  color: var(--c-text-muted);
}
.done__actions {
  display: flex;
  justify-content: center;
  gap: 8px;
}
</style>
