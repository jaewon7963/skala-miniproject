<script setup>
import { computed } from 'vue'
import { FINDING_METHOD_LABEL, VERDICT, VERDICT_LABEL } from '@/constants/enums'
import { formatPercent } from '@/utils/format'
import AppButton from '@/components/common/AppButton.vue'
import FindingTypeBadge from '@/components/common/FindingTypeBadge.vue'

/**
 * REV-05 항목 상세 / REV-07 항목 판정
 * 카드 상태 4종 : 기본(접힘) · 선택(펼침) · 판정완료-검토반영 · 판정완료-오류아님
 */
const props = defineProps({
  finding: { type: Object, required: true },
  index: Number,
  selected: Boolean,
})
const emit = defineEmits(['select', 'jump', 'verdict', 'undo'])

const decided = computed(() => props.finding.verdict !== VERDICT.PENDING)
const decidedLabel = computed(() => VERDICT_LABEL[props.finding.verdict])
</script>

<template>
  <article
    class="card"
    :class="{ 'is-selected': selected, 'is-decided': decided }"
    @click="emit('select')"
  >
    <header class="card__head">
      <span class="card__no">{{ index }}</span>
      <FindingTypeBadge :type="finding.type" size="sm" />
      <span class="card__loc">p.{{ finding.page }}</span>
    </header>

    <h3 class="card__title">{{ finding.title }}</h3>

    <p v-if="selected || !decided" class="card__desc">{{ finding.description }}</p>

    <!-- 선택 상태 : 근거 + 판정 버튼 -->
    <div v-if="selected && !decided" class="card__detail" @click.stop>
      <p class="card__label">근거</p>
      <ul class="card__evidence">
        <li v-for="(evidence, i) in finding.evidence" :key="evidence.anchorId">
          <button @click="emit('jump', i)">↗ p.{{ evidence.page }} · {{ evidence.label }}</button>
        </li>
      </ul>

      <div class="card__actions">
        <AppButton size="sm" variant="secondary" @click="emit('verdict', VERDICT.REJECTED)">
          오류 아님
        </AppButton>
        <AppButton size="sm" @click="emit('verdict', VERDICT.ACCEPTED)">검토 반영</AppButton>
      </div>
    </div>

    <!-- 판정 완료 상태 -->
    <div v-if="decided" class="card__decided" @click.stop>
      <span>{{ decidedLabel }}(으)로 표시했습니다</span>
      <button class="card__undo" @click="emit('undo')">되돌리기</button>
    </div>

    <footer class="card__foot">
      확신도 {{ formatPercent(finding.confidence) }} · {{ FINDING_METHOD_LABEL[finding.method] }}
    </footer>
  </article>
</template>

<style scoped>
.card {
  padding: 12px;
  border: 1px solid var(--c-border);
  border-radius: var(--r-md);
  background: var(--c-surface);
  cursor: pointer;
  transition:
    border-color var(--transition),
    box-shadow var(--transition);
}
.card:hover {
  border-color: var(--c-border-strong);
}
.card.is-selected {
  border-color: var(--c-primary-500);
  border-width: 2px;
  padding: 11px;
  box-shadow: var(--shadow-sm);
}
.card.is-decided {
  background: var(--c-bg-subtle);
}
.card.is-decided .card__title {
  color: var(--c-text-muted);
}

.card__head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}
.card__no {
  width: 18px;
  height: 18px;
  display: grid;
  place-items: center;
  border-radius: var(--r-sm);
  background: var(--c-bg-subtle);
  color: var(--c-text-muted);
  font-size: var(--fs-xs);
  font-weight: 700;
}
.card__loc {
  font-size: var(--fs-xs);
  color: var(--c-text-subtle);
}
.card__title {
  font-size: var(--fs-md);
  font-weight: 700;
  line-height: 1.45;
}
.card__desc {
  font-size: var(--fs-sm);
  color: var(--c-text-muted);
  line-height: 1.6;
  margin-top: 4px;
}

.card__detail {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed var(--c-border);
}
.card__label {
  font-size: var(--fs-xs);
  font-weight: 700;
  color: var(--c-text-subtle);
  margin-bottom: 4px;
}
.card__evidence button {
  display: block;
  width: 100%;
  text-align: left;
  padding: 5px 6px;
  border-radius: var(--r-sm);
  font-size: var(--fs-sm);
  color: var(--c-primary-700);
  background: var(--c-primary-50);
  margin-bottom: 4px;
}
.card__evidence button:hover {
  background: var(--c-primary-100);
}
.card__actions {
  display: flex;
  gap: 6px;
  margin-top: 10px;
}
.card__actions > * {
  flex: 1;
}

.card__decided {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
  padding: 7px 9px;
  border-radius: var(--r-sm);
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  font-size: var(--fs-sm);
  color: var(--c-text-muted);
}
.card__undo {
  font-size: var(--fs-sm);
  font-weight: 600;
  color: var(--c-primary-600);
}

.card__foot {
  margin-top: 8px;
  font-size: var(--fs-xs);
  color: var(--c-text-subtle);
}
</style>
