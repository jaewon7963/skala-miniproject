<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import AppButton from '@/components/common/AppButton.vue'
import AppInput from '@/components/common/AppInput.vue'
import { validateEmail, validatePassword } from '@/utils/validators'

/** AUTH-02 회원가입 (F-06) */
const router = useRouter()
const auth = useAuthStore()
const ui = useUiStore()

const form = ref({ email: '', password: '', agreeTerms: false, agreePrivacy: false })
const errors = ref({ email: '', password: '', agree: '' })
const formError = ref('')

async function submit() {
  errors.value.email = validateEmail(form.value.email)
  errors.value.password = validatePassword(form.value.password)
  errors.value.agree =
    form.value.agreeTerms && form.value.agreePrivacy ? '' : '필수 약관에 동의해주세요'

  if (Object.values(errors.value).some(Boolean)) return

  formError.value = ''
  try {
    await auth.signup({ ...form.value })
    ui.success('계정을 만들었습니다')
    router.push({ name: 'library' })
  } catch (e) {
    if (e.status === 409) errors.value.email = e.message
    else formError.value = e.message
  }
}
</script>

<template>
  <div>
    <h1 class="title">계정 만들기</h1>
    <p class="subtitle">업무용 이메일로 가입하면 팀 문서를 함께 검토할 수 있습니다</p>

    <p v-if="formError" class="alert">{{ formError }}</p>

    <form class="form" @submit.prevent="submit">
      <AppInput
        v-model="form.email"
        label="업무용 이메일"
        type="email"
        placeholder="name@company.com"
        :error="errors.email"
        hint="같은 도메인 사용자는 조직 단위로 묶입니다"
      />
      <AppInput
        v-model="form.password"
        label="비밀번호"
        type="password"
        placeholder="8자 이상, 영문 + 숫자"
        :error="errors.password"
        hint="8자 이상 · 영문과 숫자 각 1자 이상"
      />

      <div class="agree">
        <label class="check">
          <input v-model="form.agreeTerms" type="checkbox" />
          <span><b>(필수)</b> 이용약관 및 개인정보 처리방침에 동의합니다</span>
        </label>
        <label class="check">
          <input v-model="form.agreePrivacy" type="checkbox" />
          <span><b>(필수)</b> 업로드 문서는 검토 목적으로만 처리됩니다</span>
        </label>
        <p v-if="errors.agree" class="form-error">{{ errors.agree }}</p>
      </div>

      <AppButton type="submit" size="lg" block :loading="auth.loading">계정 만들기</AppButton>
    </form>

    <p class="foot">
      이미 계정이 있으신가요?
      <RouterLink class="link" :to="{ name: 'login' }">로그인</RouterLink>
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
.agree {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border-radius: var(--r-md);
  background: var(--c-bg-subtle);
}
.check {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: var(--fs-sm);
  color: var(--c-text-muted);
  cursor: pointer;
}
.check b {
  color: var(--c-danger-500);
}
.foot {
  margin-top: 20px;
  text-align: center;
  font-size: var(--fs-md);
  color: var(--c-text-muted);
}
.link {
  color: var(--c-primary-600);
  font-weight: 600;
}
</style>
