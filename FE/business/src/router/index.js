import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

/**
 * 화면 라우트
 * meta.layout : 'auth' | 'app' | 'workspace'
 * meta.public : 비로그인 접근 허용
 */
const routes = [
  // 미로그인 방문자의 첫 화면 (AUTH-01 진입점)
  {
    path: '/',
    name: 'welcome',
    component: () => import('@/views/WelcomeView.vue'),
    meta: { layout: 'landing', public: true, title: '사업계획서 검토를 시작하세요' },
  },

  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { layout: 'auth', public: true, title: '로그인' },
  },
  {
    path: '/signup',
    name: 'signup',
    component: () => import('@/views/auth/SignupView.vue'),
    meta: { layout: 'auth', public: true, title: '회원가입' },
  },
  {
    path: '/library',
    name: 'library',
    component: () => import('@/views/library/LibraryView.vue'),
    meta: { layout: 'app', title: '문서 라이브러리' },
  },
  {
    path: '/upload',
    name: 'upload',
    component: () => import('@/views/upload/UploadView.vue'),
    meta: { layout: 'app', title: '사업계획서 업로드' },
  },
  {
    path: '/jobs/:jobId/progress',
    name: 'job-progress',
    component: () => import('@/views/upload/JobProgressView.vue'),
    props: true,
    meta: { layout: 'app', title: '문서 분석' },
  },
  {
    path: '/review/:jobId',
    name: 'review',
    component: () => import('@/views/review/ReviewView.vue'),
    props: true,
    meta: { layout: 'workspace', title: '검토 화면' },
  },
  {
    path: '/review/:jobId/done',
    name: 'review-done',
    component: () => import('@/views/review/ReviewDoneView.vue'),
    props: true,
    meta: { layout: 'app', title: '검토 완료' },
  },
  {
    path: '/review/:jobId/report',
    name: 'review-report',
    component: () => import('@/views/review/ReportView.vue'),
    props: true,
    meta: { layout: 'app', title: '검토 의견서' },
  },
  {
    path: '/settings',
    name: 'settings',
    component: () => import('@/views/settings/SettingsView.vue'),
    meta: { layout: 'app', title: '설정' },
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { layout: 'app', public: true, title: '페이지를 찾을 수 없습니다' },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior: (to, from, saved) => {
    if (to.hash) return { el: to.hash, behavior: 'smooth', top: 72 }
    return saved ?? { top: 0 }
  },
})

router.beforeEach((to) => {
  const auth = useAuthStore()

  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: 'login', query: to.name === 'library' ? {} : { redirect: to.fullPath } }
  }
  if (to.meta.public && auth.isAuthenticated && ['login', 'signup'].includes(to.name)) {
    return { name: 'library' }
  }
  return true
})

router.afterEach((to) => {
  document.title = to.meta.title ? `${to.meta.title} · BizXray` : 'BizXray'
})

export default router
