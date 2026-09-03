import { onBeforeUnmount, onMounted } from 'vue'

/**
 * 스크롤 등장 애니메이션
 *
 * 사용법
 *   <section ref="root">
 *     <h2 data-reveal>...</h2>
 *     <div data-reveal style="--d: 80ms">...</div>
 *
 *   const root = ref(null)
 *   useReveal(root)
 *
 * - 화면에 들어온 요소에 .is-visible 을 붙이고 관찰을 해제합니다.
 * - prefers-reduced-motion 이 켜져 있으면 즉시 모두 표시합니다. (DESIGN 8.4)
 */
export function useReveal(rootRef, { threshold = 0.16, rootMargin = '0px 0px -8% 0px' } = {}) {
  let observer = null

  onMounted(() => {
    const root = rootRef.value
    if (!root) return

    const targets = root.querySelectorAll('[data-reveal]')
    const reduced = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

    if (reduced || !('IntersectionObserver' in window)) {
      targets.forEach((el) => el.classList.add('is-visible'))
      return
    }

    observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return
          entry.target.classList.add('is-visible')
          observer.unobserve(entry.target)
        })
      },
      { threshold, rootMargin },
    )

    targets.forEach((el) => observer.observe(el))
  })

  onBeforeUnmount(() => {
    observer?.disconnect()
    observer = null
  })
}

export default useReveal
