import { defineStore } from 'pinia'
import { ref } from 'vue'

let seq = 0

/** 전역 토스트 (와이어프레임: 우측 하단 안내) */
export const useUiStore = defineStore('ui', () => {
  const toasts = ref([])

  function push(message, type = 'info', duration = 2600) {
    const id = ++seq
    toasts.value.push({ id, message, type })
    setTimeout(() => dismiss(id), duration)
    return id
  }

  const success = (message) => push(message, 'success')
  const error = (message) => push(message, 'error', 3200)
  const info = (message) => push(message, 'info')

  function dismiss(id) {
    toasts.value = toasts.value.filter((t) => t.id !== id)
  }

  return { toasts, push, success, error, info, dismiss }
})
