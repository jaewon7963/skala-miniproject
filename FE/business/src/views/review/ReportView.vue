<script setup>
import { computed, onMounted, ref } from 'vue'
import { reviewApi } from '@/api'
import { FINDING_TYPE_LABEL } from '@/constants/enums'
import { formatDate } from '@/utils/format'
import { applyName } from '@/utils/documentNames'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import AppSelect from '@/components/common/AppSelect.vue'

/** SHR-03 검토 의견서 · 내보내기 */
const props = defineProps({ jobId: { type: String, required: true } })

const report = ref(null)
const settings = ref({ verdict: 'CONDITIONAL', dueDate: '2026-09-12', receiver: '사업기획팀' })

const verdictOptions = [
  { value: 'PASS', label: '적합' },
  { value: 'CONDITIONAL', label: '조건부 보완' },
  { value: 'REJECT', label: '부적합' },
]
/** 라이브러리에서 변경한 문서명을 의견서에도 반영합니다 */
const documentLabel = computed(() => {
  if (!report.value) return '-'
  const { name } = applyName({ id: report.value.documentId, name: report.value.documentName })
  return report.value.documentVersion ? `${name} v${report.value.documentVersion}` : name
})

const verdictLabel = computed(
  () => verdictOptions.find((o) => o.value === settings.value.verdict)?.label ?? '-',
)

onMounted(async () => {
  report.value = await reviewApi.getReport(props.jobId)
  settings.value.dueDate = report.value.dueDate
  settings.value.receiver = report.value.receiver
})

function printPage() {
  window.print()
}

</script>

<template>
  <div v-if="report" class="report">
    <!-- 설정 -->
    <aside class="settings no-print">
      <h2 class="settings__title">의견서 설정</h2>
      <div class="settings__field">
        <span class="form-label">판정</span>
        <AppSelect v-model="settings.verdict" :options="verdictOptions" />
      </div>
      <AppInput v-model="settings.dueDate" label="회신 기한" type="date" />
      <AppInput v-model="settings.receiver" label="수신" />
      <p class="settings__note">포함 항목 : 검토 반영 항목만 · 인쇄 시 A4 1~2쪽으로 나눕니다</p>

      <div class="settings__actions">
        <AppButton block @click="printPage">PDF로 저장 · 인쇄</AppButton>
      </div>
    </aside>

    <!-- 미리보기 (A4) -->
    <article class="paper">
      <header class="paper__head">
        <h1>검토 의견서</h1>
        <dl>
          <div><dt>문서</dt><dd>{{ documentLabel }}</dd></div>
          <div><dt>검토자</dt><dd>{{ report.reviewer }} · {{ formatDate(report.reviewedAt) }}</dd></div>
          <div><dt>수신</dt><dd>{{ settings.receiver }}</dd></div>
          <div><dt>회신 기한</dt><dd>{{ settings.dueDate }}</dd></div>
        </dl>
      </header>

      <section class="paper__section">
        <h2>1. 종합 판정</h2>
        <p class="paper__verdict">{{ verdictLabel }}</p>
        <p class="paper__meta">
          <!-- NOTE 검토 점수는 정책 확정 전 데모 값입니다. (기획서 7.10) -->
          오류 {{ report.summary.byType.ERROR }}건 · 확인 필요 {{ report.summary.byType.NEEDS_CHECK }}건 ·
          근거 부족 {{ report.summary.byType.NO_EVIDENCE }}건 · 검토 반영 {{ report.summary.accepted }}건
        </p>
      </section>

      <section class="paper__section">
        <h2>2. 지적 사항 및 수정 지시</h2>
        <table class="paper__table">
          <thead>
            <tr><th>#</th><th>유형</th><th>위치</th><th>수정 지시</th></tr>
          </thead>
          <tbody>
            <tr v-for="item in report.items" :key="item.no">
              <td>{{ item.no }}</td>
              <td>{{ FINDING_TYPE_LABEL[item.type] }}</td>
              <td>p.{{ item.page }}</td>
              <td>{{ item.instruction }}</td>
            </tr>
            <tr v-if="!report.items.length">
              <td colspan="4" class="paper__empty">검토 반영으로 판정한 항목이 없습니다</td>
            </tr>
          </tbody>
        </table>
      </section>

      <section class="paper__section">
        <h2>3. 재제출 확인 항목</h2>
        <ul class="paper__checklist">
          <li v-for="(item, index) in report.checklist" :key="index">□ {{ item }}</li>
          <li v-if="!report.checklist.length">□ 없음</li>
        </ul>
      </section>
    </article>
  </div>
</template>

<style scoped>
.report {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}
.settings {
  position: sticky;
  top: calc(var(--header-h) + 24px);
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px;
  border: 1px solid var(--c-border);
  border-radius: var(--r-lg);
}
.settings__title {
  font-size: var(--fs-base);
}
.settings__note {
  font-size: var(--fs-sm);
  color: var(--c-text-subtle);
  line-height: 1.5;
}
.settings__actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.paper {
  padding: 40px;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--r-md);
  box-shadow: var(--shadow-sm);
}
.paper__head h1 {
  font-size: var(--fs-xl);
  padding-bottom: 12px;
  border-bottom: 2px solid var(--c-primary-500);
}
.paper__head dl {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px 20px;
  margin: 14px 0 0;
}
.paper__head dl > div {
  display: flex;
  gap: 10px;
  font-size: var(--fs-sm);
}
.paper__head dt {
  width: 56px;
  color: var(--c-text-subtle);
}
.paper__head dd {
  margin: 0;
  font-weight: 600;
}
.paper__section {
  margin-top: 26px;
}
.paper__section h2 {
  font-size: var(--fs-base);
  margin-bottom: 8px;
}
.paper__verdict {
  display: inline-block;
  padding: 5px 12px;
  border-radius: var(--r-sm);
  background: var(--c-primary-50);
  color: var(--c-primary-700);
  font-weight: 700;
}
.paper__meta {
  margin-top: 6px;
  font-size: var(--fs-sm);
  color: var(--c-text-muted);
}
.paper__table th,
.paper__table td {
  border: 1px solid var(--c-border);
  padding: 8px 10px;
  font-size: var(--fs-sm);
  text-align: left;
  vertical-align: top;
}
.paper__table th {
  background: var(--c-bg-subtle);
  font-weight: 600;
  color: var(--c-text-muted);
}
.paper__empty {
  text-align: center;
  color: var(--c-text-subtle);
}
.paper__checklist {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: var(--fs-sm);
}

@media (max-width: 900px) {
  .report {
    grid-template-columns: minmax(0, 1fr);
  }
  .settings {
    position: static;
  }
  .paper {
    padding: 28px;
  }
}

@media print {
  .report {
    display: block;
  }
  .paper {
    border: none;
    box-shadow: none;
    padding: 0;
  }
}
</style>
