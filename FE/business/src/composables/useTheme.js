import { computed, ref } from 'vue'

/**
 * 라이트 · 다크 테마
 * - <html data-theme="..."> 속성만 바꾸면 tokens.css 가 색을 교체합니다.
 * - 선택값은 localStorage 에 저장하고, 저장값이 없으면 OS 설정을 따릅니다.
 */
const STORAGE_KEY = 'bizxray.theme'
const THEMES = { LIGHT: 'light', DARK: 'dark' }

const theme = ref(THEMES.LIGHT)

function prefersDark() {
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false
}

function apply(next) {
  theme.value = next
  document.documentElement.dataset.theme = next
  document.querySelector('meta[name="theme-color"]')?.setAttribute(
    'content',
    next === THEMES.DARK ? '#121110' : '#ffffff',
  )
}

/** 앱 부팅 시 1회 호출 (main.js) */
export function initTheme() {
  let saved
  try {
    saved = localStorage.getItem(STORAGE_KEY)
  } catch {
    saved = null
  }
  apply(saved === THEMES.DARK || (!saved && prefersDark()) ? THEMES.DARK : THEMES.LIGHT)
}

export function useTheme() {
  const isDark = computed(() => theme.value === THEMES.DARK)

  function setTheme(next) {
    apply(next)
    try {
      localStorage.setItem(STORAGE_KEY, next)
    } catch {
      /* 저장 실패는 무시 (프라이빗 모드 등) */
    }
  }

  const toggle = () => setTheme(isDark.value ? THEMES.LIGHT : THEMES.DARK)

  return { theme, isDark, setTheme, toggle }
}

export { THEMES }
