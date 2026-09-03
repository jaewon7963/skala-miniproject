<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import { validateEmail } from '@/utils/validators'

/** AUTH-03 로그인 (F-06) */
const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const ui = useUiStore()

const form = ref({ email: '', password: '', keepSignedIn: true })
const errors = ref({ email: '', password: '' })
const formError = ref('')

async function submit() {
  errors.value.email = validateEmail(form.value.email)
  errors.value.password = form.value.password ? '' : '비밀번호를 입력해주세요'
  if (errors.value.email || errors.value.password) return

  formError.value = ''
  try {
    const user = await auth.login({ ...form.value })
    ui.success(`${user.email} 계정으로 로그인했습니다`)
    router.push(route.query.redirect || { name: 'library' })
  } catch (e) {
    // 실패 시에만 노출되는 상단 안내 (와이어프레임 F-06)
    formError.value = e.status === 401 ? '이메일 또는 비밀번호를 확인해주세요' : e.message
  }
}
</script>

<template>
  <div>
    <h1 class="title">로그인</h1>
    <p class="subtitle">사업계획서 검토를 이어서 진행합니다</p>

    <p v-if="formError" class="alert">{{ formError }}</p>

    <form class="form" @submit.prevent="submit">
      <AppInput
        v-model="form.email"
        label="이메일"
        type="email"
        placeholder="name@company.com"
        autocomplete="email"
        :error="errors.email"
      />
      <AppInput
        v-model="form.password"
        label="비밀번호"
        type="password"
        placeholder="••••••••"
        autocomplete="current-password"
        :error="errors.password"
      />

      <div class="row">
        <label class="check">
          <input v-model="form.keepSignedIn" type="checkbox" />
          로그인 상태 유지 <span class="u-subtle">(공용 PC에서는 해제)</span>
        </label>
        <!-- AUTH-04 보류 : 링크만 유지 -->
        <button type="button" class="link" @click="ui.info('비밀번호 재설정은 준비 중입니다')">
          비밀번호를 잊으셨나요?
        </button>
      </div>

      <AppButton type="submit" size="lg" block :loading="auth.loading">로그인</AppButton>
    </form>

    <!-- 소셜 · SSO 로그인은 기획 확정 전까지 비활성 상태로 둡니다 -->
    <div class="divider"><span>또는</span></div>
    <div class="sso">
      <AppButton variant="secondary" block disabled>Google로 계속하기</AppButton>
      <AppButton variant="secondary" block disabled>기관 · 기업 SSO로 로그인</AppButton>
    </div>

    <p class="foot">
      계정이 없으신가요?
      <RouterLink class="link" :to="{ name: 'signup' }">회원가입</RouterLink>
    </p>
  </div>
</template>

<style scoped>
.title {
  font-size: var(--fs-2xl);
  letter-spacing: -0.02em;
}
.subtitle {
  color: var(--c-text-muted);
  font-size: var(--fs-md);
  margin-top: 4px;
}
.alert {
  margin-top: 16px;
  padding: 10px 12px;
  border-radius: var(--r-md);
  background: var(--c-danger-50);
  color: var(--c-danger-600);
  font-size: var(--fs-md);
  font-weight: 600;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-top: 20px;
}
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: var(--fs-sm);
}
.check {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--c-text-muted);
  cursor: pointer;
}
.link {
  color: var(--c-primary-600);
  font-weight: 600;
  font-size: var(--fs-sm);
}
.divider {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 20px 0 14px;
}
.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--mat-hairline);
}
.divider span {
  color: var(--c-text-subtle);
  font-size: var(--fs-sm);
}

.sso {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.foot {
  margin-top: 20px;
  text-align: center;
  font-size: var(--fs-md);
  color: var(--c-text-muted);
}
</style>
