<script setup>
/**
 * BizXray 로고
 * - mark  : 겹친 문서 + 오렌지 강조선
 * - text  : Biz(먹색) + X(오렌지) + ray(먹색)
 */
defineProps({
  compact: Boolean,
  tagline: Boolean,
})
</script>

<template>
  <span class="brand" :class="{ 'brand--stacked': tagline }">
    <svg class="brand__mark" viewBox="0 0 32 32" aria-hidden="true" focusable="false">
      <rect x="1.5" y="4" width="18" height="24" rx="5" class="mark__back" />
      <rect x="10.5" y="4" width="20" height="24" rx="6" class="mark__front" />
      <rect x="15" y="12.5" width="11" height="3.4" rx="1.7" class="mark__accent" />
      <rect x="15" y="18.5" width="7.5" height="3" rx="1.5" class="mark__line" />
    </svg>

    <span v-if="!compact" class="brand__body">
      <b class="brand__text">Biz<em>X</em>ray</b>
      <small v-if="tagline" class="brand__tagline">사업계획서 검토 지원 서비스</small>
    </span>
  </span>
</template>

<style scoped>
.brand {
  display: inline-flex;
  align-items: center;
  gap: 9px;
}
.brand__mark {
  width: 27px;
  height: 27px;
  flex: none;
}
.mark__back {
  fill: var(--c-text-subtle);
  opacity: 0.55;
}
.mark__front {
  fill: var(--c-text);
}
.mark__accent {
  fill: var(--c-primary-500);
}
.mark__line {
  fill: var(--c-surface);
  opacity: 0.75;
}

.brand__body {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}
.brand__text {
  font-size: var(--fs-lg);
  font-weight: 800;
  letter-spacing: -0.03em;
}
.brand__text em {
  font-style: normal;
  color: var(--c-primary-500);
}
.brand__tagline {
  font-size: 10px;
  letter-spacing: -0.01em;
  color: var(--c-text-subtle);
}

/* 태그라인이 있을 때는 마크를 살짝 키워 균형을 맞춥니다 */
.brand--stacked .brand__mark {
  width: 32px;
  height: 32px;
}
</style>
