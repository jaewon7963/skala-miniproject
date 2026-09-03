<script setup>
import { ref } from 'vue'
import { reviewApi } from '@/api'
import { useReviewStore } from '@/stores/review'
import { useUiStore } from '@/stores/ui'
import AppButton from '@/components/common/AppButton.vue'

/**
 * REV-05 확장 : 문서 내 근거만 사용하는 QA
 * NOTE 3일 구현 범위에서 제외될 수 있는 화면입니다. (기획 확정 필요)
 */
const review = useReviewStore()
const ui = useUiStore()

const question = ref('')
const messages = ref([])
const pending = ref(false)

const suggestions = [
  '근거가 약한 주장 3개',
  'KPI 평가 방법 누락 항목',
  '수치 불일치만 모아보기',
  '심사 질의 예상 5개',
]

async function ask(text = question.value) {
  const content = text.trim()
  if (!content || pending.value || !review.job) return

  messages.value.push({ role: 'user', text: content })
  question.value = ''
  pending.value = true
  try {
    const result = await reviewApi.ask(review.job.id, { question: content, selection: null })
    messages.value.push({ role: 'assistant', ...result })
  } catch (e) {
    ui.error(e.message)
  } finally {
    pending.value = false
  }
}

async function promote(message) {
  const created = await reviewApi.promoteToFinding(review.job.id, message.findingDraft)
  review.findings.push(created)
  ui.success('검토 항목으로 추가했습니다')
}
</script>

<template>
  <div class="ask">
    <p class="ask__hint">
      답변은 이 문서 안의 근거만 사용합니다 · 대화는 ReviewJob에 저장됩니다
    </p>

    <div class="ask__log u-scroll">
      <div v-if="!messages.length" class="ask__intro">
        <p class="ask__intro-title">문서에 대해 질문하기</p>
        <div class="ask__chips">
          <button v-for="item in suggestions" :key="item" @click="ask(item)">{{ item }}</button>
        </div>
      </div>

      <div v-for="(message, index) in messages" :key="index" class="msg" :class="`msg--${message.role}`">
        <p class="msg__text">{{ message.text ?? message.answer }}</p>

        <ul v-if="message.evidences?.length" class="msg__evidence">
          <li v-for="evidence in message.evidences" :key="evidence.anchorId">
            <button @click="review.goToPage(evidence.page)">
              ↗ p.{{ evidence.page }} · {{ evidence.label }}
            </button>
          </li>
        </ul>

        <AppButton
          v-if="message.promotable"
          size="sm"
          variant="subtle"
          @click="promote(message)"
        >
          이 답변을 검토 항목으로 추가
        </AppButton>
      </div>

      <p v-if="pending" class="msg msg--assistant u-muted">답변을 작성하고 있습니다…</p>
    </div>

    <form class="ask__form" @submit.prevent="ask()">
      <input v-model="question" placeholder="사업계획서에 질문하기" />
      <AppButton type="submit" size="sm" :disabled="pending">전송</AppButton>
    </form>
  </div>
</template>

<style scoped>
.ask {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  padding: 12px;
}
.ask__hint {
  font-size: var(--fs-xs);
  color: var(--c-text-subtle);
  margin-bottom: 8px;
}
.ask__log {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.ask__intro-title {
  font-weight: 700;
  font-size: var(--fs-md);
  margin-bottom: 8px;
}
.ask__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.ask__chips button {
  padding: 6px 10px;
  border: 1px solid var(--c-border);
  border-radius: var(--r-full);
  font-size: var(--fs-sm);
  color: var(--c-text-muted);
}
.ask__chips button:hover {
  border-color: var(--c-primary-300);
  color: var(--c-primary-700);
  background: var(--c-primary-50);
}

.msg {
  padding: 10px 12px;
  border-radius: var(--r-md);
  font-size: var(--fs-sm);
  line-height: 1.6;
}
.msg--user {
  background: var(--c-primary-500);
  color: #fff;
  align-self: flex-end;
  max-width: 88%;
}
.msg--assistant {
  background: var(--c-bg-subtle);
  border: 1px solid var(--c-border);
}
.msg__evidence {
  margin: 8px 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.msg__evidence button {
  text-align: left;
  font-size: var(--fs-sm);
  color: var(--c-primary-700);
}

.ask__form {
  display: flex;
  gap: 6px;
  padding-top: 10px;
}
.ask__form input {
  flex: 1;
  min-width: 0;
  height: 32px;
  padding: 0 10px;
  border: 1px solid var(--c-border-strong);
  border-radius: var(--r-md);
  font-size: var(--fs-sm);
}
.ask__form input:focus {
  outline: none;
  border-color: var(--c-primary-500);
  box-shadow: var(--ring);
}
</style>
