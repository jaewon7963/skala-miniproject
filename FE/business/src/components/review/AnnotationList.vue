<script setup>
import { ref } from 'vue'
import { useReviewStore } from '@/stores/review'
import { useUiStore } from '@/stores/ui'
import EmptyState from '@/components/common/EmptyState.vue'

const review = useReviewStore()
const ui = useUiStore()
const menuOpenId = ref(null)
const editingId = ref(null)
const editText = ref('')

function openEdit(annotation) {
  editingId.value = annotation.id
  editText.value = annotation.text
  menuOpenId.value = null
}

function cancelEdit() {
  editingId.value = null
  editText.value = ''
}

function saveEdit(annotationId) {
  if (!editText.value.trim()) return
  review.updateAnnotation(annotationId, editText.value)
  cancelEdit()
  ui.success('주석을 수정했습니다')
}

function remove(annotationId) {
  review.removeAnnotation(annotationId)
  if (editingId.value === annotationId) cancelEdit()
  menuOpenId.value = null
  ui.info('주석을 삭제했습니다')
}
</script>

<template>
  <section class="annotations">
    <header class="annotations__head">
      <h2>주석</h2>
      <span>{{ review.annotations.length }}</span>
    </header>

    <div class="annotations__list u-scroll" @click="menuOpenId = null">
      <article
        v-for="(annotation, index) in review.annotations"
        :key="annotation.id"
      >
        <button
          class="annotations__content"
          type="button"
          @click="review.selectAnnotation(annotation.id)"
        >
          <span class="annotations__meta">
            <small class="annotations__index">{{ index + 1 }}</small>
            <small>p.{{ annotation.page }}</small>
          </span>
          <template v-if="editingId !== annotation.id">
            <strong>{{ annotation.text }}</strong>
            <q>{{ annotation.selectedText }}</q>
          </template>
        </button>

        <button
          v-if="editingId !== annotation.id"
          class="annotations__more"
          type="button"
          aria-label="주석 옵션"
          title="주석 옵션"
          @click.stop="menuOpenId = menuOpenId === annotation.id ? null : annotation.id"
        >
          ⋯
        </button>

        <div v-if="menuOpenId === annotation.id" class="annotations__menu" @click.stop>
          <button type="button" @click="openEdit(annotation)">수정</button>
          <button type="button" class="is-danger" @click="remove(annotation.id)">삭제</button>
        </div>

        <form
          v-if="editingId === annotation.id"
          class="annotations__edit"
          @click.stop
          @submit.prevent="saveEdit(annotation.id)"
        >
          <textarea v-model="editText" rows="3" aria-label="주석 내용" />
          <div>
            <button type="button" @click="cancelEdit">취소</button>
            <button type="submit" class="is-save" :disabled="!editText.trim()">저장</button>
          </div>
        </form>
      </article>

      <EmptyState
        v-if="!review.annotations.length"
        icon="✎"
        title="아직 추가한 주석이 없습니다"
        description="문서의 텍스트를 드래그해 주석을 남겨보세요"
      />
    </div>
  </section>
</template>

<style scoped>
.annotations {
  min-height: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
}
.annotations__head {
  height: 48px;
  flex: none;
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 0 14px;
  border-bottom: 1px solid var(--c-border);
}
.annotations__head h2 {
  font-size: var(--fs-md);
}
.annotations__head span {
  min-width: 19px;
  height: 19px;
  display: grid;
  place-items: center;
  padding: 0 5px;
  border-radius: var(--r-full);
  background: var(--c-primary-100);
  color: var(--c-primary-700);
  font-size: var(--fs-xs);
  font-weight: 700;
}
.annotations__list {
  flex: 1;
  padding: 12px;
}
.annotations__list > article {
  position: relative;
  width: 100%;
  margin-bottom: 8px;
  border: 1px solid var(--c-border);
  border-radius: var(--r-md);
  background: var(--c-surface);
}
.annotations__list > article:hover {
  border-color: var(--c-border-strong);
  box-shadow: var(--shadow-sm);
}
.annotations__content {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 11px 40px 11px 11px;
  text-align: left;
}
.annotations__meta {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.annotations__meta small {
  color: var(--c-text-subtle);
  font-size: var(--fs-xs);
}
.annotations__meta .annotations__index {
  width: 20px;
  height: 20px;
  display: grid;
  place-items: center;
  border-radius: var(--r-full);
  background: var(--c-primary-600);
  color: var(--c-white);
  font-weight: 700;
}
.annotations__list strong {
  display: -webkit-box;
  overflow: hidden;
  color: var(--c-text);
  font-size: var(--fs-sm);
  font-weight: 600;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}
.annotations__list q {
  display: block;
  overflow: hidden;
  color: var(--c-text-subtle);
  font-size: var(--fs-xs);
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.annotations__more {
  position: absolute;
  top: 7px;
  right: 7px;
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: var(--r-sm);
  color: var(--c-text-muted);
  font-size: 20px;
  line-height: 1;
}
.annotations__more:hover {
  background: var(--c-surface-hover);
  color: var(--c-text);
}
.annotations__menu {
  position: absolute;
  top: 34px;
  right: 7px;
  z-index: 5;
  width: 92px;
  padding: 5px;
  border: 1px solid var(--c-border);
  border-radius: var(--r-md);
  background: var(--c-surface);
  box-shadow: var(--shadow-md);
}
.annotations__menu button {
  width: 100%;
  padding: 7px 8px;
  border-radius: var(--r-sm);
  color: var(--c-text-muted);
  font-size: var(--fs-sm);
  text-align: left;
}
.annotations__menu button:hover {
  background: var(--c-surface-hover);
  color: var(--c-text);
}
.annotations__menu button.is-danger {
  color: var(--c-danger-600);
}
.annotations__edit {
  padding: 0 11px 11px;
}
.annotations__edit textarea {
  width: 100%;
  resize: vertical;
  min-height: 68px;
  padding: 8px;
  border: 1px solid var(--c-border-strong);
  border-radius: var(--r-sm);
  background: var(--c-surface);
  color: var(--c-text);
  font: inherit;
  font-size: var(--fs-sm);
  line-height: 1.5;
}
.annotations__edit textarea:focus {
  border-color: var(--c-primary-500);
  box-shadow: var(--ring);
  outline: none;
}
.annotations__edit > div {
  display: flex;
  justify-content: flex-end;
  gap: 6px;
  margin-top: 7px;
}
.annotations__edit > div button {
  padding: 6px 10px;
  border-radius: var(--r-sm);
  color: var(--c-text-muted);
  font-size: var(--fs-sm);
  font-weight: 600;
}
.annotations__edit > div button:hover {
  background: var(--c-surface-hover);
}
.annotations__edit > div .is-save {
  background: var(--c-primary-500);
  color: var(--c-white);
}
.annotations__edit > div .is-save:hover {
  background: var(--c-primary-600);
}
.annotations__edit > div .is-save:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}
</style>
